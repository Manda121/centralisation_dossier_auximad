package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "statut_ot")
@Getter
@Setter
@NoArgsConstructor
public class StatutOt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;

    public StatutOt(Integer id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }
}
