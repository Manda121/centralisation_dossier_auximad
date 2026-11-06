package com.auximad.service;

import com.auximad.entity.Document;
import com.auximad.entity.DocumentMvt;
import com.auximad.entity.Dossier;
import com.auximad.entity.MvtAnnulee;
import com.auximad.entity.MvtDossier;
import com.auximad.entity.Utilisateur;
import com.auximad.repository.DocumentMvtRepository;
import com.auximad.repository.DocumentRepository;
import com.auximad.repository.DossierRepository;
import com.auximad.repository.MvtAnnuleeRepository;
import com.auximad.repository.MvtDossierRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.JwtSecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Synchronise un avancement commun sans supprimer l'historique de chaque agence. */
@Service
public class SynchronisationMouvementsService {
    private final DossierRepository dossiers;
    private final MvtDossierRepository mouvements;
    private final MvtAnnuleeRepository annulations;
    private final DocumentMvtRepository liensDocuments;
    private final DocumentRepository documents;
    private final UtilisateurRepository utilisateurs;

    public SynchronisationMouvementsService(DossierRepository dossiers,
                                             MvtDossierRepository mouvements,
                                             MvtAnnuleeRepository annulations,
                                             DocumentMvtRepository liensDocuments,
                                             DocumentRepository documents,
                                             UtilisateurRepository utilisateurs) {
        this.dossiers = dossiers;
        this.mouvements = mouvements;
        this.annulations = annulations;
        this.liensDocuments = liensDocuments;
        this.documents = documents;
        this.utilisateurs = utilisateurs;
    }

