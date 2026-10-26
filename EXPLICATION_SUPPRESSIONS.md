# Comprendre les suppressions et les liens entre tables

## Le problème, avec un exemple simple

Dans la base, une ligne peut être **référencée par d'autres lignes**. Par exemple, un dossier peut contenir `id_ordre_transit = 10`. Cela signifie : « ce dossier est lié à l'ordre de transit 10 ».

```text
ordre_transit                  dossier
id = 10        <-----------    id = 25, id_ordre_transit = 10
                 référence
```

La colonne `dossier.id_ordre_transit` est une **clé étrangère**. PostgreSQL vérifie qu'elle désigne un ordre de transit existant. Si on tente de supprimer l'ordre 10 alors que le dossier 25 pointe toujours vers lui, la base peut refuser la suppression : elle ne veut pas laisser un lien vers une ligne inexistante. Le même ordre peut aussi être référencé par des contrats, des instructions, des marchandises, des expéditions, etc.

La difficulté est donc de traiter **toutes les lignes qui pointent vers l'élément à supprimer**, y compris les liens indirects, avant de supprimer cet élément. Il faut décider quoi faire de chaque ligne liée : la conserver sans ce lien, supprimer seulement son association, ou la supprimer elle aussi.

> **Attention :** « délier » ne signifie pas toujours « supprimer l'autre ligne ». Ce choix dépend du rôle de cette ligne dans l'application.

## Les trois actions possibles

| Situation | Action | Exemple dans ce projet |
| --- | --- | --- |
| La ligne liée doit rester et sa clé étrangère peut être vide | Mettre la clé étrangère à `NULL` | Supprimer un navire : `expedition.id_navire` devient `NULL`, mais l'expédition reste. |
| La ligne est seulement une association entre deux objets | Supprimer la ligne d'association | Supprimer un dossier : retirer ses lignes dans `dossier_conteneur`, mais conserver les conteneurs associés à d'autres dossiers. |
| La ligne dépend de l'objet supprimé, ou un lien obligatoire ne peut pas être vidé | Supprimer cette ligne, après avoir traité ses propres dépendances | Supprimer un dossier : supprimer ses documents et leurs lignes `document_mvt`. |

Dans le code, une clé étrangère déclarée `NOT NULL` est **obligatoire** : on ne peut pas la remplacer par `NULL`. Pour les relations ordinaires, le service supprime alors la ligne dépendante. Il existe une exception volontaire : si `dossier.id_ordre_transit` devient obligatoire dans la base, la suppression de l'ordre de transit est refusée pour éviter de supprimer le dossier.

## Exemple 1 : supprimer un ordre de transit

Supposons que l'ordre de transit **10** soit lié au dossier **25**, à un contrat et à une marchandise.

Avant :

```text
ordre_transit 10
├── dossier 25 (id_ordre_transit = 10)
├── contrat_transport 7 (id_ordre_transit = 10)
└── marchandise 3 (id_ordre_transit = 10)
```

Le service prépare alors ce résultat :

1. **Conserver le dossier 25** : mettre `dossier.id_ordre_transit` à `NULL` et changer son statut en « Clôturé ». Ses documents et mouvements restent liés à ce dossier.
2. **Supprimer le contrat et la marchandise** liés à l'ordre 10. Le service traite aussi leurs éventuelles dépendances avant de les supprimer. Il traite de la même façon les instructions et les expéditions liées à l'ordre.
3. **Supprimer l'ordre de transit 10** en dernier.

Après :

```text
ordre_transit 10 : supprimé
dossier 25       : conservé, id_ordre_transit = NULL, statut = Clôturé
contrat 7        : supprimé
marchandise 3    : supprimée
```

Le dossier reste donc consultable. Le fait de supprimer son ordre de transit ne signifie pas que le dossier lui-même disparaît.

## Exemple 2 : supprimer un dossier partagé avec un conteneur

