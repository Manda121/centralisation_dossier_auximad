package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordre_transit")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ✅ Ajouté pour ignorer les proxies Hibernate
public class OrdreTransit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "numero_ordre_transit", unique = true)
    private Integer numeroOrdreTransit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_agence")
    private Agence agence;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_client")
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_production")
    private TypeProduction typeProduction;

    @Column(name = "numero_commande", length = 100)
    private String numeroCommande;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_regime")
    private TypeRegime typeRegime;

    @Column(name = "destination_finale", length = 200)
    private String destinationFinale;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_expediteur")
    private Client expediteur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_destinataire")
    private Client destinataire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_bureau_dedou")
    private Bureau bureauDedouanement;

    @Column(name = "est_assure")
    private Boolean estAssure = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_destination_facture")
    private Client destinationFacture;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_statut_ot")
    private StatutOt statutOt;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @OneToOne(mappedBy = "ordreTransit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Expedition expedition;

    // Recu uniquement lors de la creation pour une sauvegarde atomique.
    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private ContratTransport contratCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}