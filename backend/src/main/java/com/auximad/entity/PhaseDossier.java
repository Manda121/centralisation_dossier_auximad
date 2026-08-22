package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "phase_dossier")
@Getter
@Setter
@NoArgsConstructor
public class PhaseDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String nom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mode_transport")
    private ModeTransport modeTransport;

    @Column(columnDefinition = "TEXT")
    private String description;
}
