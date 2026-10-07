package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mvt_annulee")
@Getter
@Setter
@NoArgsConstructor
public class MvtAnnulee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mvt_dossier", nullable = false, unique = true)
    private MvtDossier mouvement;

    @Column(name = "date_annulation", nullable = false)
    private LocalDateTime dateAnnulation = LocalDateTime.now();

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur_annulation")
    private Utilisateur utilisateurAnnulation;
}
