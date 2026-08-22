package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "statut_dossier")
@Getter
@Setter
@NoArgsConstructor
public class StatutDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;
}
