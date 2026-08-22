package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bureau")
@Getter
@Setter
@NoArgsConstructor
public class Bureau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 200)
    private String adresse;

    @Column(name = "code_postal", length = 10)
    private String codePostal;

    @Column(length = 100)
    private String ville;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "code_bureau", unique = true, length = 10)
    private String codeBureau;
}
