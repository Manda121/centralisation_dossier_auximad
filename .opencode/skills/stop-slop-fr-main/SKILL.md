---
name: stop-slop-fr
description: Élimine les marqueurs IA dans les rendus académiques français — rapports de stage, mémoires (licence, master, ingénieur), dissertations, TFE, mémoires de recherche, projets tuteurés, rapports d'alternance, soutenances écrites, état de l'art, rédaction académique française. Active ce skill avant de rédiger ou de réviser tout texte universitaire francophone destiné à un jury, un tuteur ou un correcteur.
---

# stop-slop-fr

Skill de relecture pour rendus écrits étudiants en français. Cible les marqueurs de surface qui font perdre des points à la relecture humaine : phrases creuses, connecteurs en surdose, généralités sans source, plans annoncés à la chaîne.

**Ce que ce skill fait** : il améliore la voix d'un texte que l'auteur a réellement écrit, pour qu'elle ne déclenche pas les signaux que les correcteurs humains et les détecteurs automatiques associent à un texte généré.

**Ce que ce skill ne fait pas** : il ne contourne pas les détecteurs commerciaux (GPTZero, Compilatio, Turnitin). Aucun outil ne garantit l'invisibilité aux détecteurs en 2026. Krishna et al. (2023, NeurIPS) montrent même que les détecteurs mesurent la fluidité statistique, pas la paternité intellectuelle. Pour un rendu universitaire, l'usage honnête est de rédiger soi-même puis d'utiliser ce skill comme grille de relecture.

## Principe directeur

Un correcteur reconnaît un texte produit par IA en trois lignes. Les marqueurs ne sont pas dans la grammaire — ils sont dans le **rythme**, la **densité** et la **voix**. Ce skill traque ces marqueurs.

## Niveaux de preuve

Les marqueurs traités par ce skill ne se valent pas. Trois niveaux, à pondérer lors de la révision.

**Robuste** — études peer-reviewed avec mesures chiffrées, applicables aux sorties LLM en général :
- Mots anglais excédentaires (Juzek & Ward 2025, Kobak et al. 2024)
- Faible variance de longueur des phrases (burstiness GPTZero)
- Nominalisations ×2,1 et clauses participiales présentes ×5,3 par rapport au texte humain (Reinhart et al. 2025, CMU)

**Plausible** — signaux qualitatifs convergents, non quantifiés sur corpus français peer-reviewed :
- Connecteurs formels en surdose (par ailleurs, en outre, dès lors, ainsi)
- Formules métadiscursives stéréotypées (il convient de noter, il est important de souligner)
- Triades systématiques (3 items à chaque paragraphe)
- Conclusions formulaïques (en somme, en définitive, pour conclure)
- Plans annoncés mécaniquement en introduction
- Calques de l'anglais (mettre en lumière, basé sur, adresser un problème)

**Anecdotique** — signal perceptif fort pour un correcteur humain, preuve académique faible :
- Adverbes en `-ment` en cascade
- Tirets cadratins (—) en parenthétique stylé en français
- Guillemets droits `"..."` au lieu de chevrons `«...»`
- Apostrophes droites au lieu de courbes
- Ponctuation typographique parfaite et systématique (artefact de copier-coller LLM non post-traité)

Le skill traite les trois niveaux, mais formule ses retours en distinguant lequel s'applique. Un détecteur automatique repose sur les marqueurs robustes ; un correcteur humain s'arrête souvent sur des marqueurs anecdotiques.

**Lacune assumée** : aucune étude peer-reviewed n'établit à ce jour les marqueurs LLM spécifiques au français académique. Les listes françaises de ce skill sont des hypothèses convergentes issues de retours d'enseignants (Le Monde Campus, Le Figaro Étudiant, GPTZero FR) et de l'audit qualitatif de sorties Claude/GPT, pas des fréquences mesurées.

## Règles fondamentales

### 1. Registre académique français : pas de "tu" ni de "vous"

Le repo anglais `hardikpandya/stop-slop` recommande "use you". **Cette règle ne s'applique pas en académique français.**

Choix imposés :
- **Nous de modestie** : « nous observons », « nous analysons », « nous proposons »
- **Tournures impersonnelles** : « on observe », « il apparaît », « les données indiquent »
- **Sujet objet du discours** : « cette démarche montre », « le corpus révèle »

Le tutoiement est exclu. Le « vous » n'apparaît pas dans le corps du texte (uniquement en remerciements ou adresses formelles).

### 2. Couper les connecteurs surutilisés

Les modèles de langage saturent le texte de connecteurs vides. Règle simple : un connecteur logique tous les 3-4 paragraphes maximum, et seulement quand il porte une vraie articulation.

Voir [references/phrases.md](references/phrases.md) pour la liste des bannis.

### 3. Sources concrètes ou rien

Bannir :
- « selon certains auteurs »
- « de nombreuses études montrent »
- « la littérature scientifique souligne »

Imposer : `Auteur (Année)` ou pas de mention. Si la source manque, retirer l'affirmation.

