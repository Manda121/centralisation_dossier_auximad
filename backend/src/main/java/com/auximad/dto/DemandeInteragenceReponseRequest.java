package com.auximad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DemandeInteragenceReponseRequest {
    private Boolean acceptee;
    private String motifReponse;
}
