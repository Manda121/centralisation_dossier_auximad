package com.auximad.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String token;

    @Builder.Default
    private String type = "Bearer";

    private Integer id;
    private Long expiration;
    private String nomUtilisateur;
    private String nomComplet;
    private String nom;
    private String prenom;
    private String role;
    private String agence;
    private Integer codeAgence;
    private Integer idAgence;
    private Integer niveauAutorisation;
}
