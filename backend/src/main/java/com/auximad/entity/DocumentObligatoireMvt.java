package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Type de document obligatoire : doit avoir un document actif remis sur le
 * dossier (table document) avant de pouvoir enregistrer un mouvement de
 * dossier (mvt_dossier).
 */
@Entity
@Table(name = "document_obligatoire_mvt")
@Getter
@Setter
@NoArgsConstructor
public class DocumentObligatoireMvt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_type_document", nullable = false, unique = true)
    private TypeDocument typeDocument;

    @Column(columnDefinition = "TEXT")
    private String description;
}
