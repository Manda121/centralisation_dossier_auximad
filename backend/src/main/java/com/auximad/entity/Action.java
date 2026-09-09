package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "action")
@Getter
@Setter
@NoArgsConstructor
public class Action {

    public static final String VISUALISATION = "visualisation";
    public static final String CREATION = "creation";
    public static final String MODIFICATION = "modification";
    public static final String SUPPRESSION = "suppression";
    public static final String VALIDATION = "validation";
    public static final String CONNEXION = "connexion";
    public static final String DECONNEXION = "deconnexion";
    public static final String CONNEXION_ECHOUEE = "connexion echouee";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column
    private String description;
}
