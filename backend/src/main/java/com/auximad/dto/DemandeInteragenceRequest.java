package com.auximad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DemandeInteragenceRequest {
    private Integer idDossier;
    private Integer idAgenceDestinataire;
    private String motif;
}
