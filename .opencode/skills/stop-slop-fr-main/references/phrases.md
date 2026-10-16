# Phrases à bannir

Liste des expressions qui signalent un texte produit ou retouché par IA. Chaque entrée comprend la phrase à couper, la raison, et un remplacement quand il existe.

> Chaque section indique son niveau de preuve : **[Robuste]**, **[Plausible]**, ou **[Anecdotique]**. Voir `SKILL.md` § "Niveaux de preuve" pour le détail.

## Connecteurs en surdose [Plausible]

Les modèles de langage déposent un connecteur logique au début d'un paragraphe sur deux. Un correcteur humain le repère immédiatement.

| À bannir | Pourquoi | Remplacement |
|---|---|---|
| « Il est important de noter que... » | Ouverture vide qui ajoute du bruit | Couper la phrase d'amorce, garder l'information |
| « Il convient de souligner que... » | Tic académique pompeux | Aller directement au fait |
| « Force est de constater que... » | Cliché de dissertation IA | « Les données montrent que... » ou source directe |
| « Par ailleurs » (en surdose) | Connecteur passe-partout, autorisé une fois par section maximum | Reformuler la transition logique |
| « En effet » (en ouverture systématique) | Justification automatique sans valeur logique | Couper si la phrase suivante se justifie d'elle-même |
| « Ainsi » (récurrent) | Faux lien causal, souvent sans causalité réelle | « Donc », « par conséquent » si le lien existe vraiment |
| « De plus » | Empilement paresseux | Fusionner les deux idées dans une seule phrase |
| « En outre » | Doublon stylé de « de plus » | Idem |
| « Dès lors » | Connecteur signature des LLM en français | « À partir de ce constat » si nécessaire |
| « Néanmoins » (en série) | Faux contrepoint quand il n'y a pas d'opposition réelle | Vérifier qu'il y a vraiment une concession à faire |
| « Toutefois » (en série) | Idem | Idem |
| « Cela étant dit » | Tic de transition vide | Couper |

## Adverbes creux [Anecdotique]

Les adverbes en `-ment` sont la signature des LLM en français. Ils gonflent la phrase sans rien ajouter.

| À bannir | Remplacement |
|---|---|
| « véritablement » | Couper, le verbe se suffit |
| « particulièrement » | Couper ou remplacer par un chiffre concret |
| « notamment » (en abus, plus de 2 fois par page) | Citer directement les exemples |
| « fondamentalement » | Couper |
| « indéniablement » | Une affirmation indéniable n'a pas besoin d'être qualifiée comme telle |
| « assurément » | Couper |
| « manifestement » | Couper si l'évidence est dans les faits cités |
| « profondément » | Couper ou reformuler |
| « pleinement » | Couper |
| « significativement » (sans test statistique) | Couper ou citer le test |

## Formules creuses [Plausible]

Expressions toutes faites qui meublent sans rien dire.

| À bannir | Pourquoi |
|---|---|
| « joue un rôle clé » | Tic IA, et « clé » est galvaudé |
| « représente un enjeu majeur » | Cliché vide, à remplacer par l'enjeu précis |
| « s'inscrit dans une dynamique » | Métaphore creuse |
| « à l'ère du numérique » | Datée et vague |
| « dans un monde en constante évolution » | Cliché absolu, signature LLM |
| « dans un contexte en mutation » | Variante du précédent |
| « se révèle être » | « est » suffit |
| « constitue un défi » | Préciser le défi ou couper |
| « représente une opportunité » | Préciser laquelle |
| « met en lumière » (en abus) | « montre », « révèle » |
| « au cœur de » (en abus) | Couper la métaphore |
| « plus que jamais » | Cliché temporel sans appui |
| « véritable révolution » | Cliché médiatique |
| « paradigme » (hors contexte épistémologique précis) | Mot-piège, souvent mal employé |
| « écosystème » (hors biologie ou économie numérique précise) | Métaphore galvaudée |

## Ouvertures-clichés [Plausible]

À proscrire dans les introductions, partout en fait.

| À bannir |
|---|
| « De nos jours » |
| « Aujourd'hui plus que jamais » |
| « Force est de constater que » |
| « Il est devenu courant de » |
| « À l'heure actuelle » |
| « Dans la société moderne » |
| « Depuis quelques années » (sans dater précisément) |

Une introduction commence par le problème, pas par un panorama.

## Méta-commentaires [Plausible]

Phrases qui parlent du texte au lieu de le faire avancer. À couper systématiquement.

| À bannir |
|---|
| « Comme nous allons le voir » |
| « Cette partie sera consacrée à » |
| « Nous verrons dans un premier temps » |
| « Il s'agira de » |
| « L'objectif de cette section est de » |
| « Avant de poursuivre, il convient de » |
| « Comme évoqué précédemment » |
| « Nous allons maintenant aborder » |
| « Pour reprendre ce qui a été dit » |

