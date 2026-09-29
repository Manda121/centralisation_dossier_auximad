package com.auximad.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class DeclarationRequest {
    private Integer dossierId;
    private String numeroDeclaration;
    private LocalDateTime dateDeclaration;
    private Integer bureauId;
    private Integer typeDeclarationId;
    private String description;
}
