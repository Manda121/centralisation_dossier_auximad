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
- L'avancement du dossier mere est repris lors de la creation du sous-dossier.

Les documents et conteneurs sont repris au moment de la creation. Leurs ajouts
et retraits ulterieurs ne sont pas propages automatiquement.

Les mouvements, phases et statuts partagent en revanche un avancement commun :
une creation, une annulation ou un changement manuel depuis n'importe quel
dossier est propage a la mere, aux sous-dossiers et aux autres agences de la
meme famille. Les dates, auteurs, commentaires et pieces jointes des mouvements
sont conserves. L'historique annule reste disponible. Les dossiers simplement
lies au meme OT sans lien interagence restent independants.

Les membres d'une famille doivent conserver le meme type de production pour
disposer des memes operations. Le verrou sur la famille serialise les actions
simultanees ; les modifications sont enregistrees dans la meme transaction.

Avant de demarrer le backend mis a jour sur une base existante, executer
`migrations/20261103_avancement_interagence.sql` pour ajouter la cle de partage
des mouvements et son index. Le schema complet inclut deja ces definitions.
Au demarrage, les familles deja creees sont alignees depuis le dossier mere
avec `SynchronisationMouvementsService.alignerDepuisMere`. La reprise est
idempotente et preserve les historiques annules.

Pour completer les sous-dossiers deja existants, la migration
`migrations/20261103_heritage_sous_dossiers.sql` ajoute les ressources manquantes.
Elle conserve les documents et dates propres au sous-dossier, fonctionne sur
plusieurs niveaux et peut etre rejouee sans dupliquer les memes ressources.

La creation manuelle d'un dossier peut aussi transmettre `conteneurCreation`
(numero, type et description) : le dossier et l'assignation sont enregistres
ensemble.

Verification : tests unitaires, tests JPA dans un schema PostgreSQL temporaire,
annulation de transaction apres ecriture et test de migration rejouee deux fois.
