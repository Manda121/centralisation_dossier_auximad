package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conteneur")
@Getter
@Setter
@NoArgsConstructor
public class Conteneur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_dossier", nullable = false)
    private Dossier dossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_container")
    private TypeContainer typeContainer;

    @Column(name = "numero_container", unique = true, length = 100)
    private String numeroContainer;

    @Column(columnDefinition = "TEXT")
    private String description;
}
