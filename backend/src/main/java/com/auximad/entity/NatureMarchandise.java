package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "nature_marchandise")
@Getter
@Setter
@NoArgsConstructor
public class NatureMarchandise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;
}
