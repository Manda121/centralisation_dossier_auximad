package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "autorisation")
@Getter
@Setter
@NoArgsConstructor
public class Autorisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "niveau_autorisation", nullable = false)
    private Integer niveauAutorisation;
}
