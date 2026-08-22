package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compagnie")
@Getter
@Setter
@NoArgsConstructor
public class Compagnie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(length = 5)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pays")
    private Pays pays;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mode_transport")
    private ModeTransport modeTransport;

    @Column(columnDefinition = "TEXT")
    private String description;
}
