package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 15)
    private String telephone;

    @Column(length = 200)
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "code_postal", length = 10)
    private String codePostal;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_pays")
    private Pays pays;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_client")
    private TypeClient typeClient;

    @Column(unique = true, length = 20)
    private String nif;

    @Column(unique = true, length = 20)
    private String stat;

    @Column(length = 20)
    private String fax;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
