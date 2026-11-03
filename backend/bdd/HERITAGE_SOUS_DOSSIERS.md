# Ressources des sous-dossiers

Lors de la creation d'un sous-dossier interagence, directement ou apres
acceptation d'une demande, le serveur reprend les ressources disponibles
sur le dossier mere dans la meme transaction :

- Les memes conteneurs sont associes au sous-dossier, sans retirer les liens
  du dossier mere ou des autres agences. Un sous-dossier avec conteneurs doit
  conserver un transport maritime.
- Les documents actifs sont copies avec leur type, nombre, nom de fichier,
  chemin et date d'ajout. Les documents retires ne sont pas repris.
- Les remises conservent leurs dates. Les fichiers existants restent accessibles.
- Le fonctionnement habituel des documents obligatoires est recalcule apres
  la copie pour le nouveau dossier.

Il s'agit d'une reprise au moment de la creation. Les ajouts et retraits
effectues ensuite sur un dossier ne sont pas propages automatiquement.

Pour completer les sous-dossiers deja existants, la migration
`migrations/20261103_heritage_sous_dossiers.sql` ajoute les ressources manquantes.
Elle conserve les documents et dates propres au sous-dossier, fonctionne sur
plusieurs niveaux et peut etre rejouee sans dupliquer les memes ressources.

La creation manuelle d'un dossier peut aussi transmettre `conteneurCreation`
(numero, type et description) : le dossier et l'assignation sont enregistres
ensemble. Aucun changement de structure de la base n'est necessaire.

Verification : tests unitaires, tests JPA dans un schema PostgreSQL temporaire,
annulation de transaction apres ecriture et test de migration rejouee deux fois.
