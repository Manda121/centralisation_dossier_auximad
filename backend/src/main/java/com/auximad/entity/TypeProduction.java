package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "type_production")
@Getter
@Setter
@NoArgsConstructor
public class TypeProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mode_transport")
    private ModeTransport modeTransport;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "TEXT")
    private String description;
}