### 4. Données chiffrées plutôt que qualitatifs vagues

Remplacer « une part importante des salariés » par « 42 % des salariés (enquête X, 2024) ». Si le chiffre n'est pas disponible, reformuler sans prétendre à la généralité.

### 5. Description ≠ analyse

Dans les rapports de stage et d'alternance, distinguer clairement :
- **Description** : ce qui a été fait, observé, livré (passé composé, factuel)
- **Analyse** : ce qui en est tiré, les apports, les limites (présent, argumenté)

Un paragraphe purement descriptif suivi d'un paragraphe purement analytique vaut mieux qu'une bouillie où les deux se mélangent.

### 6. Pas de méta-discours

Les phrases qui annoncent ce que le texte va faire au lieu de le faire :
- « Nous verrons dans un premier temps... »
- « Cette partie sera consacrée à... »
- « Il s'agira d'analyser... »

Une introduction professionnelle pose le problème et la démarche en 4-5 phrases denses, sans annoncer la table des matières.

## Adaptation par type de rendu

Le registre se module selon le contexte. Le skill applique des règles différentes :

### Rapport de stage / rapport d'alternance

- « je » autorisé en introduction et conclusion (récit du parcours)
- « nous » dans les analyses techniques et organisationnelles
- Passé composé pour les missions accomplies
- Ancrage obligatoire : nom de l'entreprise, équipe, outils, livrables précis
- Pas de généralités sur le secteur en ouverture — entrer directement dans le contexte du stage

### Mémoire de recherche / TFE / mémoire master

- Registre formel strict
- « nous » exclusif (jamais « je »)
- Citations rigoureuses (norme APA, Vancouver ou MLA selon discipline)
- État de l'art structuré par concept ou par école, pas chronologique par défaut
- Hypothèses formulées explicitement avant la méthode

### Dissertation (sciences humaines, philosophie, droit)

- Structure thèse / antithèse / synthèse possible mais non obligatoire
- Présent de vérité générale dominant
- Citations courtes intégrées dans la phrase, pas en blocs séparés
- Problématique formulée en une phrase interrogative en fin d'introduction

### Projet tuteuré / mémoire d'ingénieur

- Équilibre technique-analytique
- Données chiffrées obligatoires : KPI, métriques, résultats mesurés
- Schémas et tableaux référencés dans le texte
- Justification des choix techniques par contraintes (budget, délai, performance)

## Checklist de relecture

Avant de rendre le document, passer le texte au crible :

- [ ] Aucune occurrence des phrases listées dans `references/phrases.md`
- [ ] Aucune des structures listées dans `references/structures.md`
- [ ] Toutes les affirmations générales sont sourcées (Auteur, Année)
- [ ] Aucun « selon certains auteurs » ou équivalent
- [ ] Pas de plan annoncé en table des matières dans l'introduction
- [ ] Pas de phrase de méta-commentaire (« nous allons voir »)
- [ ] Pas de tirets cadratins (—) en parenthétique stylé
- [ ] Pas de listes à puces dans le corps rédigé (réserver aux annexes)
- [ ] Pas de triade systématique (3 items à chaque paragraphe)
- [ ] Pas de conclusion en « En somme » / « En définitive » / « Pour conclure »
- [ ] Pas de nominalisations en cascade ni de clauses participiales présentes empilées
- [ ] Données chiffrées présentes là où c'est possible
- [ ] Description et analyse séparées (rapport de stage)
- [ ] Vocabulaire spécifique à la discipline, pas de jargon générique

## Grille de scoring

Cinq dimensions, notées de 0 à 10 chacune. Total sur 50.

| Dimension | Question de contrôle | Note |
|---|---|---|
| Précision | Les affirmations reposent-elles sur des données concrètes ou sur des généralités ? | /10 |
| Rythme | Les phrases varient-elles en longueur et en construction (variance de longueur, dite *burstiness*), ou suivent-elles une cadence mécanique ? | /10 |
| Registre | Le ton est-il académique sans pompe ni jargon creux ? | /10 |
| Authenticité | Reconnaît-on une voix d'étudiant qui pense, ou une voix de LLM lissée ? | /10 |
| Densité | Combien de mots peut-on couper sans rien perdre ? | /10 |

**Seuil** : sous 35/50, le texte doit être révisé avant rendu.

## Workflow de révision

1. Lire le brouillon complet une première fois sans rien modifier
2. Repérer chaque phrase de [phrases.md](references/phrases.md) et la couper ou la reformuler
3. Repérer chaque structure de [structures.md](references/structures.md) et la casser
4. Vérifier que chaque généralité a sa source ou disparaît
5. Comparer paragraphe par paragraphe aux paires avant/après de [examples.md](references/examples.md)
6. Appliquer la grille de scoring
7. Si le score reste sous 35, reprendre du début

## Références

- [phrases.md](references/phrases.md) — expressions à bannir
- [structures.md](references/structures.md) — structures à éviter
- [examples.md](references/examples.md) — paires avant / après par type de rendu
