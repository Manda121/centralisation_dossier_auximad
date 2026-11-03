# Répartition des données de démonstration

`donne_deepseek.sql` crée 300 dossiers et 320 ordres de transit. Les trois phases, les opérations import/export, les 17 statuts retenus et FOB sont conservés.

## Dates

Les créations occupent 300 créneaux répartis sur la période comprise entre le début du mois d'il y a onze mois et la date d'exécution du script. Chaque créneau contient une date légèrement variable. Le mois courant reçoit donc une part proportionnelle au nombre de jours déjà écoulés, au lieu d'un quota mensuel entier concentré sur les premiers jours. Les dates des mouvements, documents et expéditions restent liées à celles des dossiers et des ordres de transit.

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

## Chargement

Sur une base déjà remplie, `reinitialiser_donnees.sql` vide toutes les données en conservant les tables. Le chargement de `donne_deepseek.sql` recrée ensuite le jeu de démonstration. La vérification `tests/verification_donne_deepseek.sql` est réservée à un schéma de test isolé.
