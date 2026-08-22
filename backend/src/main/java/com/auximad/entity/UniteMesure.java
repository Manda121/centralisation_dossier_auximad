package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "unite_mesure")
@Getter
@Setter
@NoArgsConstructor
public class UniteMesure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(name = "rapport_m_cube", nullable = false)
    private BigDecimal rapportMCube;

    @Column(columnDefinition = "TEXT")
    private String description;
}
