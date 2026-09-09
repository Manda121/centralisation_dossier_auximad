package com.auximad.service;

import com.auximad.dto.DemandeInteragenceReponseRequest;
import com.auximad.dto.DemandeInteragenceRequest;
import com.auximad.dto.PageResponse;
import com.auximad.dto.SousDossierRequest;
import com.auximad.entity.*;
import com.auximad.repository.AgenceRepository;
import com.auximad.repository.DemandeInteragenceRepository;
import com.auximad.repository.DossierRepository;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.AutorisationHelper;
import com.auximad.security.JwtSecurityContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class DemandeInteragenceService {

    private final DemandeInteragenceRepository demandeRepository;
    private final DossierRepository dossierRepository;
    private final AgenceRepository agenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final InterAgenceService interAgenceService;

    public DemandeInteragenceService(DemandeInteragenceRepository demandeRepository,
                                   DossierRepository dossierRepository,
                                   AgenceRepository agenceRepository,
                                   UtilisateurRepository utilisateurRepository,
                                   InterAgenceService interAgenceService) {
        this.demandeRepository = demandeRepository;
        this.dossierRepository = dossierRepository;
        this.agenceRepository = agenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.interAgenceService = interAgenceService;
    }

    @Transactional
    public DemandeInteragence creerDemande(DemandeInteragenceRequest request) {
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        Utilisateur user = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        Dossier dossier = dossierRepository.findById(request.getIdDossier())
                .orElseThrow(() -> new IllegalArgumentException("Dossier introuvable : " + request.getIdDossier()));

        Agence agenceDest = agenceRepository.findById(request.getIdAgenceDestinataire())
                .orElseThrow(() -> new IllegalArgumentException("Agence destinataire introuvable : " + request.getIdAgenceDestinataire()));

        Agence agenceDemandeuse = user.getAgence() != null ? user.getAgence() : dossier.getAgence();

        if (agenceDemandeuse.getId().equals(agenceDest.getId())) {
            throw new IllegalArgumentException("L'agence destinataire doit être différente de l'agence du dossier.");
        }

        // Vérifier si une demande en attente existe déjà pour ce dossier
        Optional<DemandeInteragence> existante = demandeRepository
                .findFirstByDossierIdAndStatutOrderByDateDemandeDesc(dossier.getId(), "EN_ATTENTE");
        if (existante.isPresent()) {
            throw new IllegalArgumentException("Une demande d'inter-agence est déjà en attente pour ce dossier.");
        }

        DemandeInteragence demande = new DemandeInteragence();
        demande.setDossier(dossier);
        demande.setAgenceDemandeuse(agenceDemandeuse);
        demande.setAgenceDestinataire(agenceDest);
        demande.setUtilisateurDemande(user);
        demande.setMotif(request.getMotif());
        demande.setStatut("EN_ATTENTE");
        demande.setEstVue(false);
        demande.setDateDemande(LocalDateTime.now());

        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeInteragence repondreDemande(Integer idDemande, DemandeInteragenceReponseRequest reponseReq) {
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        Utilisateur user = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé."));

        DemandeInteragence demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + idDemande));

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée (" + demande.getStatut() + ").");
        }

        Integer idAgenceUser = JwtSecurityContext.getIdAgence();
        boolean peutToutValider = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_ECRITURE, AutorisationHelper.ACTION_SOUS_DOSSIER);
        if (!peutToutValider && idAgenceUser != null && !idAgenceUser.equals(demande.getAgenceDestinataire().getId())) {
            throw new SecurityException("Seule l'agence destinataire peut répondre à cette demande.");
        }

        demande.setUtilisateurReponse(user);
        demande.setReponseMotif(reponseReq.getMotifReponse());
        demande.setDateReponse(LocalDateTime.now());
        demande.setEstVue(true);

        if (Boolean.TRUE.equals(reponseReq.getAcceptee())) {
            demande.setStatut("ACCEPTEE");
            // Créer automatiquement le sous-dossier inter-agences pour l'agence destinataire
            SousDossierRequest subReq = new SousDossierRequest();
            subReq.setIdAgence(demande.getAgenceDestinataire().getId());
            subReq.setMotif(demande.getMotif());
            interAgenceService.creerSousDossier(demande.getDossier().getId(), subReq);
        } else {
            demande.setStatut("REFUSEE");
        }

        return demandeRepository.save(demande);
    }

    @Transactional
    public DemandeInteragence modifierDemande(Integer idDemande, DemandeInteragenceRequest request) {
        DemandeInteragence demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable : " + idDemande));

        if (!"EN_ATTENTE".equalsIgnoreCase(demande.getStatut())) {
            throw new IllegalArgumentException("Seule une demande en attente peut être modifiée.");
        }

        Integer idAgenceUser = JwtSecurityContext.getIdAgence();
        boolean peutToutModifier = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_ECRITURE, AutorisationHelper.ACTION_SOUS_DOSSIER);
        if (!peutToutModifier && idAgenceUser != null && !idAgenceUser.equals(demande.getAgenceDemandeuse().getId())) {
            throw new SecurityException("Seule l'agence émettrice peut modifier cette demande.");
        }

        if (request.getIdAgenceDestinataire() != null) {
            Agence agenceDest = agenceRepository.findById(request.getIdAgenceDestinataire())
                    .orElseThrow(() -> new IllegalArgumentException("Agence destinataire introuvable : " + request.getIdAgenceDestinataire()));
            if (demande.getAgenceDemandeuse().getId().equals(agenceDest.getId())) {
                throw new IllegalArgumentException("L'agence destinataire doit être différente de l'agence du dossier.");
            }
            demande.setAgenceDestinataire(agenceDest);
        }

        if (request.getMotif() != null) {
            demande.setMotif(request.getMotif());
        }

        return demandeRepository.save(demande);
    }

    @Transactional
    public Optional<DemandeInteragence> getById(Integer id) {
        Optional<DemandeInteragence> demandeOpt = demandeRepository.findById(id);
        demandeOpt.ifPresent(d -> {
            Integer idAgenceUser = JwtSecurityContext.getIdAgence();
            if (idAgenceUser != null && idAgenceUser.equals(d.getAgenceDestinataire().getId()) && !Boolean.TRUE.equals(d.getEstVue())) {
                d.setEstVue(true);
                demandeRepository.save(d);
            }
        });
        return demandeOpt;
    }

    @Transactional(readOnly = true)
    public PageResponse<DemandeInteragence> searchDemandes(
            int page, int size, String sortBy, String sortDir,
            String dateMinStr, String dateMaxStr, String statut,
            Integer agenceDemandeuseId, Integer agenceDestinataireId,
            String query) {

        String sortColumn = "date_demande";
        if ("statut".equalsIgnoreCase(sortBy)) sortColumn = "statut";
        else if ("id".equalsIgnoreCase(sortBy)) sortColumn = "id";

        Sort sort = sortDir != null && sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortColumn).ascending()
                : Sort.by(sortColumn).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        String dateMinISO = null;
        if (dateMinStr != null && !dateMinStr.isBlank()) {
            String clean = dateMinStr.trim();
            if (clean.length() >= 10) {
                dateMinISO = clean.substring(0, 10) + " 00:00:00";
            }
        }

        String dateMaxISO = null;
        if (dateMaxStr != null && !dateMaxStr.isBlank()) {
            String clean = dateMaxStr.trim();
            if (clean.length() >= 10) {
                dateMaxISO = clean.substring(0, 10) + " 23:59:59";
            }
        }

        // Filtre par agence de l'utilisateur si restreint
        Integer agenceConcerneeId = null;
        boolean peutVoirTout = JwtSecurityContext.aPermission(
                AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
        if (!peutVoirTout) {
            agenceConcerneeId = JwtSecurityContext.getIdAgence();
        }

        Page<DemandeInteragence> result = demandeRepository.searchDemandes(
                dateMinISO, dateMaxISO,
                (statut != null && !statut.isBlank()) ? statut : null,
                agenceDemandeuseId, agenceDestinataireId,
                agenceConcerneeId,
                (query != null && !query.isBlank()) ? query : null,
                pageable
        );

        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public long compterNonVues() {
        Integer idAgence = JwtSecurityContext.getIdAgence();
        if (idAgence == null) return 0;
        return demandeRepository.countByAgenceDestinataireIdAndStatutAndEstVueFalse(idAgence, "EN_ATTENTE");
    }

    @Transactional(readOnly = true)
    public List<DemandeInteragence> getDemandesParDossier(Integer dossierId) {
        return demandeRepository.findByDossierIdOrderByDateDemandeDesc(dossierId);
    }
}
