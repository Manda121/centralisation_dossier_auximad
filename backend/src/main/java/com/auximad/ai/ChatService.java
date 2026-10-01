package com.auximad.ai;

import com.auximad.dto.ChatConfirmation;
import com.auximad.dto.ChatRequete;
import com.auximad.dto.ChatReponse;
import com.auximad.entity.Utilisateur;
import com.auximad.repository.UtilisateurRepository;
import com.auximad.security.JwtSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrateur de l'assistant conversationnel.
 *
 * Déroulement d'un tour de conversation :
 * <ol>
 *   <li>la question est analysée par le moteur RAG, qui injecte dans le prompt la connaissance
 *       de l'application <i>et</i> les lignes réelles de la base correspondant à l'intention ;</li>
 *   <li>le modèle est appelé avec la liste des outils que l'utilisateur a le droit d'invoquer ;</li>
 *   <li>les outils de lecture sont exécutés immédiatement et leur résultat est renvoyé au modèle,
 *       qui boucle tant qu'il demande des informations ;</li>
 *   <li>dès qu'un outilhors lecture est appelé, l'exécution est suspendue : une carte de confirmation
 *       est renvoyée à l'interface et l'appel n'est réellement exécuté qu'après validation
 *       explicite de l'utilisateur — le contrôle d'autorisation est refait à ce moment-là.</li>
 * </ol>
 *
 * Les conversations sont conservées en mémoire, par utilisateur, et expirent après une période
 * d'inactivité.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Nombre de tours de conversation conservés en mémoire comme contexte pour le modèle. */
    private static final int TOURS_CONSERVES = 8;

    /** Durée d'inactivité au-delà de laquelle une conversation est oubliée. */
    private static final long EXPIRATION_MS = 45 * 60 * 1000L;

    private final OllamaClient ollama;
    private final ChatOutilsRegistre registre;
    private final RagService rag;
    private final UtilisateurRepository utilisateurRepository;

    private final Map<Integer, Session> sessions = new ConcurrentHashMap<>();

    public ChatService(OllamaClient ollama,
                       ChatOutilsRegistre registre,
                       RagService rag,
                       UtilisateurRepository utilisateurRepository) {
        this.ollama = ollama;
        this.registre = registre;
        this.rag = rag;
        this.utilisateurRepository = utilisateurRepository;
    }

    // ══════════════════════════════ API publique ══════════════════════════════

    /** Traite une question de l'utilisateur. */
    public ChatReponse envoyer(ChatRequete requete) {
        ContexteChat contexte = contexteCourant();
        Session session = sessionPour(contexte);

        String question = requete == null ? null : requete.texte();
        if (requete != null && requete.doitEffacer()) {
            session.reinitialiser();
        }
        if (question == null || question.isBlank()) {
            return ChatReponse.simple("Posez votre question, par exemple « combien de dossiers sont en transit ? ».");
        }

        // Une décision est attendue : on refuse d'enchaîner une nouvelle question par-dessus.
        if (session.attente != null) {
            return enAttente(session);
        }

        ContexteRag contexteRag = rag.chercher(question, contexte);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(OllamaClient.messageSystem(promptSystem(contexte, contexteRag)));
        messages.addAll(session.historique);
        messages.add(OllamaClient.messageUtilisateur(question));

        List<String> outilsUtilises = new ArrayList<>();
        Boucle resultat;
        try {
            resultat = boucle(messages, contexte, outilsUtilises);
        } catch (OllamaClient.OllamaIndisponibleException e) {
            return ChatReponse.erreur(e.getMessage());
        }

        if (resultat.confirmation() != null) {
            // On mémorise l'état exact de la boucle pour la reprendre après décision.
            session.attente = new Attente(resultat.confirmation(), resultat.argumentsBloquants(), List.copyOf(messages));
            return new ChatReponse(
                    "Une confirmation est requise. Veuillez saisir votre identifiant d'utilisateur connecté (« "
                            + contexte.nomUtilisateur() + " ») pour valider l'opération : " + resultat.confirmation().resume(),
                    sourcesDe(contexteRag),
                    resultat.confirmation(),
                    List.copyOf(outilsUtilises),
                    null);
        }

        String reponse = reponseFinale(resultat.reponse());
        memoriserTour(session, question, reponse);
        return new ChatReponse(reponse, sourcesDe(contexteRag), null, List.copyOf(outilsUtilises), null);
    }

    /** Applique la décision de l'utilisateur sur l'opération suspendue. */
    public ChatReponse confirmer(ChatConfirmation confirmation) {
        ContexteChat contexte = contexteCourant();
        Session session = sessionPour(contexte);

        Attente attente = session.attente;
        if (attente == null) {
            return ChatReponse.simple("Aucune opération n'est en attente de confirmation.");
        }
        if (confirmation == null) {
            return ChatReponse.simple("Aucune décision n'a été transmise, l'opération reste en attente.");
        }
        if (confirmation.id() != null
                && !confirmation.id().equals(attente.confirmation().id())) {
            return ChatReponse.simple("Cette demande de confirmation a expiré, reposez votre demande.");
        }

        if (!confirmation.estAccepte()) {
            session.attente = null;
            List<Map<String, Object>> messages = new ArrayList<>(attente.messages());
            List<String> outilsUtilises = new ArrayList<>();
            messages.add(OllamaClient.messageOutil(attente.confirmation().outil(),
                    "L'utilisateur a refusé cette opération. N'insiste pas, confirme le refus et propose une alternative."));
            Boucle apresRefus;
            try {
                apresRefus = boucle(messages, contexte, outilsUtilises);
            } catch (OllamaClient.OllamaIndisponibleException e) {
                return ChatReponse.erreur(e.getMessage());
            }
            if (apresRefus.confirmation() != null) {
                session.attente = new Attente(apresRefus.confirmation(), apresRefus.argumentsBloquants(),
                        List.copyOf(messages));
                return new ChatReponse("Saisissez votre identifiant d'utilisateur connecté pour confirmer cette opération.", List.of(),
                        apresRefus.confirmation(), List.copyOf(outilsUtilises), null);
            }
            String reponse = reponseFinale(apresRefus.reponse());
            return new ChatReponse(reponse, List.of(), null, List.copyOf(outilsUtilises), null);
        }

        // Vérification stricte de l'identifiant de l'utilisateur connecté
        String identifiantAttendu = contexte.nomUtilisateur();
        String identifiantSaisi = confirmation.identifiant() == null ? "" : confirmation.identifiant().trim();

        if (identifiantSaisi.isEmpty() || !identifiantSaisi.equalsIgnoreCase(identifiantAttendu)) {
            log.warn("Confirmation refusée : identifiant attendu='{}', saisi='{}'", identifiantAttendu, identifiantSaisi);
            // Annulation de la demande en attente
            session.attente = null;
            return ChatReponse.erreur("Identifiant d'utilisateur incorrect (« " + (identifiantSaisi.isEmpty() ? "aucun" : identifiantSaisi)
                    + " »). L'identifiant de l'utilisateur connecté (« " + identifiantAttendu + " ») était attendu. L'opération en base a été annulée.");
        }

        session.attente = null;

        List<Map<String, Object>> messages = new ArrayList<>(attente.messages());
        List<String> outilsUtilises = new ArrayList<>();

        // Validation : on contrôle de nouveau les droits, l'utilisateur a pu changer de profil.
        String resultat = executerOutil(attente.confirmation().outil(), attente.arguments(), contexte, outilsUtilises);

        // Le résultat doit réintégrer la conversation : sans ce message « tool », le modèle
        // reçoit un appel d'outil sans réponse et ne peut pas reformuler la confirmation.
        messages.add(OllamaClient.messageOutil(attente.confirmation().outil(), resultat));

        Boucle final_;
        try {
            final_ = boucle(messages, contexte, outilsUtilises);
        } catch (OllamaClient.OllamaIndisponibleException e) {
            return ChatReponse.erreur(e.getMessage());
        }
        if (final_.confirmation() != null) {
            session.attente = new Attente(final_.confirmation(), final_.argumentsBloquants(), List.copyOf(messages));
            return new ChatReponse("Saisissez votre identifiant d'utilisateur connecté pour confirmer cette opération.", List.of(),
                    final_.confirmation(), List.copyOf(outilsUtilises), null);
        }

        String reponse = reponseFinale(final_.reponse());
        return new ChatReponse(reponse, List.of(), null, List.copyOf(outilsUtilises), null);
    }

    /** Repart d'une conversation vierge. */
    public ChatReponse nouvelleConversation() {
        sessionPour(contexteCourant()).reinitialiser();
        return ChatReponse.simple("Nouvelle conversation. Que puis-je faire pour vous ?");
    }

    /** Exemples de questions, adaptés au périmètre de l'utilisateur. */
    public List<String> suggestions() {
        ContexteChat contexte = contexteCourant();
        List<String> liste = new ArrayList<>();
        liste.add("Combien de dossiers ai-je en cours ?");
        liste.add("Quels sont mes dossiers urgents ?");
        liste.add("Résume la situation d'un dossier");
        liste.add("Y a-t-il des demandes d'inter-agence en attente ?");
        if (contexte.niveauAutorisation() >= Resolveur.NIVEAU_ECRITURE) {
            liste.add("Fais passer un dossier à la phase suivante");
        }
        return liste;
    }

    /** Outils exposés à l'utilisateur (lecture seule pour l'affichage). */
    public List<Map<String, Object>> outils() {
        return registre.apercu(contexteCourant());
    }

    /** État du service, pour la page d'administration et le widget. */
    public Map<String, Object> etat() {
        Map<String, Object> etat = new LinkedHashMap<>();
        boolean disponible = ollama.estDisponible();
        etat.put("disponible", disponible);
        etat.put("modele", ollama.modele());
        etat.put("baseUrl", ollama.baseUrl());
        etat.put("nbOutils", registre.disponibles(contexteCourant()).size());
        etat.put("sessions", sessions.size());
        if (disponible) {
            etat.put("modelesInstalles", ollama.modeles());
        }
        return etat;
    }

    // ══════════════════════════════ Boucle d'outils ══════════════════════════════

    private record Boucle(String reponse, ChatReponse.Confirmation confirmation,
                          Map<String, Object> argumentsBloquants) {
    }

    /**
     * Fait tourner le modèle tant qu'il demande des outils.
     * Suspend le traitement dès qu'un appel nécessite une confirmation humaine.
     */
    private Boucle boucle(List<Map<String, Object>> messages, ContexteChat contexte,
                          List<String> outilsUtilises) {

        int max = Math.max(ollama.maxIterations(), 1);
        for (int tour = 0; tour < max; tour++) {
            OllamaClient.Reponse reponse = ollama.chatting(messages, registre.declarations(contexte));
            List<OllamaClient.AppelOutil> appels = reponse.appelsOutils();

            if (!reponse.aAppelsOutils()) {
                return new Boucle(reponse.contenu(), null, null);
            }

            messages.add(OllamaClient.messageAssistantAvecOutils(reponse.contenu(),
                    OllamaClient.serialiserAppels(appels)));

            // Un seul appel bloquant à la fois : le modèle doit obtenir le résultat avant de continuer.
            for (OllamaClient.AppelOutil appel : appels) {
                OutilChat outil = registre.parNom(appel.nom());
                if (outil == null) {
                    // Outil non exposé (droits insuffisants ou inexistant) : on le dit au modèle.
                    messages.add(OllamaClient.messageOutil(appel.nom(),
                            "ERREUR : cet outil n'est pas disponible pour votre profil. Réponds à l'utilisateur "
                                    + "en lui expliquant qu'il ne dispose pas des droits nécessaires."));
                    continue;
                }
                if (outil.danger() != NiveauDanger.LECTURE) {
                    Map<String, Object> args = appel.arguments() == null ? Map.of() : appel.arguments();
                    return new Boucle(null, confirmation(outil, args, contexte), args);
                }
                messages.add(OllamaClient.messageOutil(appel.nom(),
                        executerOutil(appel.nom(), appel.arguments(), contexte, outilsUtilises)));
            }
        }
        return new Boucle("Cette demande demande trop d'étapes pour être traitée en une fois. "
                + "Reformulez-la plus simplement, ou traitons-la en deux temps.", null, null);
    }

    /** Exécute un outil en capturant ses erreurs pour que le modèle puisse se corriger. */
    private String executerOutil(String nomOutil, Map<String, Object> arguments, ContexteChat contexte,
                                 List<String> outilsUtilises) {
        OutilChat outil = registre.parNom(nomOutil);
        if (outil == null) {
            return "ERREUR : outil « " + nomOutil + " » inconnu.";
        }
        // Défense en profondeur : même si le modèle tente un appel non exposé,
        // le niveau d'autorisation est revérifié à l'exécution.
        if (contexte.niveauAutorisation() < outil.niveauRequis()) {
            return "ERREUR : votre profil ne dispose pas des droits nécessaires pour cette action.";
        }
        try {
            String resultat = outil.executer(arguments == null ? Map.of() : arguments, contexte);
            if (outilsUtilises != null && !outilsUtilises.contains(nomOutil)) {
                outilsUtilises.add(nomOutil);
            }
            return resultat == null ? "(aucun résultat)" : resultat;
        } catch (SecurityException e) {
            return "ACCÈS REFUSÉ : " + e.getMessage();
        } catch (IllegalArgumentException e) {
            return "OPÉRATION IMPOSSIBLE : " + e.getMessage();
        } catch (Exception e) {
            log.warn("Outil « {} » en échec : {}", nomOutil, e.getMessage(), e);
            return "ERREUR TECHNIQUE : l'opération n'a pas pu être exécutée (" + e.getMessage() + ").";
        }
    }

    // ══════════════════════════════ Confirmations ══════════════════════════════

    private record Attente(ChatReponse.Confirmation confirmation,
                           Map<String, Object> arguments,
                           List<Map<String, Object>> messages) {
    }

    private ChatReponse.Confirmation confirmation(OutilChat outil, Map<String, Object> arguments,
                                                 ContexteChat contexte) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        String resume;
        try {
            resume = outil.resume(args, contexte);
        } catch (Exception e) {
            resume = outil.nom() + " " + args;
        }
        List<String> details = new ArrayList<>();
        args.forEach((cle, valeur) -> details.add(cle + " : " + valeur));
        return new ChatReponse.Confirmation(UUID.randomUUID().toString(), outil.nom(), resume,
                outil.danger().name(), details);
    }

    private ChatReponse enAttente(Session session) {
        ChatReponse.Confirmation c = session.attente.confirmation();
        String message = "L'opération suivante modifie la base de données : " + c.resume()
                + ". Veuillez saisir votre identifiant d'utilisateur connecté (« " + session.attente.confirmation().resume()
                + " ») pour valider l'exécution en base.";
        return new ChatReponse(message, List.of(), c, List.of(), null);
    }

    // ══════════════════════════════ Prompt système ══════════════════════════════

    private String promptSystem(ContexteChat contexte, ContexteRag contexteRag) {
        StringBuilder sb = new StringBuilder();

        sb.append("Tu es l'assistant intelligent et proactif de l'application Auximad, logiciel de gestion des dossiers de transit ")
                .append("et des ordres de transit douaniers.\n\n");

        sb.append("INTERLOCUTEUR\n")
                .append("- Utilisateur : ").append(contexte.nomComplet())
                .append(" (identifiant d'utilisateur connecté : « ").append(contexte.nomUtilisateur()).append(" », ID: ").append(contexte.idUtilisateur()).append(")\n")
                .append("- Rôle : ").append(contexte.role()).append('\n')
                .append("- Agence : ").append(contexte.nomAgence()).append('\n')
                .append("- Périmètre des données : ")
                .append(contexte.peutVoirToutesAgences()
                        ? "toutes les agences."
                        : "uniquement l'agence " + contexte.nomAgence() + ".")
                .append("\n\n");

        sb.append("RÈGLES ET COMPORTEMENT DE L'ASSISTANT\n")
                .append("1. Réponds en français, de façon concise, courtoise et structurée (avec listes à puces pour les étapes).\n")
                .append("2. N'invente jamais un numéro, un nom, une date ou un montant : utilise les outils "
                        + "pour obtenir la donnée réelle. Si un outil ne renvoie rien, dis-le clairement.\n")
                .append("3. Les chiffres et données du CONTEXTE ci-dessous représentent l'état réel de la base à cet instant.\n")
                .append("4. Appelle un outil dès que la réponse dépend de la base (statistiques, dossier, client, "
                        + "ordre de transit, documents, historique, demandes inter-agences).\n")
                .append("5. Une seule opération d'écriture à la fois.\n")
                .append("6. SÉCURITÉ MODIFICATION BASE DE DONNÉES : Toutes les opérations de création, modification ou suppression "
                        + "sont réellement exécutées en base après vérification de l'identifiant de l'utilisateur connecté (« "
                        + contexte.nomUtilisateur() + " »). Lorsque tu demandes d'exécuter une action modifiant la base, "
                        + "l'interface demandera la saisie de l'identifiant utilisateur.\n")
                .append("7. RÔLE D'ASSISTANT ET WORKFLOWS OPÉRATIONNELS :\n")
                .append("   - CRÉATION DE DOSSIER : 1. Rechercher/Créer le client -> 2. Créer ou associer l'Ordre de Transit -> 3. Créer le dossier avec nomenclature automatique -> 4. Assigner les documents obligatoires.\n")
                .append("   - FINALISATION / CLÔTURE DE DOSSIER : 1. Obtenir le détail du dossier -> 2. Vérifier que les documents obligatoires sont tous remis (documents_du_dossier) -> 3. Effectuer la transition de phase jusqu'à livraison -> 4. Changer le statut à « Clôturé ».\n")
                .append("   - ASSISTANCE GUIDÉE : Guide l'utilisateur pas-à-pas à chaque étape et propose d'exécuter les actions requises pour lui en base.\n")
                .append("8. Ne demande que les informations réellement manquantes, et une par une si elles sont nombreuses.\n")
                .append("9. Si l'utilisateur ne dispose pas des droits nécessaires, indique l'écran applicatif où effectuer l'action.\n\n");

        sb.append("OUTIL DISPONIBLES\n");
        for (OutilChat outil : registre.disponibles(contexte)) {
            sb.append("- ").append(outil.nom()).append(" : ").append(outil.description()).append('\n');
        }

        if (!contexteRag.estVide()) {
            sb.append("\nCONTEXTE RÉCUPÉRÉ POUR CETTE QUESTION\n");
            sb.append(contexteRag.texte());
        }
        return sb.toString();
    }

    // ══════════════════════════════ Sessions ══════════════════════════════

    private static final class Session {
        private final List<Map<String, Object>> historique = new ArrayList<>();
        private Attente attente;
        private long dernierAcces = System.currentTimeMillis();

        void memoriser(String question, String reponse) {
            historique.add(OllamaClient.messageUtilisateur(question));
            historique.add(OllamaClient.messageAssistant(reponse));
            // On borne l'historique : le modèle a une fenêtre de contexte limitée.
            int maxMessages = TOURS_CONSERVES * 2;
            while (historique.size() > maxMessages) {
                historique.remove(0);
            }
        }

        void reinitialiser() {
            historique.clear();
            attente = null;
            dernierAcces = System.currentTimeMillis();
        }

        void toucher() {
            dernierAcces = System.currentTimeMillis();
        }
    }

    private Session sessionPour(ContexteChat contexte) {
        Integer idUtilisateur = contexte.idUtilisateur();
        if (idUtilisateur == null) {
            // Aucune identité exploitable : conversation jetable, non mémorisée.
            return new Session();
        }
        purge();
        return sessions.computeIfAbsent(idUtilisateur, id -> new Session());
    }

    /** Oublie les conversations inactives. */
    private void purge() {
        long maintenant = System.currentTimeMillis();
        sessions.entrySet().removeIf(e -> maintenant - e.getValue().dernierAcces > EXPIRATION_MS);
    }

    private ContexteChat contexteCourant() {
        Integer idUtilisateur = JwtSecurityContext.getIdUtilisateur();
        if (idUtilisateur == null) {
            throw new SecurityException("Session expirée : reconnectez-vous pour utiliser l'assistant.");
        }
        Utilisateur utilisateur = utilisateurRepository.findById(idUtilisateur)
                .orElseThrow(() -> new SecurityException("Utilisateur introuvable."));
        return new ContexteChat(utilisateur, JwtSecurityContext.getNiveauAutorisation());
    }

    // ══════════════════════════════ Utilitaires ══════════════════════════════

    private void memoriserTour(Session session, String question, String reponse) {
        session.memoriser(question, reponse);
        session.toucher();
    }

    /** Le modèle peut répondre par un texte vide s'il a épuisé ses appels d'outils. */
    private String reponseFinale(String contenu) {
        if (contenu == null || contenu.isBlank()) {
            return "Je n'ai pas pu formuler de réponse à partir des informations disponibles.";
        }
        return contenu.trim();
    }

    private List<String> sourcesDe(ContexteRag contexteRag) {
        List<String> sources = new ArrayList<>();
        for (ContexteRag.Source s : contexteRag.sources()) {
            String libelle = s.detail() == null || s.detail().isBlank()
                    ? s.libelle()
                    : s.libelle() + " — " + s.detail();
            sources.add(capitaliser(s.type()) + " : " + libelle);
        }
        return sources;
    }

    private static String capitaliser(String type) {
        if (type == null || type.isBlank()) {
            return "Source";
        }
        return type.substring(0, 1).toUpperCase(Locale.FRENCH) + type.substring(1);
    }
}