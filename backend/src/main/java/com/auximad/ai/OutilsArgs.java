package com.auximad.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilitaires de construction de schémas JSON et de lecture des arguments reçus du modèle.
 *
 * Les petits modèles (qwen2.5:3b) envoient volontiers un nombre ou un booléen sous forme de
 * chaîne ("42", "oui"). Toutes les accessoires ci-dessous coercent tolérantement.
 */
public final class OutilsArgs {

    private OutilsArgs() {
    }

    // ─────────────── Construction des schémas ───────────────

    public static Map<String, Object> schema(Map<String, Object> proprietes, List<String> obligatoires) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", proprietes);
        schema.put("required", obligatoires == null ? List.of() : obligatoires);
        return schema;
    }

    public static Map<String, Object> prop(String type, String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", type);
        p.put("description", description);
        return p;
    }

    public static Map<String, Object> propEnum(String description, String... valeurs) {
        Map<String, Object> p = prop("string", description);
        p.put("enum", List.of(valeurs));
        return p;
    }

    public static Map<String, Object> props(Map.Entry<String, Map<String, Object>>... entrees) {
        Map<String, Object> proprietes = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Object>> e : entrees) {
            proprietes.put(e.getKey(), e.getValue());
        }
        return proprietes;
    }

    public static Map.Entry<String, Map<String, Object>> p(String nom, Map<String, Object> schema) {
        return Map.entry(nom, schema);
    }

    // ─────────────── Lecture des arguments ───────────────

    public static String texte(Map<String, Object> arguments, String cle) {
        Object v = arguments == null ? null : arguments.get(cle);
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    public static String texteObligatoire(Map<String, Object> arguments, String cle) {
        String v = texte(arguments, cle);
        if (v == null) {
            throw new IllegalArgumentException("Le paramètre « " + cle + " » est obligatoire.");
        }
        return v;
    }

    public static String texte(Map<String, Object> arguments, String cle, String defaut) {
        String v = texte(arguments, cle);
        return v != null ? v : defaut;
    }

    public static Integer entier(Map<String, Object> arguments, String cle) {
        Object v = arguments == null ? null : arguments.get(cle);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(s.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int entier(Map<String, Object> arguments, String cle, int defaut) {
        Integer v = entier(arguments, cle);
        return v != null ? v : defaut;
    }

    public static int entierObligatoire(Map<String, Object> arguments, String cle) {
        Integer v = entier(arguments, cle);
        if (v == null) {
            throw new IllegalArgumentException("Le paramètre numérique « " + cle + " » est obligatoire.");
        }
        return v;
    }

    public static boolean booleen(Map<String, Object> arguments, String cle, boolean defaut) {
        Object v = arguments == null ? null : arguments.get(cle);
        if (v == null) {
            return defaut;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(v).trim().toLowerCase();
        return switch (s) {
            case "true", "oui", "yes", "y", "1", "vrai", "on", "o" -> true;
            case "false", "non", "no", "n", "0", "faux", "off" -> false;
            default -> defaut;
        };
    }

    /** Accepte un scalaire ou une collection, et aplatit les chaînes séparées par des virgules. */
    public static List<String> textes(Map<String, Object> arguments, String cle) {
        Object v = arguments == null ? null : arguments.get(cle);
        List<String> out = new ArrayList<>();
        if (v == null) {
            return out;
        }
        if (v instanceof Collection<?> c) {
            for (Object o : c) {
                ajouter(out, o);
            }
        } else {
            ajouter(out, v);
        }
        return out;
    }

    private static void ajouter(List<String> out, Object o) {
        if (o == null) {
            return;
        }
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) {
            return;
        }
        // "12, 13" ou "12 ; 13" -> deux valeurs
        for (String part : s.split("[,;]")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
    }

    /** Comme {@link #textes} mais converti en entiers (accepte "[1, 2]" et "1, 2"). */
    public static List<Integer> entiers(Map<String, Object> arguments, String cle) {
        List<Integer> out = new ArrayList<>();
        for (String s : textes(arguments, cle)) {
            try {
                out.add(Integer.valueOf(s.replaceAll("[^0-9-]", "")));
            } catch (NumberFormatException ignored) {
                // valeur non numérique : ignorée silencieusement
            }
        }
        return out;
    }
}