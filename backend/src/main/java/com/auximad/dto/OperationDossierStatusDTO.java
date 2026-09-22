package com.auximad.dto;

import com.auximad.entity.Declaration;
import com.auximad.entity.Document;
import com.auximad.entity.OperationDossier;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OperationDossierStatusDTO {

    private OperationDossier operation;
    private Integer mvtId;
    private LocalDateTime dateMvt;
    private Boolean estFait = false;
    private Boolean estBloque = false;
    private OperationDossier operationPrecedenteObligatoire;
    private Document document;
    private Declaration declaration;
}
