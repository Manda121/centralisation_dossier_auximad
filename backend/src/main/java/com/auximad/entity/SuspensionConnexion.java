package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "suspension_connexion")
@Getter
@Setter
@NoArgsConstructor
public class SuspensionConnexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "tentatives_echouees", nullable = false)
    private Integer tentativesEchouees = 0;

    @Column(name = "date_suspension")
    private LocalDateTime dateSuspension;

    @Column(name = "duree_suspension")
    private Integer dureeSuspension = 15;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    // ========== Méthodes métier ==========

    /**
     * Vérifie si l'utilisateur est actuellement suspendu
     */
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("estSuspendu")
    public boolean estSuspendu() {
        if (dateSuspension == null) {
            return false;
        }
        LocalDateTime finSuspension = dateSuspension.plusMinutes(dureeSuspension != null ? dureeSuspension : 15);
        return !LocalDateTime.now().isAfter(finSuspension);
    }

    /**
     * Retourne le temps restant de suspension en minutes
     */
    @jakarta.persistence.Transient
    @com.fasterxml.jackson.annotation.JsonProperty("tempsRestantMinutes")
    public long getTempsRestantMinutes() {
        if (dateSuspension == null) {
            return 0;
        }
        LocalDateTime finSuspension = dateSuspension.plusMinutes(dureeSuspension != null ? dureeSuspension : 15);
        long minutes = java.time.Duration.between(LocalDateTime.now(), finSuspension).toMinutes();
        return Math.max(0, minutes);
    }

    /**
     * Incrémente les tentatives échouées et suspend si le seuil est atteint
     */
    public void incrementerTentatives(int maxTentatives, int dureeSuspensionMinutes) {
        this.tentativesEchouees++;
        if (this.tentativesEchouees >= maxTentatives) {
            this.dateSuspension = LocalDateTime.now();
            this.dureeSuspension = dureeSuspensionMinutes;
        }
    }

    /**
     * Réinitialise les tentatives après une connexion réussie ou expiration de suspension
     */
    public void reinitialiser() {
        this.tentativesEchouees = 0;
        this.dateSuspension = null;
    }

    /**
     * Crée une nouvelle instance de SuspensionConnexion pour un utilisateur
     */
    public static SuspensionConnexion creerPourUtilisateur(Utilisateur utilisateur) {
        SuspensionConnexion suspension = new SuspensionConnexion();
        suspension.setUtilisateur(utilisateur);
        suspension.setTentativesEchouees(0);
        suspension.setDateCreation(LocalDateTime.now());
        return suspension;
    }
}
