package com.auximad.ai;

import com.auximad.entity.Utilisateur;
import com.auximad.security.AutorisationHelper;

/**
 * Contexte d'exécution d'un outil : l'utilisateur connecté et ses droits.
 * Les outils ne lisent jamais le SecurityContext directement, ils passent par ce contexte,
 * ce qui garantit que l'on agit toujours au nom de l'utilisateur qui a clique sur « Envoyer ».
 */
public class ContexteChat {

    private final Utilisateur utilisateur;
    private final int niveauAutorisation;

    public ContexteChat(Utilisateur utilisateur, int niveauAutorisation) {
        this.utilisateur = utilisateur;
        this.niveauAutorisation = niveauAutorisation;
    }

    public Utilisateur utilisateur() {
        return utilisateur;
    }

    public Integer idUtilisateur() {
        return utilisateur != null ? utilisateur.getId() : null;
    }

    public String nomUtilisateur() {
        return utilisateur != null ? utilisateur.getNomUtilisateur() : "inconnu";
    }

    public String nomComplet() {
        return utilisateur != null ? utilisateur.getNomComplet() : "Utilisateur";
    }

    public Integer idAgence() {
        return (utilisateur != null && utilisateur.getAgence() != null) ? utilisateur.getAgence().getId() : null;
    }

    public String nomAgence() {
        return (utilisateur != null && utilisateur.getAgence() != null) ? utilisateur.getAgence().getNom() : "non renseignée";
    }

    public String role() {
        return (utilisateur != null && utilisateur.getRole() != null) ? utilisateur.getRole().getNom() : "non renseigné";
    }

    public int niveauAutorisation() {
        return niveauAutorisation;
    }

    /** Même formule que le reste de l'application : niveau >= (portée + codeAction). */
    public boolean aPermission(int portee, int codeAction) {
        return AutorisationHelper.verifierPermission(niveauAutorisation, portee, codeAction);
    }

    /** true si l'utilisateur a une portée lui permettant de voir toutes les agences. */
    public boolean peutVoirToutesAgences() {
        return aPermission(AutorisationHelper.PORTEE_TOUTES_LECTURE, AutorisationHelper.ACTION_OT);
    }
}