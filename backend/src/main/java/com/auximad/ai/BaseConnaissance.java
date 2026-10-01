package com.auximad.ai;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Base de connaissances documentaire de l'application (partie « savoir » du RAG).
 * Chaque fragment est retrievable par mots-clés et injecté dans le prompt du modèle
 * quand la question de l'utilisateur porte sur le fonctionnement de l'application.
 */
public final class BaseConnaissance {

    /** Un fragment de connaissance. */
    public record Fragment(String id, String titre, String texte, String motsCles) {
    }

    private BaseConnaissance() {
    }

    private static final List<Fragment> FRAGMENTS = List.of(
            new Fragment("vue-ensemble",
                    "Présentation de l'application Auximad",
                    "Auximad est une application de centralisation des dossiers de transit (transitaire / assurance). "
                            + "Elle gère le cheminement complet d'une marchandise : le client, l'ordre de transit (OT), "
                            + "les marchandises, les conteneurs, puis le dossier de transit subdivided en phases, "
                            + "avec ses documents obligatoires, ses échéances et sa provision. "
                            + "Les agences travaillent en silos : un dossier appartient à une agence, et le module "
                            + "Inter-agences permet de faire travailler plusieurs agences sur un même dossier via "
                            + "une demande d'inter-agence et un sous-dossier.",
                    "presentation application auximad centralisation transit dossier agence module"),

            new Fragment("modules",
                    "Modules et écrans de l'application",
                    "Écrans disponibles : Tableau de bord (indicateurs), Clients (fiche client + dossiers), "
                            + "Ordres de Transit (liste, assistant de création en 3 étapes, détail), Marchandises "
                            + "(par OT), Conteneurs (par dossier), Dossiers (liste, création, détail), "
                            + "Dossiers inter-agences (dossier mère / sous-dossier), Demandes inter-agences, "
                            + "Documents (remise + upload par dossier), Mouvements de dossier (journal des "
                            + "opérations effectuées sur un dossier), Historiques (journal d'audit global) et "
                            + "Configuration système (administration des référentiels, utilisateurs, rôles, "
                            + "autorisations — réservé aux Administrateurs).",
                    "module ecran menu page liste detail creation assistant historique administration referentiel"),

            new Fragment("dossier",
                    "Le dossier de transit",
                    "Un dossier est la pièce maîtresse : il porte un numéro unique, un client, un type de production "
                            + "(donc un mode de transport), éventuellement un ordre de transit, une phase, un statut, "
                            + "une échéance, une date de livraison, des pays origine/destination, un Incoterm, "
                            + "et des indicateurs financiers (prépayé, provisionné, provision demandée). "
                            + "Un dossier appartient à une agence et est créé par un utilisateur. "
                            + "Il ne peut y avoir qu'un dossier par ordre de transit et par agence.",
                    "dossier transit phase statut provision echeance livraison pays incoterm prepaye"),

            new Fragment("nomenclature",
                    "Nomenclature automatique du numéro de dossier",
                    "Le numéro de dossier est généré automatiquement par l'application selon la formule : "
                            + "code du type de production + code de l'agence (2 chiffres) + mois (2 chiffres) "
                            + "+ dernier chiffre de l'année + chronologie (4 chiffres). "
                            + "Exemple : 30-70-01-6-0001 signifie type de production 30, agence 70, janvier, "
                            + "année 2016, première création. Le numéro est unique et non modifiable.",
                    "numero nomenclature format code agence mois annee chronologie generation automatique"),

            new Fragment("phases-statuts",
                    "Phases, statuts et mouvements de dossier",
                    "Chaque dossier progresse à travers des phases (par exemple ouverture, transit, douane, "
                            + "livraison, clôture) et porte un statut (Ouvert, Clôturé, Annulé, En litige). "
                            + "Un changement de phase ou de statut doit être tracé par un mouvement de dossier : "
                            + "le mouvement enregistre l'ancienne et la nouvelle phase, l'ancien et le nouveau statut, "
                            + "l'utilisateur, son adresse IP, l'opération réalisée et un commentaire. "
                            + "Les opérations disponibles dépendent du type de production et sont ordonnées par niveau.",
                    "phase statut mouvement operation progression workflow cloture litige annule"),

            new Fragment("documents",
                    "Documents obligatoires et remises",
                    "Chaque type de production définit une liste de types de documents, dont certains sont "
                            + "obligatoires. Pour un dossier, on « remet » un document en l'assignant : une ligne "
                            + "est créée dans la table des remises avec une date de remise, et un fichier peut être "
                            + "téléversé (.pdf, .doc, .docx, images). Un document retiré est désactivé et sa remise "
                            + "supprimée. Un dossier est complet quand tous ses documents obligatoires sont remis. "
                            + "La liste « Documents » signale les dossiers incomplets et les remises urgentes.",
                    "document piece justificatif facture obligatoire remise date upload fichier retirer incomplet"),

            new Fragment("interagences",
                    "Demandes et sous-dossiers inter-agences",
                    "Lorsqu'une agence a besoin qu'une autre agence intervienne sur un dossier, elle crée une demande "
                            + "d'inter-agence en précisant l'agence destinataire et un motif. La demande passe par trois "
                            + "statuts : EN_ATTENTE, ACCEPTEE ou REFUSEE. Seule l'agence destinataire (ou un profil "
                            + "ayant une portée toutes-agences) peut répondre. Si la demande est acceptée, "
                            + "l'application crée automatiquement un sous-dossier rattaché au dossier initial : "
                            + "le dossier mère est traité par une agence, le sous-dossier par l'autre.",
                    "interagence demande sous-dossier mere agence destinataire acceptation refus motif"),

            new Fragment("transit",
                    "Ordres de transit, marchandises et conteneurs",
                    "Un ordre de transit (OT) est la demande de transport faite par un client : il porte un numéro "
                            + "automatique, un numéro de commande, un expéditeur, un destinataire, un régime, un type de "
                            + "production, un bureau de dédouanement, une destination finale et un statut (brouillon, "
                            + "validée, supprimée). On peut lui rattacher des marchandises (nature, marque, modèle, "
                            + "quantité, poids, volume, unité de mesure), un contrat de transport (compagnie, navire, "
                            + "numéro de vol, dates prévues) et des instructions. Les conteneurs ont un numéro unique, "
                            + "un type (dimensions, poids max) et peuvent être affectés à un ou plusieurs dossiers.",
                    "ordre transit ot marchandise conteneur cargaison compagnie navire contrat instruction transport"),

            new Fragment("autorisations",
                    "Rôles, portées et niveaux d'autorisation",
                    "Les droits sont calculés par la formule : niveau d'autorisation >= (portée + code d'action). "
                            + "Portées opérationnelles sur sa propre agence : lecture 100, écriture 200, "
                            + "suppression 300, validation 400. Portées sur toutes les agences : lecture 1000, "
                            + "écriture 2000, suppression 3000, validation 4000. Codes d'actions : "
                            + "ordre de transit / dossier / marchandise 10, sous-dossier et inter-agence 20, "
                            + "traitement et embarquement 30, comptabilité 40, gestion des rôles 50. "
                            + "Le rôle Administrateur possède le niveau le plus élevé et accède à la configuration "
                            + "système ; un profil Lecteur est limité à la consultation.",
                    "autorisation permission niveau role admin agent lecteur validateur chef agence portee lecture ecriture suppression"),

            new Fragment("audit",
                    "Historique et audit",
                    "Toutes les actions de l'application sont journalisées dans l'historique : utilisateur, action "
                            + "(visualisation, création, modification, suppression, validation, connexion, déconnexion), "
                            + "adresse IP, machine, logiciel utilisé, URL appelée, statut HTTP, entité concernée et "
                            + "détails. L'historique sert de piste d'audit et permet de savoir qui a fait quoi et quand.",
                    "historique journal audit trace piste utilisateur ip machine logiciel connexion action"),

            new Fragment("chatbot",
                    "Comment fonctionne cet assistant",
                    "Cet assistant interroge les données réelles de l'application : il n'invente jamais un numéro, "
                            + "un client ou un montant. Pour répondre, il utilise des outils (recherche de dossiers, "
                            + "de clients, d'ordres de transit, consultation des documents, de l'historique, "
                            + "tableaux de bord…) et n'affirme que ce que ces outils lui ont retourné. "
                            + "Toute opération qui écrit dans la base (changement de phase, remise de document, "
                            + "réponse à une demande d'inter-agence, suppression…) n'est jamais exécutée sans votre "
                            + "confirmation explicite. Si vous n'avez pas les droits nécessaires, l'assistant vous "
                            + "le dira et vous orientera vers l'écran correspondant.",
                    "assistant chatbot question outil reponse donnees reelle confirmation ecriture")
    );

