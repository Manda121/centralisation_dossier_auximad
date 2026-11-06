package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_mail")
@Getter
@Setter
@NoArgsConstructor
public class HistoriqueMail {
    public enum Statut { EN_ATTENTE, ENVOYE, ECHEC, NON_CONFIGURE, ANNULE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiants et libellés figés pour conserver un historique lisible.
    @Column(name = "id_demande_interagence", nullable = false)
    private Integer idDemandeInteragence;
    @Column(name = "id_agence", nullable = false)
    private Integer idAgence;
    @Column(name = "nom_agence", nullable = false, length = 100)
    private String nomAgence;
    @Column(name = "numero_dossier", nullable = false, length = 100)
    private String numeroDossier;
    @Column(length = 254)
    private String destinataire;
    @Column(length = 254)
    private String expediteur;
    @Column(nullable = false, length = 255)
    private String sujet;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String corps;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Statut statut = Statut.EN_ATTENTE;
    @Column(columnDefinition = "TEXT")
    private String erreur;
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
    @Column(name = "date_tentative")
    private LocalDateTime dateTentative;
    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;
    @Column(name = "id_mail_origine")
    private Long idMailOrigine;
}
