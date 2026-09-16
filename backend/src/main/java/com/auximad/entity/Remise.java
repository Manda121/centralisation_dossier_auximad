package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Remise d'un document pour un dossier.
 * Mappe la table existante "remise" (date_remise, id_type_document, id_dossier).
 */
@Entity
@Table(name = "remise")
@Getter
@Setter
@NoArgsConstructor
public class Remise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "date_remise")
    private LocalDateTime dateRemise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_document", nullable = false)
    private TypeDocument typeDocument;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;
}
