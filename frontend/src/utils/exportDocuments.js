import JSZip from 'jszip'

export function valeurChemin(objet, chemin) {
  return String(chemin || '').split('.').reduce((valeur, cle) => valeur?.[cle], objet)
}

export function texte(valeur) {
  if (valeur === null || valeur === undefined || valeur === '') return '—'
  if (typeof valeur === 'boolean') return valeur ? 'Oui' : 'Non'
  if (typeof valeur === 'object') {
    return valeur.nom || valeur.description || valeur.numeroDossier || valeur.numero || valeur.code || '—'
  }
  return String(valeur)
}

export function echapperHtml(valeur) {
  return String(valeur ?? '').replace(/[&<>"']/g, caractere => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  })[caractere])
}

function echapperXml(valeur) {
  return String(valeur ?? '').replace(/[&<>"']/g, caractere => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;'
  })[caractere])
}

function valeurColonne(ligne, colonne) {
  const valeur = colonne.get ? colonne.get(ligne) : valeurChemin(ligne, colonne.key)
  if (colonne.format === 'date' && valeur) return new Date(valeur).toLocaleDateString('fr-FR')
  if (colonne.format === 'datetime' && valeur) return new Date(valeur).toLocaleString('fr-FR')
  return texte(valeur)
}

function tableauDonnees(colonnes, lignes) {
  const colonnesExport = colonnes.filter(col => col.exportable !== false && col.key !== 'actions')
  return {
    titres: colonnesExport.map(col => col.label),
    cellules: lignes.map(ligne => colonnesExport.map(col => valeurColonne(ligne, col)))
  }
}

function nomFichier(nom) {
  return String(nom || 'export').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-|-$/g, '').toLowerCase() || 'export'
}