    /**
     * Les mutations d'une famille prennent toujours le meme verrou en premier.
     * Les identifiants sont lus avant les entites pour ne pas charger leur etat
     * metier avant l'attente eventuelle du verrou.
     */
    @Transactional
    public List<Dossier> verrouillerFamille(Integer dossierId) {
        List<Integer> ids = dossiers.findFamilleIds(dossierId);
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("Dossier introuvable : " + dossierId);
        }
        Integer racineId = ids.get(0);
        Dossier racine = dossiers.verrouillerInteragence(racineId)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + racineId));
        // Une creation de sous-dossier a pu etre terminee pendant l'attente.
        ids = dossiers.findFamilleIds(dossierId);
        if (ids.isEmpty() || !Objects.equals(racineId, ids.get(0))) {
            throw new IllegalStateException("Les liens interagences ont change. Veuillez reessayer.");
        }
        List<Dossier> famille = new ArrayList<>();
        famille.add(racine);
        for (int i = 1; i < ids.size(); i++) {
            Integer id = ids.get(i);
            famille.add(dossiers.verrouillerInteragence(id)
                    .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + id)));
        }
        if (famille.size() > 1) {
            Integer typeId = racine.getTypeProduction() == null ? null : racine.getTypeProduction().getId();
            for (Dossier membre : famille) {
                Integer membreTypeId = membre.getTypeProduction() == null ? null : membre.getTypeProduction().getId();
                if (!Objects.equals(typeId, membreTypeId)) {
                    throw new IllegalArgumentException(
                            "Les dossiers lies doivent utiliser le meme type de production pour partager leur avancement.");
                }
            }
        }
        return famille;
    }

    /** Le dossier sur lequel vient d'etre effectuee l'action devient la source. */
    @Transactional
    public void synchroniserDepuis(Dossier source) {
        List<Dossier> famille = verrouillerFamille(source.getId());
        if (famille.size() == 1) return;

        List<MvtDossier> actifsSource = mouvements.findActifsByDossierIdOrderByIdAsc(source.getId());
        Set<String> cles = new HashSet<>();
        for (MvtDossier mouvement : actifsSource) {
            if (mouvement.getClePartage() == null || mouvement.getClePartage().isBlank()) {
                mouvement.setClePartage(UUID.randomUUID().toString());
                mouvements.save(mouvement);
            }
            if (!cles.add(mouvement.getClePartage())) {
                throw new IllegalStateException("Plusieurs mouvements actifs possedent la meme cle de partage.");
            }
        }
        Utilisateur auteur = auteurAnnulation(source);
        // Une piece jointe source peut etre reprise par plusieurs agences.
        Map<Integer, List<DocumentMvt>> piecesSource = new HashMap<>();
        for (MvtDossier mouvement : actifsSource) {
            piecesSource.put(mouvement.getId(), liensDocuments.findByMvtDossierId(mouvement.getId()));
        }
        for (Dossier cible : famille) {
            if (Objects.equals(cible.getId(), source.getId())) continue;
            List<MvtDossier> actifsCible = mouvements.findActifsByDossierIdOrderByIdAsc(cible.getId());
            int copiesExistantes = actifsCible.size();
            if (!estPrefixe(actifsCible, actifsSource)) {
                // Annuler puis recopier permet de conserver l'ordre du flux (ordre par ID),
                // y compris les transitions manuelles sans operation.
                annulerSelection(actifsCible, auteur);
                // Liberer les cles actives avant d'inserer leurs nouvelles copies.
                mouvements.flush();
                copiesExistantes = 0;
            }
            Map<Integer, Document> documentsCopies = new HashMap<>();
            for (int i = copiesExistantes; i < actifsSource.size(); i++) {
                MvtDossier original = actifsSource.get(i);
                MvtDossier copie = copierMouvement(original, cible);
                for (DocumentMvt piece : piecesSource.get(original.getId())) {
                    Document document = documentsCopies.computeIfAbsent(piece.getDocument().getId(),
                            id -> copierDocument(piece.getDocument(), cible));
                    DocumentMvt lien = new DocumentMvt();
                    lien.setMvtDossier(copie);
                    lien.setDocument(document);
                    liensDocuments.save(lien);
                    copie.setDocument(document);
                }
            }
            cible.setPhaseDossier(source.getPhaseDossier());
            cible.setStatutDossier(source.getStatutDossier());
            dossiers.save(cible);
        }
    }

    /** Reprise initiale des familles historiques ou d'un nouveau sous-dossier. */
    @Transactional
    public void alignerDepuisMere(Integer dossierId) {
        List<Dossier> famille = verrouillerFamille(dossierId);
        if (famille.size() > 1) synchroniserDepuis(famille.get(0));
    }

    private boolean estPrefixe(List<MvtDossier> cible, List<MvtDossier> source) {
        if (cible.size() > source.size()) return false;
        for (int i = 0; i < cible.size(); i++) {
            String cle = cible.get(i).getClePartage();
            if (cle == null || !Objects.equals(cle, source.get(i).getClePartage())) return false;
        }
        return true;
    }

    private Utilisateur auteurAnnulation(Dossier source) {
        Integer utilisateurId = JwtSecurityContext.getIdUtilisateur();
        return utilisateurId == null ? source.getUtilisateur()
                : utilisateurs.findById(utilisateurId).orElse(source.getUtilisateur());
    }

    private void annulerSelection(List<MvtDossier> selection, Utilisateur auteur) {
        LocalDateTime maintenant = LocalDateTime.now();
        for (MvtDossier mouvement : selection) {
            if (mouvement.getAnnulation() == null) {
                MvtAnnulee annulation = new MvtAnnulee();
                annulation.setMouvement(mouvement);
                annulation.setDateAnnulation(maintenant);
                annulation.setMotifAnnulation("Synchronisation de l'avancement interagence");
                annulation.setUtilisateurAnnulation(auteur);
                mouvement.setAnnulation(annulations.save(annulation));
            }
            mouvement.setEstAnnule(true);
            mouvements.save(mouvement);
        }
    }

    private MvtDossier copierMouvement(MvtDossier source, Dossier cible) {
        MvtDossier copie = new MvtDossier();
        copie.setDossier(cible);
        copie.setUtilisateur(source.getUtilisateur());
        copie.setOperation(source.getOperation());
        copie.setAnciennePhase(source.getAnciennePhase());
        copie.setNouvellePhase(source.getNouvellePhase());
        copie.setAncienStatut(source.getAncienStatut());
        copie.setNouveauStatut(source.getNouveauStatut());
        copie.setDateMvt(source.getDateMvt());
        copie.setCommentaire(source.getCommentaire());
        copie.setOrigine(source.getOrigine());
        copie.setGroupeAjustement(source.getGroupeAjustement());
        copie.setClePartage(source.getClePartage());
        copie.setEstAnnule(false);
        return mouvements.save(copie);
    }

    private Document copierDocument(Document source, Dossier cible) {
        Document copie = new Document();
        copie.setDossier(cible);
        copie.setTypeDocument(source.getTypeDocument());
        copie.setNombre(source.getNombre());
        copie.setCheminFichier(source.getCheminFichier());
        copie.setNomFichier(source.getNomFichier());
        copie.setDateAjout(source.getDateAjout());
        copie.setEstActif(source.getEstActif());
        return documents.save(copie);
    }
}
