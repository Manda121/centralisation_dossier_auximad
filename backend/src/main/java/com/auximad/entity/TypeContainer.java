package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "type_container")
@Getter
@Setter
@NoArgsConstructor
public class TypeContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal volume;

    @Column(name = "poids_max", precision = 10, scale = 2)
    private BigDecimal poidsMax;

    @Column(precision = 10, scale = 2)
    private BigDecimal longueur;

    @Column(precision = 10, scale = 2)
    private BigDecimal largeur;

    @Column(precision = 10, scale = 2)
    private BigDecimal hauteur;
}
