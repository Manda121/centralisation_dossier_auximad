package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Type de document obligatoire pour une operation donnee : doit avoir un
 * document actif remis sur le dossier (table document) avant de pouvoir
 * enregistrer le mouvement (mvt_dossier) de CETTE operation.
 * Les operations sans document requis restent realizables.
 */
@Entity
@Table(name = "document_obligatoire_mvt",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_operation", "id_type_document"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentObligatoireMvt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_operation", nullable = false)
    private OperationDossier operation;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_type_document", nullable = false)
    private TypeDocument typeDocument;

    @Column(columnDefinition = "TEXT")
    private String description;
}
