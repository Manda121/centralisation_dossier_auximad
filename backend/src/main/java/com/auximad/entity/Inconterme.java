package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inconterme")
@Getter
@Setter
@NoArgsConstructor
public class Inconterme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(unique = true, length = 10)
    private String abreviation;

    @Column(columnDefinition = "TEXT")
    private String description;
}
