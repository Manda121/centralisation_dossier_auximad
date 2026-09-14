package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dossier")
@Getter
@Setter
@NoArgsConstructor
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agence", nullable = false)
    private Agence agence;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_client", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_production")
    private TypeProduction typeProduction;

    @Column(name = "numero_dossier", nullable = false, unique = true, length = 100)
    private String numeroDossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ordre_transit")
    private OrdreTransit ordreTransit;

    @Column(name = "est_groupage", nullable = false)
    private Boolean estGroupage = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_phase")
    private PhaseDossier phaseDossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statut")
    private StatutDossier statutDossier;

    @Column
    private LocalDateTime echeance;

    @Column(name = "pour_compte", length = 100)
    private String pourCompte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pays_origine")
    private Pays paysOrigine;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pays_destination")
    private Pays paysDestination;

    @Column(name = "est_prepaye", nullable = false)
    private Boolean estPrepaye = false;

    @Column(name = "est_provisionne", nullable = false)
    private Boolean estProvisionne = false;

    @Column(name = "provision_demandee", precision = 10, scale = 2)
    private BigDecimal provisionDemandee = BigDecimal.ZERO;

    @Column(name = "lieu_paiement_fret", length = 100)
    private String lieuPaiementFret;

    @Column(length = 100)
    private String depart;

    @Column(name = "date_livraison")
    private LocalDateTime dateLivraison;

    @Column(name = "adresse_livraison", length = 200)
    private String adresseLivraison;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_inconterme")
    private Inconterme inconterme;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();


    @Transient
    private Boolean estDossierMere = false;

    @Transient
    private Boolean estSousDossier = false;

    @Transient
    private String motifInteragence;

    @Transient
    private String numeroDossierMere;
}
