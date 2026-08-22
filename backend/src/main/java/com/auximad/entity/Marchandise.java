package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "marchandise")
@Getter
@Setter
@NoArgsConstructor
public class Marchandise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ordre_transit", nullable = false)
    private OrdreTransit ordreTransit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_unite_mesure")
    private UniteMesure uniteMesure;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nature_marchandise")
    private NatureMarchandise natureMarchandise;

    @Column(length = 100)
    private String marque;

    @Column(length = 100)
    private String modele;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 10, scale = 2)
    private BigDecimal poids;

    @Column(precision = 10, scale = 2)
    private BigDecimal volume;

    @Column
    private Integer quantite;
}
