const formatMois = new Intl.DateTimeFormat('fr-FR', { month: 'short', year: 'numeric', timeZone: 'UTC' })

export function construireEvolutionMensuelle(evolution = []) {
  const mois = [...evolution].sort((a, b) => a.mois.localeCompare(b.mois))
  const valeurs = mois.map(item => Math.max(0, Number(item.valeur) || 0))
  const maximum = Math.max(1, ...valeurs)
  const ordre = 10 ** Math.floor(Math.log10(maximum / 4))
  const pas = Math.max(1, [1, 2, 5, 10].find(n => n * ordre >= maximum / 4) * ordre)
  const plafond = Math.ceil(maximum / pas) * pas
  const largeur = 900
  const hauteur = 280
  const gauche = 64
  const droite = largeur - 28
  const haut = 40
  const bas = hauteur - 48
  const positionY = valeur => bas - (valeur / plafond) * (bas - haut)
  const graduations = Array.from({ length: plafond / pas + 1 }, (_, index) => ({
    valeur: index * pas,
    y: positionY(index * pas),
  }))
  const points = mois.map((item, index) => {
    const [annee, numeroMois] = item.mois.split('-').map(Number)
    return {
      mois: item.mois,
      label: formatMois.format(new Date(Date.UTC(annee, numeroMois - 1, 1))),
      labelCourt: `${String(numeroMois).padStart(2, '0')}/${String(annee).slice(-2)}`,
      valeur: valeurs[index],
      x: mois.length === 1 ? (gauche + droite) / 2 : gauche + index * (droite - gauche) / (mois.length - 1),
      y: positionY(valeurs[index]),
    }
  })
  const ligne = points.map(point => `${point.x},${point.y}`).join(' ')
  const aire = points.length ? `${points[0].x},${bas} ${ligne} ${points.at(-1).x},${bas}` : ''
  return { largeur, hauteur, gauche, droite, haut, bas, plafond, graduations, points, ligne, aire }
}
