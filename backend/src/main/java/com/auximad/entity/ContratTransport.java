package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "contrat_transport")
@Getter
@Setter
@NoArgsConstructor
public class ContratTransport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ordre_transit", nullable = false)
    private OrdreTransit ordreTransit;

    @Column(unique = true, length = 100)
    private String numero;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_prevue")
    private LocalDateTime datePrevue;

    @Column(name = "date_arrivee")
    private LocalDateTime dateArrivee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compagnie")
    private Compagnie compagnie;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_navire")
    private Navire navire;

    @Column(name = "n_vol", length = 100)
    private String numeroVol;

    @Column(name = "lieu_emission", length = 100)
    private String lieuEmission;

    @Column(length = 200)
    private String notify;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
