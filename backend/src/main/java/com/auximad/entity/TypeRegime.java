package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_regime")
@Getter
@Setter
@NoArgsConstructor
public class TypeRegime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;
}
