package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "operation_dossier")
@Getter
@Setter
@NoArgsConstructor
public class OperationDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_phase")
    private PhaseDossier phaseDossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statut")
    private StatutDossier statutDossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_production")
    private TypeProduction typeProduction;

    @Column(name = "niveau_autorisation")
    private Integer niveauAutorisation;

    @Column(name = "niveau_validation")
    private Integer niveauValidation;

    @Column(name = "niveau_operation")
    private Integer niveauOperation;

    @Column(name = "est_actif")
    private Boolean estActif = true;

    @Column(name = "est_obligatoire")
    private Boolean estObligatoire = false;
}
