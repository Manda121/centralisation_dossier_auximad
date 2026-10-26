# stop-slop-fr

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Skill: Claude](https://img.shields.io/badge/Claude-Skill-7C3AED)](https://docs.claude.com)
[![Version](https://img.shields.io/badge/version-0.2.0-blue)](./CHANGELOG.md)

Skill de relecture pour étudiants français : élimine les marqueurs IA dans les rendus écrits académiques.

Inspiré de [hardikpandya/stop-slop](https://github.com/hardikpandya/stop-slop), adapté au registre académique francophone.

## À qui ça sert

Aux étudiants qui rédigent ou relisent :

- Rapports de stage et rapports d'alternance
- Mémoires (licence, master, ingénieur)
- TFE et mémoires de recherche
- Dissertations
- Projets tuteurés
- Soutenances écrites
- États de l'art

Le skill détecte et corrige les tics de Claude et de ChatGPT en français académique : connecteurs en surdose, formules creuses, plans annoncés, généralités sans source, conclusions formulaïques.

## Ce qu'il fait concrètement

1. Impose le **nous de modestie** ou les tournures impersonnelles (le « tu » et le « vous » du repo anglais ne s'appliquent pas en académique français).
2. Adapte le ton selon le type de rendu (rapport de stage, mémoire de recherche, dissertation, projet tuteuré).
3. Bannit une centaine de phrases-types repérées comme signature LLM.
4. Casse les structures mécaniques : triades systématiques, phrases-tunnels, plans-types annoncés, listes à puces dans le corps rédigé.
5. Exige des sources concrètes (`Auteur, Année`) à la place des « selon certains auteurs ».
6. Applique une grille de scoring sur 50 (seuil de révision : 35).

## Structure

```
stop-slop-fr/
├── SKILL.md              ← règles principales + checklist + scoring
├── references/
│   ├── phrases.md        ← expressions à bannir
│   ├── structures.md     ← structures à éviter
│   ├── examples.md       ← 12 paires avant / après
│   ├── markers-en.md     ← mots anglais excédentaires sourcés
│   └── metrics.md        ← métriques mesurables (script Python futur)
├── CHANGELOG.md          ← historique des versions
├── README.md             ← ce fichier
└── LICENSE               ← MIT
```

## Installation

### Claude Code (CLI)

Pour activer le skill globalement (tous projets) :

```bash
mkdir -p ~/.claude/skills
cp -r stop-slop-fr ~/.claude/skills/
```

Pour l'activer sur un projet précis :

```bash
mkdir -p .claude/skills
cp -r stop-slop-fr .claude/skills/
```

Le skill s'active automatiquement lorsque la conversation porte sur un rapport de stage, un mémoire, une dissertation ou tout autre rendu listé dans le frontmatter de `SKILL.md`.

Activation manuelle :

```
/skill stop-slop-fr
```

### Claude Projects (claude.ai)

1. Créer un nouveau Project ou ouvrir un Project existant
2. Aller dans **Project knowledge** ou **Custom instructions**
3. Coller le contenu de `SKILL.md`, puis ajouter en fin de prompt :
   ```
   Référer aussi aux fichiers references/phrases.md, references/structures.md
   et references/examples.md (joints en pièces jointes du Project).
   ```
4. Téléverser les trois fichiers `references/*.md` dans le knowledge du Project
5. Démarrer une conversation : Claude appliquera les règles dès qu'on lui soumet un texte académique français à relire

### API Anthropic

Pour utiliser le skill via l'API, charger `SKILL.md` comme partie du `system` prompt et fournir les fichiers de référence dans le contexte :

```python
from anthropic import Anthropic
from pathlib import Path

client = Anthropic()

skill_dir = Path("stop-slop-fr")
system_prompt = (skill_dir / "SKILL.md").read_text(encoding="utf-8")
phrases = (skill_dir / "references" / "phrases.md").read_text(encoding="utf-8")
structures = (skill_dir / "references" / "structures.md").read_text(encoding="utf-8")
examples = (skill_dir / "references" / "examples.md").read_text(encoding="utf-8")

system_with_refs = f"""{system_prompt}

# Référence : phrases.md
{phrases}

# Référence : structures.md
{structures}

# Référence : examples.md
{examples}
"""

texte_etudiant = Path("mon_rapport.md").read_text(encoding="utf-8")

response = client.messages.create(
    model="claude-opus-4-7",
    max_tokens=4096,
    system=system_with_refs,
    messages=[
        {
            "role": "user",
            "content": (
                "Relis ce texte de rapport de stage, applique le skill stop-slop-fr, "
                "et propose une réécriture conforme. Donne aussi le score sur 50 "
                "et la liste des marqueurs IA détectés.\n\n"
                f"{texte_etudiant}"
            ),
        }
    ],
)

print(response.content[0].text)
```

Pour économiser les tokens sur des relectures répétées, activer le **prompt caching** sur le bloc `system` :

```python
response = client.messages.create(
    model="claude-opus-4-7",
    max_tokens=4096,
    system=[
        {
            "type": "text",
            "text": system_with_refs,
            "cache_control": {"type": "ephemeral"},
        }
    ],
    messages=[{"role": "user", "content": "..."}],
)
```

## Utilisation type

### Cas 1 : relire un rapport de stage avant rendu

```
Tu es un relecteur académique. Applique stop-slop-fr à ce rapport de stage.
Pour chaque paragraphe :
1. Donne le score sur 50
2. Liste les marqueurs IA détectés
3. Propose une réécriture

[coller le texte]
```

### Cas 2 : générer une introduction propre

```
Rédige l'introduction d'un mémoire de master (sciences de gestion) sur
[sujet]. Applique strictement les règles de stop-slop-fr : pas d'ouverture-
cliché, pas de plan annoncé, pas de méta-commentaire. Quatre paragraphes
maximum, sources réelles citées avec Auteur (Année).
```

### Cas 3 : auto-détecter ses propres tics

```
Voici cinq paragraphes que j'ai rédigés sur plusieurs semaines. Identifie
les tics récurrents qui sont aussi des marqueurs IA selon stop-slop-fr.
Donne-moi une liste personnalisée des cinq tournures à supprimer
en priorité dans mes prochains rendus.
```

## Limites

- Le skill ne remplace pas la lecture par un humain (tuteur, pair, correcteur).
- Les règles sont calibrées pour les rendus académiques français standards. Pour des disciplines très spécifiques (mathématiques pures, droit notarial, médecine), certaines tournures bannies ailleurs sont conventionnelles dans la discipline et peuvent rester.
- Le skill ne corrige pas l'orthographe ni la grammaire : utiliser un correcteur dédié (Antidote, LanguageTool) en complément.
- La grille de scoring est qualitative. Deux relectures peuvent attribuer des notes différentes ; le seuil de 35/50 sert d'alerte, pas de verdict.

## Lacunes assumées

Ce skill repose sur la littérature peer-reviewed disponible à mai 2026. Plusieurs limites doivent être déclarées explicitement :

1. **Aucune étude peer-reviewed ne quantifie les marqueurs LLM spécifiques au français académique.** Les listes françaises de ce skill sont des hypothèses convergentes (retours d'enseignants, audit qualitatif), pas des fréquences mesurées. Les listes anglaises (`markers-en.md`) sont, elles, sourcées sur Juzek & Ward 2025, Reinhart et al. 2025, Kobak et al. 2024.

2. **Aucune évaluation indépendante de Compilatio Magister+** sur taux de faux positifs et de faux négatifs en français académique étudiant n'est publiquement disponible.

3. **Pas de seuil quantifié validé pour la densité de connecteurs ni pour la symétrie des paragraphes en français.** Les seuils du fichier `metrics.md` sont des hypothèses à calibrer.

4. **Pas de liste validée d'adverbes en `-ment` spécifiquement LLM en français.** La section correspondante de `phrases.md` est marquée [Anecdotique].

## Limites du skill

- Ce skill ne contourne aucun détecteur commercial. Krishna et al. (2023, NeurIPS) montrent que les détecteurs mesurent la fluidité statistique, pas la paternité ; les humanizers commerciaux atteignent 87-94 % de bypass rates en 2026, ce qui dit moins de leur efficacité que de la fragilité fondamentale des détecteurs.
- Les détecteurs IA actuels (GPTZero, Originality, Turnitin, Compilatio) ont des taux de faux positifs documentés sur texte humain natif et non-natif (Liang et al. 2023 mesurent jusqu'à 61 % de faux positifs sur essais d'étudiants non-natifs).
- L'usage attendu : rédiger soi-même, puis utiliser ce skill comme grille de relecture pour passer la révision humaine d'un correcteur. Pas pour "humaniser" un texte intégralement généré.
- Pour les rendus universitaires, vérifier la charte d'usage de l'IA de l'établissement avant publication.

## Licence

MIT — voir [LICENSE](LICENSE).

## Crédits

Skill inspiré de [`hardikpandya/stop-slop`](https://github.com/hardikpandya/stop-slop) (MIT), adapté pour l'écriture académique française par [Whitemes](https://whitemes.dev).
