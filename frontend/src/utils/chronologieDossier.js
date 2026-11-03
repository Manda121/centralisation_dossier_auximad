const MILLISECONDES_PAR_JOUR = 24 * 60 * 60 * 1000

export function dateValide(valeur) {
    if (!valeur) return null
    const date = new Date(valeur)
    return Number.isNaN(date.getTime()) ? null : date
}

// Comparer les jours du calendrier local, y compris lors d'un changement d'heure.
export function joursDepuisOuverture(ouverture, evenement) {
    const debut = dateValide(ouverture)
    const fin = dateValide(evenement)
    if (!debut || !fin) return null
    const jourDebut = Date.UTC(debut.getFullYear(), debut.getMonth(), debut.getDate())
    const jourFin = Date.UTC(fin.getFullYear(), fin.getMonth(), fin.getDate())
    return Math.round((jourFin - jourDebut) / MILLISECONDES_PAR_JOUR)
}

export function repereChronologique(ouverture, evenement) {
    const jours = joursDepuisOuverture(ouverture, evenement)
    if (jours === null) return '—'
    if (jours === 0) return 'J0'
    return jours > 0 ? `+${jours} j` : `−${Math.abs(jours)} j`
}
