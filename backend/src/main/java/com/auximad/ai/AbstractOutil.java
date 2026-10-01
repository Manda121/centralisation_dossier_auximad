package com.auximad.ai;

import java.util.Map;

/**
 * Base commune des outils : porte les métadonnées (nom, description, schéma, danger, niveau requis)
 * afin que chaque outil concret ne définisse que ce qu'il sait faire.
 */
public abstract class AbstractOutil implements OutilChat {

    private final String nom;
    private final String description;
    private final Map<String, Object> schema;
    private final NiveauDanger danger;
    private final int niveauRequis;

    protected AbstractOutil(String nom,
                            String description,
                            Map<String, Object> schema,
                            NiveauDanger danger,
                            int niveauRequis) {
        this.nom = nom;
        this.description = description;
        this.schema = schema;
        this.danger = danger;
        this.niveauRequis = niveauRequis;
    }

    @Override
    public String nom() {
        return nom;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Map<String, Object> schema() {
        return schema;
    }

    @Override
    public NiveauDanger danger() {
        return danger;
    }

    @Override
    public int niveauRequis() {
        return niveauRequis;
    }

    // ─────────── Raccourcis de schéma ───────────

    protected static Map<String, Object> schema(Map<String, Object> proprietes, String... obligatoires) {
        return OutilsArgs.schema(proprietes, java.util.List.of(obligatoires));
    }

    protected static Map<String, Object> str(String description) {
        return OutilsArgs.prop("string", description);
    }

    protected static Map<String, Object> entierProp(String description) {
        return OutilsArgs.prop("integer", description);
    }

    protected static Map<String, Object> booleenProp(String description) {
        return OutilsArgs.prop("boolean", description);
    }

    protected static Map<String, Object> listeDe(String type, String description) {
        Map<String, Object> p = OutilsArgs.prop("array", description);
        p.put("items", Map.of("type", type));
        return p;
    }
}