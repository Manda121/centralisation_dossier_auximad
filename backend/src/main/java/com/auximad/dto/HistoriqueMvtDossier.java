package com.auximad.dto;

import com.auximad.entity.MvtDossier;
import com.auximad.entity.Utilisateur;

import java.time.LocalDateTime;

public record HistoriqueMvtDossier(
        String id,
        String typeEvenement,
        LocalDateTime dateMvt,
        MvtDossier mouvement,
        Utilisateur utilisateur,
        String motif
) {}