    public static List<Fragment> tous() {
        return FRAGMENTS;
    }

    /**
     * Récupère les fragments les plus pertinents pour une question.
     * Le fragment « vue-ensemble » est toujours renvoyé en premier (le modèle a besoin du cadre général).
     */
    public static List<Fragment> rechercher(String questionNormalisee, int max) {
        Set<String> motsQuestion = motsCles(questionNormalisee);

        List<Fragment> classes = new ArrayList<>();
        for (Fragment f : FRAGMENTS) {
            int score = 0;
            for (String mot : motsQuestion) {
                if (f.motsCles().contains(" " + mot + " ") || f.titre().toLowerCase().contains(mot)) {
                    score++;
                }
            }
            classes.add(new Fragment(f.id(), f.titre(), f.texte(), Integer.toString(score)));
        }
        classes.sort((a, b) -> Integer.compare(score(b), score(a)));

        List<Fragment> retenus = new ArrayList<>();
        for (Fragment f : FRAGMENTS) {
            if (f.id().equals("vue-ensemble")) {
                retenus.add(f);
                break;
            }
        }
        for (Fragment f : classes) {
            if (retenus.size() >= max) {
                break;
            }
            if (!retenus.contains(f)) {
                retenus.add(f);
            }
        }
        return retenus;
    }

    private static int score(Fragment f) {
        try {
            return Integer.parseInt(f.motsCles());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Normalise une chaîne : minuscules, sans accents, sans apostrophes. */
    public static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texte.length());
        for (char c : texte.toLowerCase(Locale.FRENCH).toCharArray()) {
            switch (c) {
                case 'à', 'â', 'ä' -> sb.append('a');
                case 'é', 'è', 'ê', 'ë' -> sb.append('e');
                case 'î', 'ï' -> sb.append('i');
                case 'ô', 'ö' -> sb.append('o');
                case 'û', 'ü' -> sb.append('u');
                case 'ç' -> sb.append('c');
                case 'æ' -> sb.append("ae");
                case '\'' -> sb.append(' ');
                default -> sb.append(c);
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static Set<String> motsCles(String texteNormalise) {
        Set<String> mots = new LinkedHashSet<>();
        for (String mot : texteNormalise.split("[^a-z0-9]+")) {
            if (mot.length() >= 3) {
                mots.add(mot);
            }
        }
        return mots;
    }
}