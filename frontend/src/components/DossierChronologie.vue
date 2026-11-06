<script setup>
import { computed } from 'vue'
import { dateValide, repereChronologique } from '../utils/chronologieDossier'

const props = defineProps({
    dossier: { type: Object, required: true },
    operations: { type: Array, default: () => [] },
    erreur: { type: Boolean, default: false },
})

const evenements = computed(() => props.operations.filter(item => item.estFait).slice().sort((a, b) => {
    const dateA = dateValide(a.dateMvt)?.getTime() ?? Number.POSITIVE_INFINITY
    const dateB = dateValide(b.dateMvt)?.getTime() ?? Number.POSITIVE_INFINITY
    return dateA - dateB || (a.mvtId ?? a.operation?.id ?? 0) - (b.mvtId ?? b.operation?.id ?? 0)
}))
const auteurOuverture = computed(() => {
    const auteur = props.dossier.utilisateur
    return [auteur?.prenom, auteur?.nom].filter(Boolean).join(' ') || auteur?.nomUtilisateur || ''
})

function afficherDate(valeur) {
    const date = dateValide(valeur)
    return date ? date.toLocaleString('fr-FR', {
        day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    }) : 'Date non renseignée'
}
</script>

<template>
    <section class="card border shadow-sm mb-3" aria-labelledby="dossier-chronologie-title">
        <div class="card-header bg-white border-bottom py-3">
            <h5 id="dossier-chronologie-title" class="mb-1 fw-semibold text-dark">Chronologie du dossier</h5>
            <p class="small text-secondary mb-0">Les repères indiquent les jours écoulés depuis l’ouverture.</p>
        </div>
        <div class="card-body">
            <p v-if="erreur" class="alert alert-warning small" role="status">
                Les opérations réalisées ne sont pas disponibles. Rechargez le dossier pour afficher sa chronologie complète.
            </p>
            <ol class="dossier-timeline" aria-label="Ouverture et opérations réalisées">
                <li class="dossier-timeline__event dossier-timeline__event--opening">
                    <span class="dossier-timeline__marker" aria-hidden="true">●</span>
                    <span class="dossier-timeline__offset">{{ repereChronologique(dossier.dateCreation, dossier.dateCreation) }}</span>
                    <div class="dossier-timeline__content">
                        <div class="dossier-timeline__title">Ouverture du dossier</div>
                        <time v-if="dateValide(dossier.dateCreation)" :datetime="dossier.dateCreation" class="dossier-timeline__date">{{ afficherDate(dossier.dateCreation) }}</time>
                        <span v-else class="dossier-timeline__date">Date non renseignée</span>
                        <p v-if="auteurOuverture" class="dossier-timeline__meta mb-0">Créé par {{ auteurOuverture }}</p>
                    </div>
                </li>
                <li v-for="evenement in evenements" :key="evenement.mvtId ?? evenement.operation?.id" class="dossier-timeline__event">
                    <span class="dossier-timeline__marker" aria-hidden="true">✓</span>
                    <span class="dossier-timeline__offset">{{ repereChronologique(dossier.dateCreation, evenement.dateMvt) }}</span>
                    <div class="dossier-timeline__content">
                        <div class="dossier-timeline__title">{{ evenement.operation?.description || 'Opération réalisée' }}</div>
                        <time v-if="dateValide(evenement.dateMvt)" :datetime="evenement.dateMvt" class="dossier-timeline__date">{{ afficherDate(evenement.dateMvt) }}</time>
                        <span v-else class="dossier-timeline__date">Date non renseignée</span>
                        <div class="dossier-timeline__tags">
                            <span v-if="evenement.operation?.phaseDossier?.nom">{{ evenement.operation.phaseDossier.nom }}</span>
                            <span v-if="evenement.operation?.statutDossier?.nom">{{ evenement.operation.statutDossier.nom }}</span>
                        </div>
                    </div>
                </li>
            </ol>
            <p v-if="!erreur && !evenements.length" class="small text-secondary mt-3 mb-0">Aucune opération réalisée depuis l’ouverture.</p>
        </div>
    </section>
</template>

<style scoped>
.dossier-timeline { list-style: none; margin: 0; padding: 0; }
.dossier-timeline__event { position: relative; display: grid; grid-template-columns: 1.5rem 4.5rem minmax(0, 1fr); gap: .75rem; padding: 0 0 1.5rem; }
.dossier-timeline__event:last-child { padding-bottom: 0; }
.dossier-timeline__event:not(:last-child)::before { content: ''; position: absolute; top: 1.5rem; bottom: 0; left: .7rem; width: 2px; background: #d5e4db; }
.dossier-timeline__marker { display: inline-flex; width: 1.5rem; height: 1.5rem; align-items: center; justify-content: center; border: 1px solid #b9ddc8; border-radius: 50%; color: #146c43; background: #e9f7ef; font-size: .75rem; }
.dossier-timeline__event--opening .dossier-timeline__marker { color: #3269ab; background: #eaf1fb; border-color: #c6d8f4; font-size: .6rem; }
.dossier-timeline__offset { align-self: start; padding: .2rem .3rem; border-radius: .35rem; color: #3269ab; background: #eef4fc; font-size: .75rem; font-weight: 700; text-align: center; white-space: nowrap; }
.dossier-timeline__title { font-size: .85rem; color: #24364a; font-weight: 600; overflow-wrap: anywhere; }
.dossier-timeline__date { display: block; margin-top: .2rem; color: #64748b; font-size: .75rem; }
.dossier-timeline__meta { margin-top: .25rem; color: #64748b; font-size: .75rem; }
.dossier-timeline__tags { display: flex; flex-wrap: wrap; gap: .3rem; margin-top: .4rem; }
.dossier-timeline__tags span { background: #f1f5f9; border: 1px solid #e2e8f0; padding: .15rem .45rem; border-radius: .3rem; color: #475569; font-size: .65rem; }
@media (max-width: 575px) { .dossier-timeline__event { grid-template-columns: 1.5rem 3.8rem minmax(0, 1fr); gap: .5rem; } }
</style>
