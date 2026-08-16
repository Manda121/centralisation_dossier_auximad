package com.auximad.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    private String nomUtilisateur;
    private Integer codeAgence;
    private String motDePasse;
}
