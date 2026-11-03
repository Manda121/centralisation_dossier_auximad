# Emails des demandes interagences

Les notifications utilisent Jakarta Mail (JavaMail) via `spring-boot-starter-mail` et `JavaMailSender`.

## Installation

Sur la base existante, appliquer uniquement la migration :

```powershell
psql -U postgres -d dossier_auximad -v ON_ERROR_STOP=1 -f bdd/migrations/20261102_emails_interagences.sql
```

Le fichier `bdd/base_centralisation.sql` contient également les nouvelles tables pour une installation neuve. Ce fichier recrée la base : ne pas l'exécuter sur une base à conserver.

## Serveur SMTP

Définir les variables d'environnement du processus Java, puis redémarrer le backend :

| Variable | Description | Valeur par défaut |
| --- | --- | --- |
| `MAIL_HOST` | Hôte SMTP | vide |
| `MAIL_PORT` | Port SMTP | `587` |
| `MAIL_USERNAME` | Identifiant SMTP | vide |
| `MAIL_PASSWORD` | Mot de passe SMTP | vide |
| `MAIL_FROM` | Adresse expéditrice autorisée par le fournisseur | vide |
| `MAIL_SMTP_AUTH` | Authentification SMTP | `true` |
| `MAIL_STARTTLS` | Activer et exiger STARTTLS | `true` |
| `MAIL_SSL` | TLS implicite (habituellement port 465) | `false` |

Pour un serveur sur le port 465, utiliser `MAIL_SSL=true` et `MAIL_STARTTLS=false`. Pour un serveur de test local sans authentification ni TLS, utiliser `MAIL_SMTP_AUTH=false` et `MAIL_STARTTLS=false`. Les délais de connexion, lecture et écriture sont limités à 5 secondes chacun. Les secrets SMTP restent dans l'environnement du serveur et ne sont jamais renvoyés à l'interface.

## Utilisation

Dans **Administration → Structure organisationnelle → Adresses emails**, ajouter les destinataires de chaque agence. Plusieurs adresses peuvent être actives pour la même agence. Une adresse peut être modifiée ou désactivée.

À la création d'une demande, une ligne `historique_mail` est enregistrée pour chaque destinataire actif dans la même transaction que la demande. Un traitement périodique expédie les lignes en attente, par lots de 10, environ toutes les 10 secondes. L'absence de SMTP ou de destinataire est enregistrée avec le statut `NON_CONFIGURE` et ne bloque pas la création de la demande. Aucun email n'est envoyé pour une demande dont la transaction a été annulée. Les demandes déjà présentes avant l'installation ne déclenchent pas d'envoi rétroactif.

L'historique affiche le dossier, l'agence, le destinataire, l'expéditeur, le sujet, le corps, les dates et le résultat. Il est paginé et filtrable par agence et statut. Après correction de la configuration, **Relancer** crée une nouvelle tentative et conserve la précédente. Une tentative ne peut être relancée qu'une fois ; en cas de nouvel échec, relancer sa dernière tentative. Si aucune adresse n'était configurée, la relance utilise les adresses actives de l'agence au moment de la relance ; sinon elle conserve l'adresse de la tentative originale.

Une modification du motif ou de l'agence destinataire prépare de nouveaux emails et annule les anciens emails encore en attente. Au moment de l'envoi, une demande déjà traitée ou réaffectée à une autre agence entraîne le statut `ANNULE` pour éviter une notification devenue inutile.

`ENVOYE` signifie que le serveur SMTP a accepté le message. Cela ne confirme pas sa réception ni sa lecture. Après une coupure entre l'acceptation SMTP et la validation de la transaction en base, le traitement peut envoyer un doublon au redémarrage : SMTP ne garantit pas une livraison exactement une fois.

Les endpoints `/api/admin/emails/**` exigent le niveau administrateur 10000. Les tables `adresse_email_agence` et `historique_mail` conservent respectivement la configuration et les envois.
