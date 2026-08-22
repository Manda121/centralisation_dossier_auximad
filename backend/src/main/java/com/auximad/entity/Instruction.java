package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "instruction")
@Getter
@Setter
@NoArgsConstructor
public class Instruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ordre_transit", nullable = false)
    @JsonIgnore // ✅ Masque la référence circulaire et évite le proxy Hibernate
    private OrdreTransit ordreTransit;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}