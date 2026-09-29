package com.auximad.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_mvt")
@Getter
@Setter
@NoArgsConstructor
public class DocumentMvt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mvt_dossier", nullable = false)
    private MvtDossier mvtDossier;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_document", nullable = false)
    private Document document;
}
