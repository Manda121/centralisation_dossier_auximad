package com.auximad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MvtDossierRequest {
    private Integer dossierId;
    private Integer operationId;
    private LocalDateTime dateMvt;
    private String commentaire;
}
