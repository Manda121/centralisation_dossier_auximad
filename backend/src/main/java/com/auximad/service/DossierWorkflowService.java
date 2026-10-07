package com.auximad.service;

import com.auximad.entity.*;
import com.auximad.repository.*;
import com.auximad.security.JwtSecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DossierWorkflowService {
    private final DossierRepository dossiers;
    private final PhaseDossierRepository phases;
    private final StatutDossierRepository statuts;
    private final OperationDossierRepository operations;
    private final MvtDossierRepository mouvements;
    private final MvtAnnuleeRepository annulations;
    private final TypeDocumentRepository typesDocuments;
    private final DocumentRepository documents;
    private final UtilisateurRepository utilisateurs;

    public DossierWorkflowService(DossierRepository dossiers, PhaseDossierRepository phases,
                                  StatutDossierRepository statuts, OperationDossierRepository operations,
                                  MvtDossierRepository mouvements, MvtAnnuleeRepository annulations,
                                  TypeDocumentRepository typesDocuments,
                                  DocumentRepository documents, UtilisateurRepository utilisateurs) {
        this.dossiers = dossiers;
        this.phases = phases;
        this.statuts = statuts;
        this.operations = operations;
        this.mouvements = mouvements;
        this.annulations = annulations;
        this.typesDocuments = typesDocuments;
        this.documents = documents;
        this.utilisateurs = utilisateurs;
    }

    private List<OperationDossier> etapes(Dossier dossier) {
        if (dossier.getTypeProduction() == null) return List.of();
        return operations.findByTypeProductionIdOrderByNiveauOperationAscIdAsc(dossier.getTypeProduction().getId())
                .stream().filter(op -> Boolean.TRUE.equals(op.getEstActif())).toList();
    }

    private List<PhaseDossier> phasesDuFlux(List<OperationDossier> etapes) {
        LinkedHashMap<Integer, PhaseDossier> uniques = new LinkedHashMap<>();
        etapes.stream().map(OperationDossier::getPhaseDossier).filter(Objects::nonNull)
                .forEach(phase -> uniques.putIfAbsent(phase.getId(), phase));
        return new ArrayList<>(uniques.values());
    }

    private int rang(List<PhaseDossier> phasesFlux, PhaseDossier phase) {
        if (phase == null) return -1;
        for (int i = 0; i < phasesFlux.size(); i++) {
            if (Objects.equals(phasesFlux.get(i).getId(), phase.getId())) return i;
        }
        return -1;
    }

    private PhaseDossier phaseInitiale(List<OperationDossier> etapes) {
        return phasesDuFlux(etapes).stream()
                .filter(p -> "PREPARATION".equalsIgnoreCase(p.getNom()))
                .findFirst().orElseGet(() -> phases.findAll().stream()
                        .filter(p -> "PREPARATION".equalsIgnoreCase(p.getNom())).findFirst().orElse(null));
    }

    private StatutDossier statutInitial() {
        return statuts.findAll().stream().filter(s -> "Ouvert".equalsIgnoreCase(s.getNom()))
                .findFirst().orElse(null);
    }

    public void initialiser(Dossier dossier) {
        PhaseDossier phase = phaseInitiale(etapes(dossier));
        StatutDossier statut = statutInitial();
        if (phase == null || statut == null) {
            throw new IllegalArgumentException("Le flux du dossier doit contenir la phase PREPARATION et le statut Ouvert.");
        }
        dossier.setPhaseDossier(phase);
        dossier.setStatutDossier(statut);
    }

    private Dossier dossier(Integer id) {
        return dossiers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + id));
    }

    private Utilisateur auteur(Dossier dossier) {
        Integer id = JwtSecurityContext.getIdUtilisateur();
        if (id == null && dossier.getUtilisateur() != null) id = dossier.getUtilisateur().getId();
        if (id == null) throw new IllegalArgumentException("Utilisateur introuvable pour le mouvement.");
        final Integer utilisateurId = id;
        return utilisateurs.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));
    }

    private List<MvtDossier> actifs(Integer dossierId) {
        return mouvements.findActifsByDossierIdOrderByIdAsc(dossierId);
    }

    private Set<Integer> operationsFaites(List<MvtDossier> actifs) {
        return actifs.stream().map(MvtDossier::getOperation).filter(Objects::nonNull)
                .map(OperationDossier::getId).collect(Collectors.toSet());
    }

    public PhaseDossier phaseApresOperation(Dossier dossier, OperationDossier operation,
                                            List<MvtDossier> actifs) {
        List<OperationDossier> etapes = etapes(dossier);
        List<PhaseDossier> phasesFlux = phasesDuFlux(etapes);
        int index = rang(phasesFlux, operation.getPhaseDossier());
        if (index < 0) return dossier.getPhaseDossier();
        Set<Integer> faites = operationsFaites(actifs);
        faites.add(operation.getId());
        final int rangOperation = index;
        boolean phaseTerminee = etapes.stream()
                .filter(op -> rang(phasesFlux, op.getPhaseDossier()) == rangOperation)
                .filter(op -> Boolean.TRUE.equals(op.getEstObligatoire()))
                .allMatch(op -> faites.contains(op.getId()));
        int suivant = phaseTerminee && index + 1 < phasesFlux.size() ? index + 1 : index;
        int courant = rang(phasesFlux, dossier.getPhaseDossier());
        return phasesFlux.get(Math.max(suivant, courant));
    }

    private MvtDossier creerAutomatique(Dossier dossier, OperationDossier operation, String origine,
                                        String commentaire, Utilisateur utilisateur, String groupeAjustement) {
        MvtDossier mvt = new MvtDossier();
        mvt.setDossier(dossier);
        mvt.setUtilisateur(utilisateur);
        mvt.setOperation(operation);
        mvt.setAnciennePhase(dossier.getPhaseDossier());
        mvt.setAncienStatut(dossier.getStatutDossier());
        mvt.setNouvellePhase(phaseApresOperation(dossier, operation, actifs(dossier.getId())));
        mvt.setNouveauStatut(operation.getStatutDossier() != null ? operation.getStatutDossier() : dossier.getStatutDossier());
        mvt.setOrigine(origine);
        mvt.setGroupeAjustement(groupeAjustement);
        mvt.setCommentaire(commentaire);
        mouvements.save(mvt);
        dossier.setPhaseDossier(mvt.getNouvellePhase());
        dossier.setStatutDossier(mvt.getNouveauStatut());
        dossiers.save(dossier);
        return mvt;
    }

    private void annulerDepuis(List<MvtDossier> actifs, int index, String motif, Utilisateur utilisateur) {
        for (int i = index; i < actifs.size(); i++) {
            MvtDossier mvt = actifs.get(i);
            MvtAnnulee annulation = new MvtAnnulee();
            annulation.setMouvement(mvt);
            annulation.setDateAnnulation(LocalDateTime.now());
            annulation.setMotifAnnulation(motif);
            annulation.setUtilisateurAnnulation(utilisateur);
            annulations.save(annulation);
            mvt.setAnnulation(annulation);
        }
    }

    private void recalculer(Dossier dossier) {
        List<MvtDossier> restants = actifs(dossier.getId());
        if (restants.isEmpty()) {
            initialiser(dossier);
        } else {
            MvtDossier dernier = restants.get(restants.size() - 1);
            dossier.setPhaseDossier(dernier.getNouvellePhase());
            dossier.setStatutDossier(dernier.getNouveauStatut());
        }
        dossiers.save(dossier);
    }

    @Transactional
    public Dossier synchroniserDocuments(Integer dossierId) {
        Dossier dossier = dossier(dossierId);
        List<OperationDossier> etapes = etapes(dossier);
        PhaseDossier preparation = phaseInitiale(etapes);
        if (preparation == null) return dossier;
        Integer natureId = dossier.getOrdreTransit() == null || dossier.getOrdreTransit().getNatureMarchandise() == null
                ? null : dossier.getOrdreTransit().getNatureMarchandise().getId();
        List<TypeDocument> requis = typesDocuments.findApplicables(dossier.getTypeProduction().getId(), natureId)
                .stream().filter(td -> Boolean.TRUE.equals(td.getEstObligatoire())).toList();
        Set<Integer> remis = documents.findByDossierIdAndEstActifTrue(dossierId).stream()
                .map(doc -> doc.getTypeDocument().getId()).collect(Collectors.toSet());
        boolean complets = requis.stream().allMatch(td -> remis.contains(td.getId()));
        List<MvtDossier> actifs = actifs(dossierId);
        if (!complets) {
            int premierAuto = -1;
            for (int i = 0; i < actifs.size(); i++) {
                if ("DOCUMENTS".equals(actifs.get(i).getOrigine())) { premierAuto = i; break; }
            }
            if (premierAuto >= 0) {
                annulerDepuis(actifs, premierAuto, "Documents obligatoires retirés", auteur(dossier));
                recalculer(dossier);
            }
            return dossier;
        }
        if (!Objects.equals(dossier.getPhaseDossier() == null ? null : dossier.getPhaseDossier().getId(), preparation.getId())) {
            return dossier;
        }
        List<PhaseDossier> phasesFlux = phasesDuFlux(etapes);
        int rangPreparation = rang(phasesFlux, preparation);
        if (rangPreparation < 0 || rangPreparation + 1 >= phasesFlux.size()) return dossier;
        Utilisateur utilisateur = auteur(dossier);
        Set<Integer> faites = operationsFaites(actifs);
        for (OperationDossier op : etapes) {
            if (!Objects.equals(op.getPhaseDossier().getId(), preparation.getId())) continue;
            if (!faites.contains(op.getId())) {
                creerAutomatique(dossier, op, "DOCUMENTS", "Documents obligatoires complets", utilisateur, null);
                faites.add(op.getId());
            }
        }
        dossier.setPhaseDossier(phasesFlux.get(rangPreparation + 1));
        return dossiers.save(dossier);
    }

    @Transactional
    public Dossier apresMouvement(Integer dossierId) {
        Dossier dossier = dossier(dossierId);
        recalculer(dossier);
        return dossier;
    }

    @Transactional
    public Dossier annulerMouvement(Integer mouvementId, String motif) {
        MvtDossier mouvement = mouvements.findById(mouvementId)
                .orElseThrow(() -> new IllegalArgumentException("Mouvement introuvable : " + mouvementId));
        if (mouvement.getAnnulation() != null) return mouvement.getDossier();
        Dossier dossier = mouvement.getDossier();
        List<MvtDossier> actifs = actifs(dossier.getId());
        int index = -1;
        for (int i = 0; i < actifs.size(); i++) {
            if (Objects.equals(actifs.get(i).getId(), mouvementId)) { index = i; break; }
        }
        if (index < 0) throw new IllegalArgumentException("Mouvement déjà annulé.");
        annulerDepuis(actifs, index, motif == null || motif.isBlank() ? "Annulation manuelle" : motif, auteur(dossier));
        recalculer(dossier);
        return dossier;
    }

    @Transactional
    public Dossier changerPhase(Integer dossierId, Integer phaseId) {
        Dossier dossier = dossier(dossierId);
        List<OperationDossier> etapes = etapes(dossier);
        List<PhaseDossier> phasesFlux = phasesDuFlux(etapes);
        PhaseDossier cible = phasesFlux.stream().filter(p -> Objects.equals(p.getId(), phaseId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Phase absente du flux de ce dossier."));
        if (Objects.equals(dossier.getPhaseDossier() == null ? null : dossier.getPhaseDossier().getId(), phaseId)) return dossier;
        int rangCible = rang(phasesFlux, cible);
        String groupeAjustement = UUID.randomUUID().toString();
        Utilisateur utilisateur = auteur(dossier);
        List<MvtDossier> actifs = actifs(dossierId);
        int rangActuel = rang(phasesFlux, dossier.getPhaseDossier());
        if (rangCible < rangActuel) {
            int debut = actifs.size();
            for (int i = 0; i < actifs.size(); i++) {
                MvtDossier mvt = actifs.get(i);
                int rangMouvement = mvt.getOperation() != null
                        ? rang(phasesFlux, mvt.getOperation().getPhaseDossier())
                        : rang(phasesFlux, mvt.getNouvellePhase());
                if (rangMouvement >= rangCible) { debut = i; break; }
            }
            annulerDepuis(actifs, debut, "Retour manuel à la phase " + cible.getNom(), utilisateur);
            recalculer(dossier);
        } else {
            Set<Integer> faites = operationsFaites(actifs);
            for (OperationDossier op : etapes) {
                if (rang(phasesFlux, op.getPhaseDossier()) >= rangCible) break;
                if (!faites.contains(op.getId())) {
                    creerAutomatique(dossier, op, "AJUSTEMENT", "Avancement manuel à la phase " + cible.getNom(), utilisateur, groupeAjustement);
                    faites.add(op.getId());
                }
            }
        }
        MvtDossier transition = new MvtDossier();
        transition.setDossier(dossier);
        transition.setUtilisateur(utilisateur);
        transition.setAnciennePhase(dossier.getPhaseDossier());
        transition.setNouvellePhase(cible);
        transition.setAncienStatut(dossier.getStatutDossier());
        transition.setNouveauStatut(dossier.getStatutDossier());
        transition.setOrigine("AJUSTEMENT_PHASE");
        transition.setGroupeAjustement(groupeAjustement);
        transition.setCommentaire("Phase modifiée manuellement");
        mouvements.save(transition);
        dossier.setPhaseDossier(cible);
        return dossiers.save(dossier);
    }

    @Transactional
    public Dossier changerStatut(Integer dossierId, Integer statutId) {
        Dossier dossier = dossier(dossierId);
        StatutDossier cible = statuts.findById(statutId)
                .orElseThrow(() -> new IllegalArgumentException("Statut introuvable : " + statutId));
        if (Objects.equals(dossier.getStatutDossier() == null ? null : dossier.getStatutDossier().getId(), statutId)) return dossier;
        List<OperationDossier> etapes = etapes(dossier);
        List<MvtDossier> actifs = actifs(dossierId);
        Utilisateur utilisateur = auteur(dossier);
        OperationDossier cibleOperation = null;
        for (OperationDossier op : etapes) {
            if (op.getStatutDossier() != null && Objects.equals(op.getStatutDossier().getId(), statutId)) {
                cibleOperation = op;
            }
        }
        if (cibleOperation != null && !"Ouvert".equalsIgnoreCase(cible.getNom())) {
            String groupeAjustement = UUID.randomUUID().toString();
            Set<Integer> faites = operationsFaites(actifs);
            for (OperationDossier op : etapes) {
                if (!faites.contains(op.getId())) {
                    creerAutomatique(dossier, op, "AJUSTEMENT", "Avancement manuel au statut " + cible.getNom(), utilisateur, groupeAjustement);
                    faites.add(op.getId());
                }
                if (Objects.equals(op.getId(), cibleOperation.getId())) break;
            }
            if (!Objects.equals(dossier.getStatutDossier() == null ? null : dossier.getStatutDossier().getId(), statutId)) {
                MvtDossier transition = new MvtDossier();
                transition.setDossier(dossier);
                transition.setUtilisateur(utilisateur);
                transition.setAnciennePhase(dossier.getPhaseDossier());
                transition.setNouvellePhase(dossier.getPhaseDossier());
                transition.setAncienStatut(dossier.getStatutDossier());
                transition.setNouveauStatut(cible);
                transition.setOrigine("AJUSTEMENT_STATUT");
                transition.setGroupeAjustement(groupeAjustement);
                transition.setCommentaire("Statut modifié manuellement");
                mouvements.save(transition);
                dossier.setStatutDossier(cible);
                dossiers.save(dossier);
            }
            return dossier;
        }
        if ("Ouvert".equalsIgnoreCase(cible.getNom())) {
            int debut = actifs.size();
            String groupeAAnnuler = actifs.isEmpty() ? null : actifs.get(actifs.size() - 1).getGroupeAjustement();
            for (int i = 0; i < actifs.size(); i++) {
                MvtDossier mvt = actifs.get(i);
                if (groupeAAnnuler != null && groupeAAnnuler.equals(mvt.getGroupeAjustement())) {
                    debut = i; break;
                }
                if (groupeAAnnuler == null && mvt.getNouveauStatut() != null
                        && !Objects.equals(mvt.getNouveauStatut().getId(), statutId)) {
                    debut = i; break;
                }
            }
            annulerDepuis(actifs, debut, "Retour manuel au statut Ouvert", utilisateur);
            recalculer(dossier);
            return dossier;
        }
        String groupeAjustement = UUID.randomUUID().toString();
        List<PhaseDossier> phasesFlux = phasesDuFlux(etapes);
        int rangActuel = rang(phasesFlux, dossier.getPhaseDossier());
        Set<Integer> faites = operationsFaites(actifs);
        for (OperationDossier op : etapes) {
            if (rang(phasesFlux, op.getPhaseDossier()) >= rangActuel) break;
            if (!faites.contains(op.getId())) {
                creerAutomatique(dossier, op, "AJUSTEMENT", "Avancement manuel au statut " + cible.getNom(), utilisateur, groupeAjustement);
                faites.add(op.getId());
            }
        }
        MvtDossier transition = new MvtDossier();
        transition.setDossier(dossier);
        transition.setUtilisateur(utilisateur);
        transition.setAnciennePhase(dossier.getPhaseDossier());
        transition.setNouvellePhase(dossier.getPhaseDossier());
        transition.setAncienStatut(dossier.getStatutDossier());
        transition.setNouveauStatut(cible);
        transition.setOrigine("AJUSTEMENT_STATUT");
        transition.setGroupeAjustement(groupeAjustement);
        transition.setCommentaire("Statut modifié manuellement");
        mouvements.save(transition);
        dossier.setStatutDossier(cible);
        return dossiers.save(dossier);
    }
}
