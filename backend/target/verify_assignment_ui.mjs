import { readFile, writeFile, mkdir } from 'node:fs/promises'
import assert from 'node:assert/strict'
import path from 'node:path'

const root = process.cwd()
const target = path.join(root, 'backend/target')
const runtime = JSON.parse((await readFile(path.join(target, 'ui_runtime.json'), 'utf8')).replace(/^\uFEFF/, ''))
const api = 'http://localhost:18081/api'
const origin = 'http://localhost:5173'
const report = { checks: [], consoleErrors: [], networkErrors: [] }
const resumeMovements = process.env.UI_RESUME === 'movements'
const resumeDetails = process.env.UI_RESUME === 'details' || resumeMovements
if (resumeDetails) report.checks = JSON.parse(await readFile(path.join(target, 'ui-verification-report.json'), 'utf8')).checks
const pause = ms => new Promise(resolve => setTimeout(resolve, ms))
async function json(url, options = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(20000) })
  const data = await response.json().catch(() => null)
  return { response, data }
}
const login = await json(api + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ nomUtilisateur: 'user1', codeAgence: runtime.codeAgence, motDePasse: 'UiVerification_2026' }) })
assert.equal(login.response.status, 200, 'La connexion au backend isolé doit réussir')
const token = login.data.token
const user = { ...login.data }; delete user.token
async function request(endpoint, method = 'GET', body) {
  return json(api + endpoint, { method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }) })
}

// Les requêtes vers plusieurs agences sont persistées dans le schéma isolé.
if (!resumeDetails) {
const source = (await request('/dossiers/140')).data
assert.ok(source?.id, 'Dossier de test interagences disponible')
const agences = (await request('/agences')).data
const autres = agences.filter(a => a.id !== source.agence.id).slice(0, 2)
const creees = []
for (const agence of autres) {
  const result = await request('/demandes-interagence', 'POST', {
    idDossier: source.id, idAgenceDestinataire: agence.id, motif: 'Vérification multi-agences isolée',
  })
  assert.equal(result.response.status, 201, `Créer une demande distincte vers ${agence.nom}`)
  creees.push(result.data)
}
for (const demande of creees) {
  const result = await request(`/demandes-interagence/${demande.id}/repondre`, 'POST', { acceptee: true, motifReponse: 'Test isolé accepté' })
  assert.equal(result.response.status, 200, 'Accepter une demande et créer un sous-dossier')
  assert.equal(result.data.statut, 'ACCEPTEE')
}
const enfants = (await request(`/inter-agences/sous-dossiers/${source.id}`)).data
assert.equal(enfants.length, 2, 'Un dossier doit pouvoir associer deux autres agences')
const doublon = await request('/demandes-interagence', 'POST', { idDossier: source.id, idAgenceDestinataire: autres[0].id })
assert.equal(doublon.response.status, 400, 'Doublon vers la même agence refusé')
report.checks.push('API : deux demandes vers deux agences, deux acceptations/sous-dossiers, doublon refusé')

const nonMaritime = (await request('/dossiers/299')).data
assert.notEqual(nonMaritime.typeProduction.modeTransport.nom.toLowerCase(), 'maritime')
const interdit = await request('/conteneurs', 'POST', { dossier: { id: nonMaritime.id }, numeroContainer: 'UI-NONMARITIME' })
assert.equal(interdit.response.status, 400, 'Le serveur doit refuser le conteneur non maritime')
report.checks.push('API : assignation de conteneur à un dossier non maritime refusée')
}

