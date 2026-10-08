import { echapperHtml, texte } from './exportDocuments'

const e = value => echapperHtml(texte(value))
const date = value => value ? new Date(value).toLocaleDateString('fr-FR') : '—'
const nom = personne => [personne?.nom, personne?.prenom].filter(Boolean).join(' ') || '—'
const caseCochee = (label, checked) => `<span class="option"><span class="box">${checked ? '✓' : ''}</span>${label}</span>`

export const styleFiche = `
  @page { size: A4 portrait; margin: 7mm; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: Arial, Helvetica, sans-serif; color: #171717; font-size: 10px; }
  .sheet { width: 100%; padding: 7mm; background: white; }
  .brand { font-size: 19px; font-weight: 800; letter-spacing: 5px; }
  .top { display: flex; align-items: end; justify-content: space-between; border-bottom: 2px solid #222; padding-bottom: 7px; margin-bottom: 8px; }
  .top-small { text-align: right; font-size: 10px; }
  .title { text-align: center; font-weight: 800; font-size: 16px; border: 1px solid #222; padding: 8px 3px; background: #eee; margin: 7px 0; }
  .subtitle { text-align: center; font-size: 11px; font-weight: 700; margin: 5px 0 9px; }
  .section { background: #ddd; border: 1px solid #222; font-weight: 800; text-align: center; padding: 5px; margin-top: 8px; text-transform: uppercase; }
  .grid { display: grid; grid-template-columns: 1fr 1fr; border-left: 1px solid #222; border-top: 1px solid #222; }
  .field { display: flex; gap: 6px; min-height: 25px; border-right: 1px solid #222; border-bottom: 1px solid #222; padding: 5px; }
  .field .label { font-weight: 700; min-width: 95px; }
  .field .value { overflow-wrap: anywhere; }
  .line { display: flex; gap: 10px; padding: 5px 6px; border: 1px solid #222; border-top: 0; min-height: 25px; }
  .line b { min-width: 125px; }
  .option-row { border: 1px solid #222; padding: 6px; display: flex; justify-content: space-around; }
  .option { display: inline-flex; align-items: center; gap: 5px; font-weight: 700; }
  .box { border: 1px solid #222; width: 12px; height: 12px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; }
  table { width: 100%; border-collapse: collapse; table-layout: fixed; }
  th, td { border: 1px solid #222; padding: 5px; text-align: left; vertical-align: top; overflow-wrap: anywhere; }
  th { background: #eee; font-weight: 700; }
  tr { page-break-inside: avoid; break-inside: avoid; }
  .two-col { display: grid; grid-template-columns: 1fr 1fr; }
  .two-col > div { border: 1px solid #222; padding: 6px; min-height: 45px; }
  .notes { border: 1px solid #222; padding: 7px; min-height: 54px; white-space: pre-wrap; }
  .signature { margin-top: 12px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
  .signature > div { min-height: 55px; border-top: 1px solid #222; text-align: center; padding-top: 5px; font-weight: 700; }
  .muted { color: #555; }
  .reference-header { display:grid; grid-template-columns:32% 68%; gap:7px; align-items:start; margin-bottom:7px; }
  .reference-header .brand { margin-bottom:4px; }
  .reference-title { font-weight:800; font-size:16px; text-align:center; }
  .reference-client { border:1px solid #222; padding:6px; line-height:1.55; }
  .reference-client strong { font-size:12px; }
  .reference-split { display:grid; grid-template-columns:1fr 1fr; border-left:1px solid #222; }
  .reference-split > div { border-right:1px solid #222; border-bottom:1px solid #222; padding:5px; min-height:48px; }
  .reference-split .label { display:block; text-align:center; font-weight:700; margin-bottom:6px; }
  .reference-meta { border:1px solid #222; border-top:0; padding:5px; }
  .reference-meta-row { display:grid; grid-template-columns:repeat(4,1fr); gap:4px; margin:3px 0; }
  .reference-meta-row b { margin-right:3px; }
  .reference-checks { display:flex; flex-wrap:wrap; gap:14px; border:1px solid #222; border-top:0; padding:6px; }
  .reference-checks .box { margin-left:4px; }
  .reference-section { text-align:center; border:1px solid #222; border-bottom:0; font-size:12px; padding:4px; margin-top:8px; }
  .reference-table th { background:#fff; font-weight:400; text-align:center; }
  .reference-table th, .reference-table td { padding:4px; }
  .reference-operations td { height:22px; }
  .reference-operations .phase-head { font-weight:700; text-align:center; background:#f5f5f5; }
  .reference-ot-header { display:grid; grid-template-columns:30% 70%; min-height:73px; align-items:center; }
  .reference-ot-header .option { display:flex; margin:3px 0; }
  .reference-ot-header .reference-title { line-height:1.6; }
  .reference-ot-body { display:grid; grid-template-columns:1fr 1fr; border-top:1px solid #222; border-left:1px solid #222; }
  .reference-ot-body > div { border-right:1px solid #222; border-bottom:1px solid #222; padding:6px; min-height:44px; }
  .reference-ot-body .tall { min-height:69px; }
  .reference-ot-body .small-title { display:block; font-size:10px; margin-bottom:5px; }
  .reference-ot-body .center { text-align:center; }
  .reference-ot-merch td { height:55px; }
  .reference-ot-end { display:grid; grid-template-columns:1fr 1fr; border-left:1px solid #222; }
  .reference-ot-end > div { border-right:1px solid #222; border-bottom:1px solid #222; padding:6px; min-height:75px; }
`