Un conteneur peut être affecté à plusieurs dossiers au moyen de `dossier_conteneur` :

```text
dossier 25 ── dossier_conteneur ──┐
                                  ├── conteneur 8
dossier 30 ── dossier_conteneur ──┘
```

Si on supprime le dossier 25, le service supprime **son association** avec le conteneur 8, ainsi que les données qui appartiennent au dossier 25 (par exemple ses documents et ses mouvements). Le conteneur 8 et son association avec le dossier 30 restent. Une ancienne colonne directe `conteneur.id_dossier`, si elle pointe vers le dossier supprimé, est mise à `NULL`.

Si l'utilisateur retire seulement l'affectation du conteneur 8 au dossier 25, le service conserve le conteneur tant qu'il reste affecté à un autre dossier. Si c'était sa dernière affectation, cette opération supprime aussi le conteneur.

## Comment le programme procède réellement

Le mécanisme central est dans `backend/src/main/java/com/auximad/service/SuppressionService.java`.

1. **Recevoir l'ID à supprimer.** Un contrôleur appelle `supprimer("dossier", id)`, `supprimer("ordre_transit", id)`, etc. La suppression par lot passe par `supprimerLot`.
2. **Lire les relations de PostgreSQL.** Le service consulte les contraintes de clés étrangères du schéma courant. Il découvre les tables et colonnes qui référencent la ligne choisie ; la liste n'est pas codée table par table dans la méthode de suppression.
3. **Construire un plan.** Pour chaque ligne trouvée, il choisit entre `UPDATE ... SET colonne = NULL` et `DELETE`. S'il faut supprimer une ligne enfant, il examine d'abord les lignes qui dépendent de cet enfant. Il évite de traiter deux fois la même ligne.
4. **Présenter l'impact avant toute modification.** La première requête renvoie une réponse HTTP `409` avec la liste des éléments à supprimer, des liens à retirer et des dossiers à clôturer. Aucun changement n'est encore enregistré.
5. **Confirmer.** Le navigateur affiche cette liste. Si l'utilisateur confirme, il renvoie la requête avec l'en-tête `X-Confirmation-Suppression`. Le serveur recalcule le plan et vérifie que la confirmation correspond encore à cet impact. Si les données liées ont changé, il demande une nouvelle confirmation.
6. **Exécuter dans une transaction.** Le service retire d'abord les liens à conserver (`UPDATE`), puis supprime les lignes dépendantes avant la ligne principale (`DELETE`). Si une contrainte ou une erreur empêche la fin, la transaction annule l'ensemble des modifications.

Le code de la fenêtre de confirmation est dans `frontend/src/services/SuppressionService.js`. La réponse `409` est produite par `backend/src/main/java/com/auximad/controller/SuppressionExceptionHandler.java`.

## Pourquoi ne pas faire simplement `DELETE FROM dossier WHERE id = 25` ?

Parce que `document`, `mvt_dossier`, `dossier_conteneur` et d'autres tables peuvent encore référencer ce dossier. Une seule requête `DELETE` ne décide pas quelles données doivent survivre. Certaines contraintes SQL ont `ON DELETE CASCADE`, mais ce n'est pas une règle générale de la base, et une cascade supprimerait les lignes sans distinguer les objets partagés des données propres au dossier. Le service applique donc explicitement les règles de l'application.

## À retenir

**Supprimer une entité = repérer ce qui la référence, décider du sort de chaque lien et de chaque ligne dépendante, puis supprimer l'entité seulement quand ces relations ont été traitées.** Dans ce projet, cette opération est planifiée, montrée à l'utilisateur, confirmée et exécutée en une seule transaction.

Le service sait parcourir les clés étrangères simples du schéma. S'il rencontre une contrainte complexe qu'il ne sait pas traiter, une dépendance cyclique, ou un lien obligatoire entre un ordre de transit et un dossier à conserver, il arrête la suppression au lieu de modifier les données partiellement.
