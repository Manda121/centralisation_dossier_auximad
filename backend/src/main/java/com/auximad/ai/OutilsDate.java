package com.auximad.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Interprétation tolérante des dates citées par l'utilisateur ou par le modèle.
 * Les petits modèles produisent indifféremment « 2026-10-15 », « 15/10/2026 »,
 * « 2026-10-15T10:30 » ou « demain ».
 */
public final class OutilsDate {

    private OutilsDate() {
    }

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
    };

    private static final DateTimeFormatter[] FORMATS_DATE = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    };

    /**
     * @return la date parsed, ou {@code defaut} si la valeur est absente ou illisible
     */
    public static LocalDateTime parser(String valeur, LocalDateTime defaut) {
        if (valeur == null || valeur.isBlank()) {
            return defaut;
        }
        String v = valeur.trim();

        // Formules relatives
        String norm = BaseConnaissance.normaliser(v);
        LocalDateTime maintenant = LocalDateTime.now();
        switch (norm) {
            case "aujourdhui", "aujourd hui", "ce jour", "maintenant" -> {
                return maintenant;
            }
            case "demain" -> {
                return maintenant.plusDays(1);
            }
            case "hier" -> {
                return maintenant.minusDays(1);
            }
            case "apres demain" -> {
                return maintenant.plusDays(2);
            }
            case "dans une semaine", "dans 7 jours", "sous 7 jours" -> {
                return maintenant.plusDays(7);
            }
            case "dans un mois", "dans 30 jours" -> {
                return maintenant.plusDays(30);
            }
            default -> {
                // pas de formule relative reconnue
            }
        }

        for (DateTimeFormatter f : FORMATS) {
            try {
                return LocalDateTime.parse(v, f);
            } catch (Exception ignored) {
                // on essaie le format suivant
            }
        }
        for (DateTimeFormatter f : FORMATS_DATE) {
            try {
                LocalDate d = LocalDate.parse(v, f);
                // Une date sans heure est interprétée en fin de journée (une échéance de
                // remise saisie en date seule vaut pour le jour entier).
                return LocalDateTime.of(d, LocalTime.of(23, 59, 59));
            } catch (Exception ignored) {
                // on essaie le format suivant
            }
        }
        return defaut;
    }

    public static LocalDateTime debutJour(String valeur, LocalDateTime defaut) {
        LocalDateTime dt = parser(valeur, null);
        if (dt == null) {
            return defaut;
        }
        return dt.toLocalDate().atStartOfDay();
    }

    public static LocalDateTime finJour(String valeur, LocalDateTime defaut) {
        LocalDateTime dt = parser(valeur, null);
        if (dt == null) {
            return defaut;
        }
        return dt.toLocalDate().atTime(23, 59, 59);
    }

    public static String iso(LocalDateTime dt) {
        return dt == null ? null : dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public static String lisible(LocalDateTime dt) {
        return dt == null ? "non définie" : dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public static String lisible(LocalDate d) {
        return d == null ? "non définie" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}