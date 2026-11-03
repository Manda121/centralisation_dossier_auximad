# Demarrer les tests sans dossiers

`donnees_initiales.sql` charge les memes referentiels que `donne_deepseek.sql` :
agences, roles, regimes, phases, statuts, operations, documents obligatoires,
transports, bureaux et autres listes de configuration. Les 30 comptes `user1`
a `user30`, leurs mots de passe chiffres et leurs autorisations sont conserves
identiques au jeu de demonstration. Ils disposent tous du niveau 10000 pour les tests.

Aucun client, OT, dossier, mouvement, marchandise, conteneur, document remis,
declaration, demande interagence ou historique n'est cree.

Dans psql, connecte a la base de test avec les tables deja creees :

```sql
\set ON_ERROR_STOP on
-- Seulement si la base contient deja des donnees : efface toutes les lignes.
\i 'D:/Stage Auximad/centralisation_app/backend/bdd/reinitialiser_donnees.sql'
\i 'D:/Stage Auximad/centralisation_app/backend/bdd/donnees_initiales.sql'
```

La reinitialisation conserve les tables mais efface aussi les comptes et les
adresses email d'agence existants. Le second script recree les comptes du jeu
de demonstration ; les destinataires email seront a configurer dans l'application.
La configuration SMTP privee dans les fichiers du serveur reste en place.

Sur des tables deja vides, executer uniquement `donnees_initiales.sql`.
Ne pas charger egalement `donne_deepseek.sql` : ce sont deux jeux alternatifs.
Ces scripts n'ont pas ete appliques a votre base actuelle.
