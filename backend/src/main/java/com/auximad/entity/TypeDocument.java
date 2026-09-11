package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_document")
@Getter
@Setter
@NoArgsConstructor
public class TypeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100, unique = true)
    private String nom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_production")
    private TypeProduction typeProduction;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_nature_marchandise")
    private NatureMarchandise natureMarchandise;

    @Column(name = "est_obligatoire", nullable = false)
    private Boolean estObligatoire = false;

    @Column(columnDefinition = "TEXT")
    private String description;
}
