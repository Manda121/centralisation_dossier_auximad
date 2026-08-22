package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "navire")
@Getter
@Setter
@NoArgsConstructor
public class Navire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nom;

    @Column(unique = true, length = 20)
    private String imo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_compagnie")
    private Compagnie compagnie;

    @Column(name = "date_ajout")
    private LocalDateTime dateAjout;

    @PrePersist
    protected void onCreate() {
        this.dateAjout = LocalDateTime.now();
    }
}
