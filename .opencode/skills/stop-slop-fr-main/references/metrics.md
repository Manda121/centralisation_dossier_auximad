# Métriques de validation [pour script Python futur]

Ce fichier prépare le script `validate.py` qui calculera des indicateurs mesurables sur un texte d'entrée.

## Stack cible

- `spacy` + modèle `fr_core_news_lg`
- `transformers` + `camembert-base` pour la perplexité
- `pandas` pour l'export
- Pas de promesse de "détection IA" ; output : score de surface LLM-like par famille

## Sept familles de features

| Famille | Features | Source |
|---|---|---|
| Typographie | présence de `—`, `–`, guillemets droits, espaces typographiques mécaniques | retours enseignants FR, signal anecdotique |
| Lexique | densité connecteurs, formules métadiscursives, adverbes `-ment`, anglicismes | listes `phrases.md` |
| Syntaxe | écart-type longueur phrases, phrases courtes/longues, nominalisations, participiales présentes | Reinhart 2025, GPTZero |
| Structure | longueur paragraphes, paragraphes ouvrant par connecteur, conclusions formulaïques | qualitatif, à calibrer |
| Format | listes à puces, numérotation, titres en gras `**X :** ...` | retours enseignants FR |
| Sémantique | densité d'entités nommées, densité de chiffres, exemples génériques vs situés | inspiré de LAMP (Chakrabarty 2024) |
| Robustesse | erreurs LanguageTool, variation de registre, perplexité CamemBERT | GPTZero (perplexité), retours enseignants (fautes) |

## Output attendu

Le script ne dira jamais "ce texte est généré par IA". Il affichera :

```
Score de surface LLM-like par famille (0-100)
- Typographie : 65/100 (3 cadratins parenthétiques détectés, lignes 12, 47, 89)
- Lexique : 42/100 (densité connecteurs : 31/1000 mots ; cible : <25)
- Syntaxe : 58/100 (écart-type longueurs phrases : 6,2 mots ; cible : >8)
- ...

Suggestions de réécriture (top 5) :
1. Ligne 12 : remplacer le tiret cadratin par une virgule
2. Ligne 47 : casser la triade "D'abord... Ensuite... Enfin..."
...
```

## Calibration nécessaire

Tous les seuils sont des hypothèses à calibrer sur un corpus de 30-50 paragraphes étudiants français annotés (manuellement marqués comme "écrit humain" ou "sortie LLM brute"). Cette calibration n'est pas faite à la date de la v0.2.0.
