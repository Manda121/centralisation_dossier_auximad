package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_declaration")
@Getter
@Setter
@NoArgsConstructor
public class TypeDeclaration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100, unique = true)
    private String nom;

    @Column(columnDefinition = "TEXT")
    private String description;
}