function telecharger(blob, nom) {
  const url = URL.createObjectURL(blob)
  const lien = document.createElement('a')
  lien.href = url
  lien.download = nom
  document.body.appendChild(lien)
  lien.click()
  lien.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

function celluleExcel(colonne, ligne, valeur) {
  let numero = colonne + 1
  let lettre = ''
  while (numero > 0) {
    numero--
    lettre = String.fromCharCode(65 + numero % 26) + lettre
    numero = Math.floor(numero / 26)
  }
  return `<c r="${lettre}${ligne}" t="inlineStr"><is><t xml:space="preserve">${echapperXml(valeur)}</t></is></c>`
}

async function fichierExcel(titres, cellules) {
  const zip = new JSZip()
  const lignes = [titres, ...cellules]
  const contenu = lignes.map((ligne, index) => `<row r="${index + 1}">${ligne.map((valeur, colonne) => celluleExcel(colonne, index + 1, valeur)).join('')}</row>`).join('')
  zip.file('[Content_Types].xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>`)
  zip.file('_rels/.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`)
  zip.file('xl/workbook.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Données" sheetId="1" r:id="rId1"/></sheets></workbook>`)
  zip.file('xl/_rels/workbook.xml.rels', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>`)
  zip.file('xl/worksheets/sheet1.xml', `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${contenu}</sheetData></worksheet>`)
  return zip.generateAsync({ type: 'blob', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
}

const styleListe = `
  @page { size: A4 landscape; margin: 10mm; }
  body { margin: 0; color: #111; font-family: Arial, sans-serif; }
  .export-sheet { width: 100%; box-sizing: border-box; padding: 8px; background: white; }
  .export-heading { display:flex; justify-content:space-between; align-items:end; margin-bottom:14px; }
  .export-brand { display:inline-flex; align-items:center; gap:8px; font-size:20px; font-weight:700; letter-spacing:5px; }
  .export-brand img { width:28px; height:28px; object-fit:contain; }
  .export-date { font-size:11px; }
  .export-title { border:1px solid #222; background:#e8ebf0; padding:7px; text-align:center; font-size:17px; font-weight:700; }
  .export-table { width:100%; border-collapse:collapse; table-layout:fixed; font-size:10px; }
  .export-table th, .export-table td { border:1px solid #222; padding:5px 6px; overflow-wrap:anywhere; vertical-align:top; }
  .export-table th { background:#f2f2f2; text-align:left; font-size:11px; }
  .export-table tr { break-inside:avoid; page-break-inside:avoid; }
  .export-footer { margin-top:9px; font-size:10px; text-align:right; }
`

export function htmlListe(titre, colonnes, lignes) {
  const donnees = tableauDonnees(colonnes, lignes)
  const date = new Date().toLocaleDateString('fr-FR')
  return `<div class="export-sheet"><div class="export-heading"><span class="export-brand"><img src="/logo/1000130987.webp" alt="">AUXIMAD</span><span class="export-date">Édité le ${echapperHtml(date)}</span></div><div class="export-title">${echapperHtml(titre)}</div><table class="export-table"><thead><tr>${donnees.titres.map(valeur => `<th>${echapperHtml(valeur)}</th>`).join('')}</tr></thead><tbody>${donnees.cellules.map(ligne => `<tr>${ligne.map(valeur => `<td>${echapperHtml(valeur)}</td>`).join('')}</tr>`).join('')}</tbody></table><div class="export-footer">${lignes.length} résultat(s)</div></div>`
}

export function ouvrirFenetreImpression() {
  const fenetre = window.open('', '_blank')
  if (!fenetre) throw new Error('Autorisez l’ouverture de la fenêtre d’impression.')
  fenetre.document.write('<!doctype html><html><head><meta charset="utf-8"><title>Préparation de l’impression</title></head><body>Préparation du document…</body></html>')
  fenetre.document.close()
  return fenetre
}

export function imprimerHtml(fenetre, titre, html, style = styleListe) {
  fenetre.document.open()
  fenetre.document.write(`<!doctype html><html lang="fr"><head><meta charset="utf-8"><title>${echapperHtml(titre)}</title><style>${style}</style></head><body>${html}</body></html>`)
  fenetre.document.close()
  fenetre.focus()
  const images = [...fenetre.document.images]
  Promise.all(images.map(img => img.complete ? Promise.resolve() : new Promise(resolve => {
    img.addEventListener('load', resolve, { once: true })
    img.addEventListener('error', resolve, { once: true })
  }))).then(() => {
    setTimeout(() => { if (!fenetre.closed) fenetre.print() }, 150)
  })
}

export async function exporterPdfHtml(titre, html, style = styleListe, orientation = 'landscape') {
  const { default: html2pdf } = await import('html2pdf.js')
  const feuilleStyle = document.createElement('style')
  feuilleStyle.textContent = style
  document.head.appendChild(feuilleStyle)
  try {
    await html2pdf().set({
      margin: 7,
      filename: `${nomFichier(titre)}.pdf`,
      image: { type: 'jpeg', quality: 0.98 },
      html2canvas: { scale: 1.7, useCORS: true, backgroundColor: '#ffffff' },
      jsPDF: { unit: 'mm', format: 'a4', orientation },
      pagebreak: { mode: ['css', 'legacy'] }
    }).from(html, 'string').save()
  } finally {
    feuilleStyle.remove()
  }
}

export async function exporterListe({ titre, colonnes, lignes, format }) {
  const { titres, cellules } = tableauDonnees(colonnes, lignes)
  const base = nomFichier(titre)
  if (format === 'pdf') return exporterPdfHtml(titre, htmlListe(titre, colonnes, lignes))
  if (format === 'xlsx') {
    telecharger(await fichierExcel(titres, cellules), `${base}.xlsx`)
    return
  }
  if (format === 'csv') {
    const csv = [titres, ...cellules].map(ligne => ligne.map(valeur => {
      const securise = /^[=+@-]/.test(valeur) ? `'${valeur}` : valeur
      return `"${securise.replace(/"/g, '""')}"`
    }).join(';')).join('\r\n')
    telecharger(new Blob(['\uFEFF', csv], { type: 'text/csv;charset=utf-8' }), `${base}.csv`)
    return
  }
  if (format === 'txt') {
    const txt = [titres, ...cellules].map(ligne => ligne.map(valeur => valeur.replace(/[\t\r\n]+/g, ' ')).join('\t')).join('\r\n')
    telecharger(new Blob([txt], { type: 'text/plain;charset=utf-8' }), `${base}.txt`)
  }
}

export function imprimerListe(fenetre, titre, colonnes, lignes) {
  imprimerHtml(fenetre, titre, htmlListe(titre, colonnes, lignes))
}
