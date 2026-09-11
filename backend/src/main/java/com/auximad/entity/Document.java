package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_document", nullable = false)
    private TypeDocument typeDocument;

    @Column
    private Integer nombre;

    @Column(name = "chemin_fichier", length = 200)
    private String cheminFichier;

    @Column(name = "nom_fichier", length = 200)
    private String nomFichier;

    @Column(name = "est_actif", nullable = false)
    private Boolean estActif = true;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();
}
