package com.auximad.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conteneur")
@Getter
@Setter
@NoArgsConstructor
public class Conteneur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "dossier_conteneur",
            joinColumns = @JoinColumn(name = "id_conteneur"),
            inverseJoinColumns = @JoinColumn(name = "id_dossier")
    )
    @JsonIgnoreProperties({"conteneurs", "ordreTransit", "client", "phaseDossier", "statutDossier"})
    private Set<Dossier> dossiers = new HashSet<>();

    @JsonProperty("dossier")
    public Dossier getDossier() {
        if (dossiers != null && !dossiers.isEmpty()) {
            return dossiers.iterator().next();
        }
        return null;
    }

    @JsonSetter("dossier")
    public void setDossier(Dossier dossier) {
        if (dossier != null) {
            if (this.dossiers == null) {
                this.dossiers = new HashSet<>();
            }
            this.dossiers.add(dossier);
        }
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_type_container")
    private TypeContainer typeContainer;

    @Column(name = "numero_container", unique = true, length = 100)
    private String numeroContainer;

    @Column(columnDefinition = "TEXT")
    private String description;
}