export function ficheDossierReference(dossier, { marchandises = [], documents = [], remises = [], typesDocuments = [], operations = [], conteneurs = [] } = {}) {
  const ot = dossier.ordreTransit || {}
  const exp = ot.expedition || {}
  const nomClient = nom(dossier.client)
  const remisesMap = new Map(remises.map(r => [r.typeDocument?.id, r.dateRemise]))
  const idsDocuments = new Set(documents.map(doc => doc.typeDocument?.id))
  const documentsManquants = typesDocuments.filter(type => !idsDocuments.has(type.id))
  const nombreLignesDocuments = Math.max(documents.length, documentsManquants.length, 1)
  const phases = [...new Map(operations.filter(x => x.operation?.phaseDossier).map(x => [x.operation.phaseDossier.id, x.operation.phaseDossier])).values()]
  const phaseGroups = phases.map(phase => ({ phase, items: operations.filter(x => x.operation?.phaseDossier?.id === phase.id) }))
  const gauche = phaseGroups.filter((_, index) => index % 2 === 0).flatMap(g => [{ heading: g.phase.nom }, ...g.items])
  const droite = phaseGroups.filter((_, index) => index % 2 === 1).flatMap(g => [{ heading: g.phase.nom }, ...g.items])
  const maximum = Math.max(gauche.length, droite.length, 1)
  const celluleOperation = item => item?.heading
    ? `<td colspan="2" class="phase-head">PHASE ${e(item.heading)}</td>`
    : `<td>${e(item?.operation?.description)}</td><td>${item?.estFait ? e(date(item.dateMvt)) : '___/___/____'}</td>`
  const marchandisesRows = marchandises.length ? marchandises : [{}]
  return `<div class="sheet">
    <div class="reference-header"><div><div class="brand">AUXIMAD</div>AGENCE ${e(dossier.agence?.nom)}</div><div class="reference-title">DOSSIER IMPORT ${e(ot.typeProduction?.nom || 'TRANSIT')} N° ${e(dossier.numeroDossier)}<br>ANNÉE ${e(dossier.dateCreation ? new Date(dossier.dateCreation).getFullYear() : '')}</div></div>
    <div class="reference-client"><strong>Client : ${e(nomClient)}</strong><br>Adresse : ${e(dossier.client?.adresse)} &nbsp;&nbsp; Tél : ${e(dossier.client?.telephone)}<br>Adresse Mail : ${e(dossier.client?.email)} &nbsp;&nbsp; NIF : ${e(dossier.client?.nif)} &nbsp;&nbsp; STAT : ${e(dossier.client?.stat)}</div>
    <div class="reference-split"><div><span class="label">DESTINATAIRE :</span>${e(nom(ot.destinataire))}</div><div><span class="label">PARTIE À NOTIFIER :</span>${e(nom(ot.destinationFacture || ot.destinataire))}</div></div>
    <div class="reference-meta">
      <div class="reference-meta-row"><span><b>Doss ouvert le :</b>${e(date(dossier.dateCreation))}</span><span><b>N° Doss mère :</b>${e(dossier.dossierMere?.numeroDossier || dossier.numeroDossier)}</span><span><b>N° O.T :</b>${e(ot.numeroOrdreTransit)}</span><span><b>Réf. Cli :</b>${e(ot.numeroCommande)}</span></div>
      <div class="reference-meta-row"><span><b>Navire :</b>${e(exp.navire?.nom || exp.numeroVol)}</span><span><b>Voyage :</b>${e(exp.numeroVoyage)}</span><span><b>Embarquement à :</b>${e(exp.lieuBl || exp.lieuLta)}</span><span><b>Arrivée le :</b>${e(date(exp.dateArrivee))}</span></div>
      <div class="reference-meta-row"><span><b>Pays origine :</b>${e(dossier.paysOrigine?.nom)}</span><span><b>Pays dest :</b>${e(dossier.paysDestination?.nom)}</span><span><b>BL N° :</b>${e(exp.numeroBl || exp.numeroLta)}</span><span><b>Du :</b>${e(date(exp.dateBl || exp.dateLta))}</span></div>
      <div class="reference-meta-row"><span><b>Frêt payé à :</b>${e(dossier.lieuPaiementFret)}</span><span><b>Incoterm :</b>${e(dossier.inconterme?.abreviation)}</span><span><b>Phase :</b>${e(dossier.phaseDossier?.nom)}</span><span><b>Statut :</b>${e(dossier.statutDossier?.nom)}</span></div>
    </div>
    <div class="reference-checks"><span>Prépayé ${caseCochee('', dossier.estPrepaye)}</span><span>Assurance ${caseCochee('', ot.estAssure)}</span><span>Groupage ${caseCochee('', dossier.estGroupage)}</span><span>Provision ${caseCochee('', dossier.estProvisionne)}</span><span>Montant (Ar) ${e(dossier.provisionDemandee)}</span></div>
    <div class="reference-section">INFORMATIONS SUR LES MARCHANDISES</div>
    <table class="reference-table"><thead><tr><th style="width:22%">Marques &amp; Numéros</th><th style="width:9%">Nbre CTN</th><th style="width:10%">Nbre Colis</th><th style="width:32%">Nature Marchandise</th><th style="width:12%">Poids brut (Kg)</th><th style="width:8%">Volume (m³)</th><th style="width:7%">Observation</th></tr></thead><tbody>${marchandisesRows.map(m => `<tr><td>${e([m.marque, m.modele].filter(Boolean).join(' / '))}</td><td>${e(conteneurs.length || '')}</td><td>${e(m.quantite)}</td><td>${e(m.description || ot.natureMarchandise?.nom)}</td><td>${e(m.poids)}</td><td>${e(m.volume)}</td><td></td></tr>`).join('')}<tr><td colspan="4" style="text-align:right">TOTAL</td><td>${e(marchandises.reduce((total, m) => total + Number(m.poids || 0), 0))}</td><td>${e(marchandises.reduce((total, m) => total + Number(m.volume || 0), 0))}</td><td></td></tr></tbody></table>
    <div class="reference-section">SUIVI DES OPÉRATIONS</div>
    <table class="reference-table reference-operations"><tbody>${Array.from({ length: maximum }, (_, i) => `<tr>${celluleOperation(gauche[i])}${celluleOperation(droite[i])}</tr>`).join('')}</tbody></table>
    <div class="reference-section">SUIVI DES DOCUMENTS</div>
    <table class="reference-table"><thead><tr><th style="width:28%">DOCUMENT JOINT</th><th style="width:13%">Reçus le</th><th style="width:8%">Nb orig</th><th style="width:8%">Nb copie</th><th style="width:25%">DOCUMENT MANQUANT</th><th style="width:7%">Sous Miss</th><th style="width:11%">Relance</th></tr></thead><tbody>${Array.from({ length: nombreLignesDocuments }, (_, index) => { const doc = documents[index]; const manque = documentsManquants[index]; return `<tr><td>${doc ? e(doc.typeDocument?.nom) : ''}</td><td>${doc ? e(date(remisesMap.get(doc.typeDocument?.id))) : ''}</td><td></td><td></td><td>${manque ? e(manque.nom) : ''}</td><td></td><td></td></tr>` }).join('')}</tbody></table>
  </div>`
}

