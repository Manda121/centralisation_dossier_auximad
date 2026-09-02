package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "demande_interagence")
@Getter
@Setter
@NoArgsConstructor
public class DemandeInteragence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agence_demandeuse", nullable = false)
    private Agence agenceDemandeuse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agence_destinataire", nullable = false)
    private Agence agenceDestinataire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur_demande", nullable = false)
    private Utilisateur utilisateurDemande;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur_reponse")
    private Utilisateur utilisateurReponse;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "reponse_motif", columnDefinition = "TEXT")
    private String reponseMotif;

    @Column(length = 20, nullable = false)
    private String statut = "EN_ATTENTE"; // EN_ATTENTE, ACCEPTEE, REFUSEE

    @Column(name = "est_vue", nullable = false)
    private Boolean estVue = false;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "date_reponse")
    private LocalDateTime dateReponse;
}
