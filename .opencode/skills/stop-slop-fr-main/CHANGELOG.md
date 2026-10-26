# Changelog

Format : [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/).
Versionning : [SemVer](https://semver.org/lang/fr/).

## [0.2.0] - 2026-05-06

### Ajouté
- Section "Niveaux de preuve" dans `SKILL.md` (robuste / plausible / anecdotique)
- Section "Anglicismes et calques" dans `phrases.md`
- Section "Métriques mesurables" dans `structures.md`
- Nouveau fichier `references/markers-en.md` : mots anglais excédentaires sourcés (Juzek 2025, Reinhart 2025, Kobak 2024)
- Nouveau fichier `references/metrics.md` : préparation du script de validation Python
- Section "Lacunes assumées" et "Limites du skill" dans le README

### Modifié
- Reformulation de la mission du skill : "marqueurs de surface à réviser", pas "détection d'IA"
- Refonte de la section "Affirmations sans agent identifié" (ex-"Voix passive sans agent") dans `structures.md`
- Tagging par niveau de preuve dans toutes les sections de `phrases.md`
- Réécriture de plusieurs paires "après" dans `examples.md` pour éliminer les tirets cadratins parenthétiques et les triades explicites qui contredisaient les règles du skill

### Corrigé
- **Erreur factuelle** : la règle "voix passive = marqueur IA" est supprimée. Reinhart et al. (2025, CMU) mesurent que GPT-4o utilise la voix passive ×0,5 par rapport aux humains. Le vrai marqueur syntaxique LLM est la nominalisation (×2,1) et la clause participiale présente (×5,3). Le skill cible désormais ces deux marqueurs.
- Suppression du plan numéroté ironique dans `structures.md` (la section "Une introduction efficace pose : 1... 2... 3..." reproduisait le patron annoncé que le skill interdit).
- Suppression de tous les tirets cadratins parenthétiques des versions "après" de `examples.md`.

### Sources de calibration
- Juzek & Ward (2025, COLING)
- Reinhart et al. (2025, CMU/PNAS preprint, arXiv:2410.16107)
- Chakrabarty et al. (2024, Salesforce, arXiv:2409.14509) ; taxonomie LAMP
- Kobak et al. (2024, Science Advances)
- Liang et al. (2025, Nature Human Behaviour)
- GPTZero (Tian) ; perplexité + burstiness

## [0.1.0] - 2026-05-05

### Ajouté
- Skill initial `stop-slop-fr` adapté à l'écriture académique française
- Couverture des rendus : rapport de stage, mémoire, TFE, dissertation, projet tuteuré, alternance
- Liste de ~80 expressions bannies dans `references/phrases.md`
- Patrons structurels mécaniques dans `references/structures.md`
- 12 paires avant/après dans `references/examples.md`
- Système de scoring sur 50 (Précision / Rythme / Registre / Authenticité / Densité)
- Adaptation contextuelle selon le type de rendu (usage du je/nous/impersonnel)
