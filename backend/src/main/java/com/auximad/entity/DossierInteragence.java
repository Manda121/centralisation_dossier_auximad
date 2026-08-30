package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dossier_interagence")
@Getter
@Setter
@NoArgsConstructor
public class DossierInteragence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier_mere", nullable = false)
    private Dossier dossierMere;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sous_dossier", nullable = false)
    private Dossier sousDossier;

    @Column(columnDefinition = "TEXT")
    private String motif;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}