L'unique méta-commentaire toléré est la formulation de la problématique en fin d'introduction.

## Faux balancements [Plausible]

Quand le contenu ne justifie pas la structure binaire.

| À bannir | Quand c'est faux |
|---|---|
| « Non seulement... mais aussi » | Si les deux éléments ne sont pas vraiment complémentaires |
| « D'une part... d'autre part » | Si le texte ne traite pas deux aspects distincts |
| « Tantôt... tantôt » | Si l'alternance n'existe pas réellement |
| « À la fois... et... » (en cascade) | Si l'on cherche juste à empiler |

Vérifier que la structure binaire correspond à une vraie opposition ou complémentarité dans les faits.

## Affirmations sans source [Robuste, retour enseignants]

Bannir absolument :

| À bannir | Remplacement |
|---|---|
| « selon certains auteurs » | Citer l'auteur ou supprimer |
| « de nombreuses études montrent » | Citer une étude précise ou supprimer |
| « il est démontré que » | Citer la démonstration ou supprimer |
| « la littérature scientifique souligne » | Citation précise ou supprimer |
| « les experts s'accordent à dire » | Nommer les experts ou supprimer |
| « il est communément admis que » | Citer la source du consensus ou supprimer |
| « la recherche montre que » (sans référence) | Citer ou supprimer |
| « des chercheurs ont mis en évidence » | Lesquels, où, quand |

## Quantifieurs flous [Robuste, retour enseignants]

À remplacer par des chiffres ou à supprimer.

| À bannir | Remplacement |
|---|---|
| « une part importante » | « 42 % » (avec source) |
| « la majorité » (sans chiffre) | Pourcentage exact |
| « la plupart » (sans chiffre) | Idem |
| « beaucoup de » | Idem |
| « un certain nombre de » | Citer le nombre |
| « divers » (en flou) | Lister ou compter |
| « plusieurs » (en flou) | Idem |

## Verbes faibles [Plausible]

Verbes passe-partout qui diluent la phrase.

| À bannir | Remplacement |
|---|---|
| « mettre en place » | « instaurer », « déployer », « créer » selon le sens |
| « faire en sorte que » | « pour que » + subjonctif |
| « avoir pour but de » | « vise à » |
| « se traduit par » (en abus) | Verbe spécifique |
| « être en mesure de » | « pouvoir » |
| « être à même de » | « pouvoir » |
| « avoir tendance à » | Décrire l'action directement |

## Pléonasmes IA fréquents [Plausible]

Doublons que les modèles produisent par habitude statistique.

| À bannir | Pourquoi |
|---|---|
| « collaborer ensemble » | Collaborer suppose ensemble |
| « monter en haut » | Pléonasme |
| « prévoir à l'avance » | Pléonasme |
| « s'avérer être » | « être » suffit |
| « se révéler être » | Idem |
| « apparaître comme étant » | « apparaître » suffit |

## Tournures de remplissage [Plausible]

Phrases qui remplissent l'espace sans porter d'information.

- « Cela étant, il est intéressant de remarquer que »
- « Sans entrer dans les détails »
- « Pour faire simple »
- « En d'autres termes » (si la phrase précédente était claire)
- « Il va sans dire que » (alors si ça va sans dire, ne le dire pas)
- « Comme chacun sait » (parler pour le lecteur)

## Anglicismes et calques [Plausible]

Les LLM transposent des structures anglaises sans adaptation française. Ces calques sonnent faux pour un correcteur francophone.

| À bannir | Origine | Remplacement |
|---|---|---|
| « adresser un problème » | *to address an issue* | « traiter », « aborder », « résoudre » |
| « supporter une fonctionnalité » | *to support a feature* | « prendre en charge », « gérer » |
| « impacter » | *to impact* | « affecter », « influencer », « peser sur » |
| « basé sur » (en abus) | *based on* | « fondé sur », « à partir de », « selon » |
| « versus » | *vs.* | « par rapport à », « contre », « face à » |
| « en termes de » (en abus) | *in terms of* | Reformuler avec un substantif |
| « au final » | *in the end* | « finalement », « au bout du compte » |
| « initier un projet » | *to initiate* | « lancer », « démarrer » |
| « réaliser que » | *to realize* | « se rendre compte que », « comprendre » |
| « définitivement » (= certainement) | *definitely* | « certainement », « sans doute » |
| « éventuellement » (= finalement) | *eventually* (faux ami) | « finalement », « à terme » |
| « adresser une question » (au sens traiter) | *to address* | « traiter », « répondre à » |