const pages = await (await fetch('http://localhost:19222/json/list')).json()
const ws = new WebSocket(pages.find(p => p.type === 'page').webSocketDebuggerUrl)
await new Promise((resolve, reject) => { ws.addEventListener('open', resolve, { once: true }); ws.addEventListener('error', reject, { once: true }) })
let seq = 0
const pending = new Map()
ws.addEventListener('message', event => {
  const message = JSON.parse(event.data)
  if (message.id) {
    const pair = pending.get(message.id); if (!pair) return
    pending.delete(message.id)
    message.error ? pair.reject(new Error(message.error.message)) : pair.resolve(message.result)
  } else if (message.method === 'Runtime.exceptionThrown') report.consoleErrors.push(message.params.exceptionDetails.text)
  else if (message.method === 'Network.responseReceived' && message.params.response.status >= 400
      && !(message.params.response.status === 404 && /\/inter-agences\/dossier-mere\//.test(message.params.response.url))) {
    report.networkErrors.push({ url: message.params.response.url, status: message.params.response.status })
  }
})
function cdp(method, params = {}) {
  const id = ++seq
  return new Promise((resolve, reject) => { pending.set(id, { resolve, reject }); ws.send(JSON.stringify({ id, method, params })) })
}
async function evaluate(expression) {
  const result = await cdp('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.exception?.description || result.exceptionDetails.text)
  return result.result.value
}
async function waitFor(expression, label, timeout = 20000) {
  const start = Date.now()
  while (Date.now() - start < timeout) {
    if (await evaluate(expression)) return
    await pause(180)
  }
  const body = await evaluate('document.body.innerText.slice(0,1200)')
  throw new Error(`Attente échouée : ${label}. Écran : ${body}`)
}
async function navigate(route) {
  await cdp('Page.navigate', { url: origin + route })
  await waitFor('document.readyState === "complete" && !!document.querySelector("main")', 'chargement de ' + route)
}
async function clickText(text) {
  const found = await evaluate(`(() => { const el = [...document.querySelectorAll('button,a')].find(e => e.offsetParent !== null && e.textContent.trim().includes(${JSON.stringify(text)}) && !e.disabled); if(!el)return false; el.scrollIntoView({block:'center'}); el.click(); return true })()`)
  assert.ok(found, 'Bouton/lien disponible : ' + text)
}
await cdp('Runtime.enable'); await cdp('Page.enable'); await cdp('Network.enable')
await cdp('Emulation.setTimezoneOverride', { timezoneId: 'Europe/Moscow' })
await cdp('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1100, deviceScaleFactor: 1, mobile: false })
await cdp('Page.addScriptToEvaluateOnNewDocument', { source: `
  (() => {
    const initial = XMLHttpRequest.prototype.open;
    XMLHttpRequest.prototype.open = function(...args) { if(typeof args[1] === 'string') args[1] = args[1].replace('http://localhost:8080/api', ${JSON.stringify(api)}); return initial.apply(this,args) };
    try { localStorage.setItem('auximad_token', ${JSON.stringify(token)}); localStorage.setItem('auximad_user_info', ${JSON.stringify(JSON.stringify(user))}); } catch (_) {}
  })();` })
const captures = path.join(target, 'ui-captures'); await mkdir(captures, { recursive: true })
async function screenshot(name, selector) {
  if (selector) await evaluate(`document.querySelector(${JSON.stringify(selector)})?.scrollIntoView({block:'start'})`)
  await pause(200)
  const shot = await cdp('Page.captureScreenshot', { format: 'png' })
  await writeFile(path.join(captures, name + '.png'), Buffer.from(shot.data, 'base64'))
}
try {
  if (!resumeDetails) {
  await navigate('/marchandises')
  await waitFor('document.querySelector(".assignment-picker")?.innerText.includes("Assigner les marchandises")', 'dossiers du jour marchandises')
  assert.ok(await evaluate('document.querySelector(".assignment-picker").innerText.includes("Dossiers du jour")'))
  await screenshot('marchandises-choix')
  await clickText('Assigner les marchandises')
  await waitFor('!!document.querySelector(".assignment-context") && !!document.querySelector(".table")', 'marchandises du dossier sélectionné')
  await screenshot('marchandises-dossier')
  report.checks.push('Navigateur : dossiers du jour → sélection → marchandises de l’OT du dossier')

  await navigate('/conteneurs')
  await waitFor('document.querySelector(".assignment-picker")?.innerText.includes("Assigner les conteneurs")', 'dossier maritime du jour')
  await clickText('Assigner les conteneurs')
  await waitFor('document.querySelector(".assignment-context")?.innerText.includes("Maritime") && !!document.querySelector("table tbody tr")', 'conteneurs du dossier maritime')
  await screenshot('conteneurs-dossier')
  await navigate('/conteneurs?dossierId=299')
  await waitFor('document.querySelector(".alert-danger")?.innerText.includes("maritimes")', 'refus du dossier non maritime dans l’interface')
  report.checks.push('Navigateur : sélection maritime → conteneurs ; URL non maritime bloquée')

  await navigate('/documents/dossiers?dossierId=31')
  await waitFor('document.querySelectorAll(".doc-item--remis").length > 0 && document.querySelectorAll(".doc-item--manquant").length > 0', 'documents remis et manquants')
  const avant = await evaluate('document.querySelectorAll(".doc-item--remis").length')
  const docId = await evaluate(`(() => { const el=document.querySelector('.doc-item--manquant .doc-checkbox'); const id=el.id; el.scrollIntoView({block:'center'}); el.click(); return id })()`)
  await waitFor('!!document.querySelector(".doc-item--selectionne")', 'document sélectionné distingué')
  await screenshot('documents-selection', '.documents-section')
  await clickText('Assigner tout')
  await waitFor(`document.getElementById(${JSON.stringify(docId)})?.closest('.doc-item')?.classList.contains('doc-item--remis')`, 'document enregistré distingué en vert')
  assert.ok(await evaluate(`document.querySelectorAll('.doc-item--remis').length > ${avant}`))
  await screenshot('documents-remis', '.documents-section')
  report.checks.push('Navigateur/API : document manquant → sélection à enregistrer → remise persistée en vert')
  }

  if (!resumeMovements) {
  await navigate('/dossiers/31')
  await waitFor('!!document.querySelector(".dossier-heading__workflow")', 'phase et statut dans l’en-tête')
  assert.equal(await evaluate('document.body.innerText.includes("Chronologie du dossier")'), false)
  assert.equal(await evaluate('document.body.innerText.includes("Voir l\'OT associé")'), false)
  assert.ok(await evaluate("!!document.querySelector('a[href^=\"/ordres-transit/\"]')"))
  await screenshot('detail-en-tete')
  report.checks.push('Navigateur : phase/statut près des exports, numéro d’OT cliquable, aucune chronologie sur le détail')
  }

  await navigate('/mouvements-dossier/creer?dossierId=31')
  await waitFor('document.querySelectorAll(".dossier-timeline__event").length > 1 && [...document.querySelectorAll("th")].some(e => e.textContent.includes("Depuis l’ouverture"))', 'chronologie et repères dans la liste des mouvements')
  await evaluate('document.querySelector(".mvt-chronologie").open = true')
  assert.ok(await evaluate('document.querySelector(".dossier-timeline").innerText.includes("J0")'))
  assert.ok(await evaluate('/\\+\\d+ j/.test(document.querySelector(".dossier-timeline").innerText)'))
  await screenshot('mouvements-chronologie', '.dossier-timeline')
  report.checks.push('Navigateur : chronologie J0 et +n j dans la page de création des mouvements')
  assert.deepEqual(report.consoleErrors, [], 'Aucune exception JavaScript dans les parcours')
  assert.deepEqual(report.networkErrors, [], 'Aucune réponse HTTP en erreur pendant les parcours valides')
  report.success = true
  report.checks = [...new Set(report.checks)]
  await writeFile(path.join(target, 'ui-verification-report.json'), JSON.stringify(report, null, 2))
  console.log(JSON.stringify({ success: true, checks: report.checks }, null, 2))
} catch (error) {
  report.success = false; report.error = error.message
  await screenshot('erreur').catch(() => {})
  await writeFile(path.join(target, 'ui-verification-report.json'), JSON.stringify(report, null, 2))
  console.error(error.message); process.exitCode = 1
} finally { ws.close() }
