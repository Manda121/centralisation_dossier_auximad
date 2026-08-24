package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
public class Agence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private Integer code;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 200)
    private String adresse;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(name = "cp_client", nullable = false, length = 10)
    private String cpClient;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;
}
