package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mvt_dossier")
@Getter
@Setter
@NoArgsConstructor
public class MvtDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @Column(name = "ip_utilisateur", length = 45)
    private String ipUtilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_operation")
    private OperationDossier operation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ancienne_phase")
    private PhaseDossier anciennePhase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nouvelle_phase")
    private PhaseDossier nouvellePhase;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ancien_statut")
    private StatutDossier ancienStatut;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nouveau_statut")
    private StatutDossier nouveauStatut;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_mvt", nullable = false)
    private LocalDateTime dateMvt = LocalDateTime.now();

    @Transient
    private Document document;
}
