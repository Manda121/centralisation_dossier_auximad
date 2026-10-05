package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;

@Entity
@Table(name = "expedition")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Expedition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ordre_transit", unique = true)
    private OrdreTransit ordreTransit;

    // Le lien historique au dossier reste facultatif.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_dossier")
    private Dossier dossier;

    @Column(name = "est_achemine")
    private Boolean estAchemine = false;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mode_transport")
    private ModeTransport modeTransport;
    @Column(name = "id_mode_acheminement")
    private Integer modeAcheminementId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compagnie")
    private Compagnie compagnie;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_navire")
    private Navire navire;
    @Column(name = "numero_bl", length = 100)
    private String numeroBl;
    @Column(name = "date_bl")
    private OffsetDateTime dateBl;
    @Column(name = "lieu_bl", length = 100)
    private String lieuBl;
    @Column(name = "id_port_embarquement")
    private Integer portEmbarquementId;
    @Column(name = "id_port_debarquement")
    private Integer portDebarquementId;
    @Column(name = "numero_vol", length = 100)
    private String numeroVol;
    @Column(name = "numero_lta", length = 100)
    private String numeroLta;
    @Column(name = "date_lta")
    private OffsetDateTime dateLta;
    @Column(name = "lieu_lta", length = 100)
    private String lieuLta;
    @Column(name = "id_aeroport_embarquement")
    private Integer aeroportEmbarquementId;
    @Column(name = "id_aeroport_debarquement")
    private Integer aeroportDebarquementId;
    @Column(name = "lib_scan", length = 200)
    private String libScan;
    @Column(name = "date_depart")
    private OffsetDateTime dateDepart;
    @Column(name = "date_prevu")
    private OffsetDateTime datePrevue;
    @Column(name = "date_arrivee")
    private OffsetDateTime dateArrivee;
}
