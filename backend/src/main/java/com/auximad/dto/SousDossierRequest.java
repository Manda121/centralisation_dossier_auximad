package com.auximad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO pour la création d'un sous-dossier inter-agences.
 * Seuls l'agence destination et le motif sont requis côté client.
 * Le reste est hérité du dossier mère.
 */
@Getter
@Setter
@NoArgsConstructor
public class SousDossierRequest {

    /** ID de l'agence qui va prendre en charge le sous-dossier */
    private Integer idAgence;

    /** Motif / raison de la délégation inter-agences */
    private String motif;

    /** ID du type de production (optionnel, hérite du dossier mère si absent) */
    private Integer idTypeProduction;
}
