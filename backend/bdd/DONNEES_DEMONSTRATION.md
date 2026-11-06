# Répartition des données de démonstration

`donne_deepseek.sql` crée 300 dossiers et 320 ordres de transit. Les trois phases, les opérations import/export, les 17 statuts retenus et FOB sont conservés.

## Dates

Les créations occupent 300 créneaux répartis sur la période comprise entre le début du mois d'il y a onze mois et la date d'exécution du script. Chaque créneau contient une date légèrement variable. Le mois courant reçoit donc une part proportionnelle au nombre de jours déjà écoulés, au lieu d'un quota mensuel entier concentré sur les premiers jours. Le dernier dossier est maritime et daté du jour pour illustrer les parcours d'assignation, avec une permutation de deux productions qui conserve leurs volumes. Les dates des mouvements, documents et expéditions restent liées à celles des dossiers et des ordres de transit.

## Agences

Les populations urbaines proviennent du [RGPH-3 INSTAT 2018, tome 1, tableau 4, pages 13 à 15](https://instat.mg/documents/upload/main/INSTAT_RGPH3-Definitif-ResultatsGlogaux-Tome1_17-2021.pdf#page=45). Les populations sont celles du recensement de 2018. Elles servent à pondérer le jeu de démonstration, sans représenter des volumes commerciaux observés.

| Ville | Population urbaine 2018 | Dossiers |
| --- | ---: | ---: |
| Antananarivo | 1 274 225 | 152 |
| Toamasina | 325 857 | 39 |
| Antsirabe | 246 354 | 30 |
| Mahajanga | 246 022 | 29 |
| Toliara | 168 756 | 20 |
| Antsiranana | 129 320 | 16 |
| Fort-Dauphin | 67 284 | 8 |
| Nosy Be | 50 251 | 6 |

La méthode des plus forts restes arrondit les proportions à exactement 300 dossiers. Antananarivo est comptée une seule fois. Son quota est partagé entre **TNR : 137 dossiers** et **DIR : 15 dossiers** ; la clé interne 90 % / 10 % est une hypothèse de démonstration. Les affectations sont mélangées de façon reproductible pour varier les agences au fil du temps et des types de production. Les sous-dossiers gardent ces affectations.

Les 20 ordres de transit supplémentaires sont tirés selon la même pondération. Les comptes utilisateurs conservent au moins un représentant dans chacune des neuf agences.

## Cohérence logistique et interagences

Les conteneurs sont affectés uniquement aux dossiers maritimes. Les expéditions maritimes utilisent un navire de leur compagnie et des ports ; les expéditions aériennes utilisent un vol, une LTA et des aéroports, sans navire, BL ni port. Le départ est à l'étranger et l'arrivée à Madagascar pour l'import ; le sens est inversé pour l'export. Les pays et lieux des dossiers suivent ces stations. Les régimes import/export et les dates de départ, prévision et arrivée sont cohérents avec le sens et l'OT. Les marchandises et expéditions sont affectées aux OT validés. Chaque groupage utilise l'expédition de l'OT de son dossier.

Le jeu comprend **80 demandes interagences** : 40 acceptées correspondant aux 40 sous-dossiers, 20 en attente et 20 refusées. Les demandeurs et répondants appartiennent aux agences concernées ; les réponses suivent les demandes, et l'acceptation correspond à la date de création du sous-dossier. Aucun email n'est expédié ou ajouté à l'historique par le chargement des données.

Un même dossier mère peut posséder des sous-dossiers dans plusieurs agences. Le jeu illustre ces prises en charge multiples avec une seule association par agence et par dossier mère. Une clôture intervient après les acceptations interagences du dossier concerné. Le dernier dossier est maritime et créé le jour du chargement pour permettre de vérifier les écrans d'assignation du jour.

## Chargement

Les régimes sont `IM4 / 4000 / definitif`, `IM5 / 5000 / economique`, `IM7 / 7000 / economique`, `IM8 / 8900 / transitoire`, `IM8 / 8000 / ordinaire`, `EX1 / 1000 / definitif` et `EX2 / 2000 / temporaire`. Le code est unique ; le nom peut se répéter. Les OT import utilisent un régime IM, les OT export un régime EX, et les autres productions le régime transitoire IM8 8900. Les déclarations de démonstration reprennent la famille de régime de leur OT.

Pour mettre à jour uniquement les régimes d'une base existante, appliquer `migrations/20261103_regimes_douaniers.sql`. Cette migration conserve les identifiants des anciens régimes quand c'est possible et préserve les relations avec les OT. Elle peut être rejouée.

Sur une base déjà remplie, `reinitialiser_donnees.sql` vide toutes les données en conservant les tables. Le chargement de `donne_deepseek.sql` recrée ensuite le jeu de démonstration. La vérification `tests/verification_donne_deepseek.sql` est réservée à un schéma de test isolé.
