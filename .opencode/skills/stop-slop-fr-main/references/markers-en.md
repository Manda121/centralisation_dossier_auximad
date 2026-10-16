# Marqueurs anglais documentés [Robuste]

Liste des mots anglais excédentaires dans les sorties LLM, mesurés sur corpus peer-reviewed. Utile pour les rendus bilingues (abstracts, papers, traductions, présentations internationales).

## 21 focal words — Juzek & Ward (2025, COLING)

Mots dont la fréquence a explosé dans les abstracts PubMed entre 2020 et 2024 ET surreprésentés dans les sorties ChatGPT vs texte humain. Mécanisme probable : alignement RLHF.

| Mot | Fréquence 2020 (occurrences par million) | Fréquence 2024 |
|---|---:|---:|
| delve / delves / delved / delving | 0,21–0,58 | 8,50–14,38 |
| showcasing / showcases | 0,59 | 8,79 |
| underscores / underscore / underscoring | 4,50 | 45,19 |
| intricate / intricacies | 6,22 | 44,22 |
| advancements | 12,49 | 47,17 |
| groundbreaking | n/a | hausse forte |
| aligns | n/a | hausse forte |
| garnered | n/a | hausse forte |
| surpasses / surpassing | n/a | hausse forte |
| comprehending | n/a | hausse forte |
| emphasizing | n/a | hausse forte |
| realm | n/a | hausse forte |
| boasts | n/a | hausse forte |

Source : Juzek & Ward, *Investigating the impact of LLM use on academic writing through word frequency analysis*, COLING 2025.

## Mots à plus de 100× le taux humain — Reinhart et al. (2025, CMU)

Mesurés sur 12 000 textes parallèles GPT-4o vs humain.

| Mot | Ratio GPT-4o / humain |
|---|---:|
| camaraderie | ×162 |
| tapestry | ×155 |
| intricate | ×119 |
| underscore | ×107 |
| unspoken | ×102 |
| amidst | ×100 |
| palpable | ×95 |
| solace | ×95 |
| fleeting | ×84 |
| unravel | ×83 |

`tapestry` apparaît dans 23 % des sorties GPT-4o. `amidst` dans 27 %.

Source : Reinhart et al., *Do LLMs write like humans?*, CMU/PNAS preprint 2025.

## Excess words — Kobak et al. (2024, Science Advances)

Analyse de 15,1 millions d'abstracts PubMed. 379 mots excédentaires en 2024. Estimation : au moins 13,5 % des abstracts 2024 traités par LLM.

Mots communs à fort signal (à éviter en abstract de mémoire ingénieur ou de paper) :
- `across`, `additionally`, `comprehensive`, `crucial`, `enhancing`, `exhibited`
- `insights`, `notably`, `particularly`, `within`, `findings`, `potential`

Source : Kobak et al., *Delving into LLM-assisted writing in biomedical publications*, Science Advances 2024.

## Implication

Pour un abstract en anglais ou une publication internationale, faire un dernier pass et compter les occurrences de ces mots. Plus de 3 occurrences distinctes de la liste Reinhart sur un abstract de 250 mots = signal trop fort.

## Limite

Ces listes sont publiques et déjà connues. Les LLM les plus récents (Claude 4, GPT-5) commencent à les éviter quand on les en informe par prompt système. La fiabilité de la détection sur ces seuls mots décline depuis fin 2025.
