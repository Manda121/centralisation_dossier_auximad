package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "historique")
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Historique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur")
    @JsonIgnoreProperties({"motDePasse", "role", "agence"})
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_action")
    private Action action;

    @Column(name = "hostname", length = 100)
    private String hostname;

    @Column(length = 200)
    private String url;

    @Column(name = "logiciel", length = 200)
    private String logiciel;

    @Column(length = 45)
    private String adresseIp;

    @Column(length = 10)
    private String methodeHttp;

    @Column(nullable = false)
    private Integer statutHttp = 200;

    @Column(length = 255)
    private String entite;

    @Column(length = 100)
    private String identifiantEntite;

    @Column
    private String details;

    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction;

    @PrePersist
    protected void onCreate() {
        if (this.dateAction == null) {
            this.dateAction = LocalDateTime.now();
        }
    }
}