export function ficheOrdreTransitReference(ot, { contrat = null, marchandises = [], instructions = [], documents = [] } = {}) {
  const exp = ot.expedition || {}
  const mode = String(exp.modeTransport?.nom || exp.modeTransport || ot.typeProduction?.nom || '').toLowerCase()
  const client = ot.client || {}
  const facturation = ot.destinationFacture || client
  return `<div class="sheet">
    <div class="reference-ot-header"><div>${caseCochee('MARITIME', mode.includes('maritime'))}${caseCochee('AÉRIEN', mode.includes('aérien') || mode.includes('aerien'))}${caseCochee('COMBINÉ', mode.includes('combin'))}</div><div class="reference-title">ORDRE DE TRANSIT IMPORT<br><span style="font-size:12px;font-weight:400">À LA SOCIÉTÉ AUXIMAD<br>Agence ${e(ot.agence?.nom)} · OT N° ${e(ot.numeroOrdreTransit)}</span></div></div>
    <div class="reference-ot-body"><div class="tall"><span class="small-title">DONNEUR D’ORDRE</span><strong>${e(nom(client))}</strong><br>${e(client.adresse)}<br>Tél : ${e(client.telephone)}</div><div class="tall">${caseCochee('Constats systématiques à chaque transfert de responsabilité', false)}<br>${caseCochee('Constat en cas d’avaries ou pertes apparentes', false)}<br>${caseCochee('Pas de constats', false)}</div>
    <div class="tall"><span class="small-title">IMPORTATEUR</span><strong>${e(nom(facturation))}</strong><br>${e(facturation.adresse)}<br>Tél : ${e(facturation.telephone)}</div><div class="tall"><div class="center">NOM DE L’EXPERT</div><hr>Acheminement jusqu’à destination finale<br>${caseCochee('Par train', false)} &nbsp; ${caseCochee('Par camion', false)} &nbsp; ${caseCochee('Par avion', false)} &nbsp; ${caseCochee('Par nos soins', false)}</div>
    <div class="tall"><span class="small-title">CAUTION</span>${e(ot.caution)}</div><div class="tall">Bureau de dédouanement : <strong>${e(ot.bureauDedouanement?.nom)}</strong><br><br>Livraison à<br><strong>${e(ot.destinationFinale || nom(ot.destinataire))}</strong></div>
    <div><span class="small-title">B.L. ${e(exp.numeroBl || exp.numeroLta)}</span>Point d’émission : ${e(exp.lieuBl || exp.lieuLta)}</div><div>Facture à : ${e(nom(facturation))}</div>
    <div>Navire(s) : ${e(exp.navire?.nom || exp.numeroVol)}<br>Attendu / Arrivée le : ${e(date(exp.dateArrivee || exp.datePrevue))}</div><div>C/L N° ${e(contrat?.numero)} &nbsp; du ${e(date(contrat?.dateCreation))}<br>Point d’émission : ${e(contrat?.lieuEmission)}</div>
    <div>Point de déchargement : ${e(exp.lieuDechargement)}</div><div>Destination finale : ${e(ot.destinationFinale)}</div></div>
    <table class="reference-table reference-ot-merch"><thead><tr><th style="width:55%">Marques et nombre des colis : Nature de la marchandise</th><th style="width:15%">Poids net</th><th style="width:15%">Poids brut</th><th style="width:15%">Volume</th></tr></thead><tbody>${(marchandises.length ? marchandises : [{}]).map(m => `<tr><td><b>${e(m.quantite)} ${e(m.uniteMesure?.nom)} — ${e(m.description || ot.natureMarchandise?.nom)}</b><br>${e(m.marque)}</td><td>${e(m.poids)}</td><td>${e(m.poids)}</td><td>${e(m.volume)}</td></tr>`).join('')}</tbody></table>
    <div class="reference-ot-end"><div><u>DOCUMENTS JOINTS :</u><br>${documents.length ? documents.map(doc => `${e(doc.typeDocument?.nom)} ${caseCochee('', true)}`).join('<br>') : '—'}</div><div></div></div>
    <div class="reference-section" style="text-align:left;border-bottom:0">INSTRUCTIONS PARTICULIÈRES</div><div class="notes">${instructions.length ? instructions.map(item => e(item.instruction || item.contenu || item.description || item.texte)).join('<br>') : '—'}</div>
    <div class="reference-ot-end"><div><b>Veuillez délivrer le BAD à AUXIMAD</b><br><br>Le donneur d’ordre confirme les renseignements et les documents transmis pour établir la déclaration en douane.</div><div>Fait à ${e(ot.agence?.nom)} le ${e(date(ot.dateCreation))}<br>Le donneur d’ordre<br><br><br><div class="center">(Signature)</div></div></div>
  </div>`
}
