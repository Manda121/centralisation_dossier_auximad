package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "declaration")
@Getter
@Setter
@NoArgsConstructor
public class Declaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @Column(name = "numero_declaration", length = 100, unique = true)
    private String numeroDeclaration;

    @Column(name = "date_declaration")
    private LocalDateTime dateDeclaration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bureau")
    private Bureau bureau;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_declaration")
    private TypeDeclaration typeDeclaration;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_document")
    private Document document;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();
}
