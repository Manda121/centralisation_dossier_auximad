package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "adresse_email_agence", uniqueConstraints =
        @UniqueConstraint(columnNames = {"id_agence", "email"}))
@Getter
@Setter
@NoArgsConstructor
public class AdresseEmailAgence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_agence", nullable = false)
    private Agence agence;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private boolean actif = true;
}
