import axios from 'axios'
import { useNotification } from '../composables/useNotification'

export function messageSuppression(impact) {
    const lignes = ['Confirmer la suppression définitive ?']
    if (impact.suppressions?.length) {
        lignes.push('', 'Éléments supprimés :', ...impact.suppressions.map(d => `• ${d.nombre} ${d.objet}`))
    }
    if (impact.liensRetires?.length) {
        lignes.push('', 'Liens retirés (éléments conservés) :', ...impact.liensRetires.map(d => `• ${d.nombre} ${d.objet}`))
    }
    if (impact.dossiersClotures) lignes.push('', `${impact.dossiersClotures} dossier(s) seront automatiquement clôturés.`)
    lignes.push('', 'Cette action est irréversible.')
    return lignes.join('\n')
}

/** Le serveur calcule l'impact avant toute modification. Une annulation ne relance pas la requête. */
export async function requeteSuppression(client, configuration) {
    const { confirm } = useNotification()
    let confirmation
    for (; ;) {
        try {
            return await client.request({
                ...configuration,
                headers: { ...configuration.headers, ...(confirmation ? { 'X-Confirmation-Suppression': confirmation } : {}) }
            })
        } catch (error) {
            const data = error.response?.data
            if (error.response?.status !== 409 || data?.code !== 'CONFIRMATION_SUPPRESSION') throw error
            const ok = await confirm(messageSuppression(data.impact))
            if (!ok) throw new axios.CanceledError('Suppression annulée')
            confirmation = data.impact.confirmation
        }
    }
}