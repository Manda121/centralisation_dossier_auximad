package com.auximad.dto;

import lombok.Getter;
import lombok.Setter;

/** Optional filters combined with the existing dossier search criteria. */
@Getter
@Setter
public class DossierFiltres {
    private Boolean documentsIncomplets;
    private String numeroDossier;
    private String clientNom;
    private String cpClient;
    private Integer numeroOt;
    private Integer paysOrigineId;
    private Integer paysDestinationId;
    private Integer incontermeId;
    private String pourCompte;
    private String depart;
    private String adresseLivraison;
    private String lieuPaiementFret;
    private Boolean estPrepaye;
    private Boolean estProvisionne;
    private java.math.BigDecimal provisionMin;
    private java.math.BigDecimal provisionMax;
    private String echeanceMin;
    private String echeanceMax;
    private String dateLivraisonMin;
    private String dateLivraisonMax;
}
