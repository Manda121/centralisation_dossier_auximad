import{a as M,t as R}from"./ExportActions-C1fiM2j8.js";const e=t=>M(R(t)),p=t=>t?new Date(t).toLocaleDateString("fr-FR"):"—",c=t=>[t?.nom,t?.prenom].filter(Boolean).join(" ")||"—",s=(t,l)=>`<span class="option"><span class="box">${l?"✓":""}</span>${t}</span>`,P=`
  @page { size: A4 portrait; margin: 7mm; }
  * { box-sizing: border-box; }
  body { margin: 0; font-family: Arial, Helvetica, sans-serif; color: #171717; font-size: 10px; }
  .sheet { width: 100%; padding: 7mm; background: white; }
  .brand { display:inline-flex; align-items:center; gap:7px; font-size: 19px; font-weight: 800; letter-spacing: 5px; }
  .brand img, .brand-inline img { width:24px; height:24px; object-fit:contain; }
  .brand-inline { display:inline-flex; align-items:center; gap:5px; white-space:nowrap; vertical-align:middle; }
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
`;function S(t,{marchandises:l=[],documents:b=[],remises:m=[],typesDocuments:g=[],operations:d=[],conteneurs:h=[]}={}){const o=t.ordreTransit||{},r=o.expedition||{},i=c(t.client),D=new Map(m.map(n=>[n.typeDocument?.id,n.dateRemise])),N=new Set(b.map(n=>n.typeDocument?.id)),x=g.filter(n=>!N.has(n.id)),A=Math.max(b.length,x.length,1),u=[...new Map(d.filter(n=>n.operation?.phaseDossier).map(n=>[n.operation.phaseDossier.id,n.operation.phaseDossier])).values()].map(n=>({phase:n,items:d.filter(a=>a.operation?.phaseDossier?.id===n.id)})),v=u.filter((n,a)=>a%2===0).flatMap(n=>[{heading:n.phase.nom},...n.items]),$=u.filter((n,a)=>a%2===1).flatMap(n=>[{heading:n.phase.nom},...n.items]),T=Math.max(v.length,$.length,1),y=n=>n?.heading?`<td colspan="2" class="phase-head">PHASE ${e(n.heading)}</td>`:`<td>${e(n?.operation?.description)}</td><td>${n?.estFait?e(p(n.dateMvt)):"___/___/____"}</td>`,I=l.length?l:[{}];return`<div class="sheet">
    <div class="reference-header"><div><div class="brand"><img src="/logo/1000130987.webp" alt="">AUXIMAD</div><br>AGENCE ${e(t.agence?.nom)}</div><div class="reference-title">DOSSIER IMPORT ${e(o.typeProduction?.nom||"TRANSIT")} N° ${e(t.numeroDossier)}<br>ANNÉE ${e(t.dateCreation?new Date(t.dateCreation).getFullYear():"")}</div></div>
    <div class="reference-client"><strong>Client : ${e(i)}</strong><br>Adresse : ${e(t.client?.adresse)} &nbsp;&nbsp; Tél : ${e(t.client?.telephone)}<br>Adresse Mail : ${e(t.client?.email)} &nbsp;&nbsp; NIF : ${e(t.client?.nif)} &nbsp;&nbsp; STAT : ${e(t.client?.stat)}</div>
    <div class="reference-split"><div><span class="label">DESTINATAIRE :</span>${e(c(o.destinataire))}</div><div><span class="label">PARTIE À NOTIFIER :</span>${e(c(o.destinationFacture||o.destinataire))}</div></div>
    <div class="reference-meta">
      <div class="reference-meta-row"><span><b>Doss ouvert le :</b>${e(p(t.dateCreation))}</span><span><b>N° Doss mère :</b>${e(t.dossierMere?.numeroDossier||t.numeroDossier)}</span><span><b>N° O.T :</b>${e(o.numeroOrdreTransit)}</span><span><b>Réf. Cli :</b>${e(o.numeroCommande)}</span></div>
      <div class="reference-meta-row"><span><b>Navire :</b>${e(r.navire?.nom||r.numeroVol)}</span><span><b>Voyage :</b>${e(r.numeroVoyage)}</span><span><b>Embarquement à :</b>${e(r.lieuBl||r.lieuLta)}</span><span><b>Arrivée le :</b>${e(p(r.dateArrivee))}</span></div>
      <div class="reference-meta-row"><span><b>Pays origine :</b>${e(t.paysOrigine?.nom)}</span><span><b>Pays dest :</b>${e(t.paysDestination?.nom)}</span><span><b>BL N° :</b>${e(r.numeroBl||r.numeroLta)}</span><span><b>Du :</b>${e(p(r.dateBl||r.dateLta))}</span></div>
      <div class="reference-meta-row"><span><b>Frêt payé à :</b>${e(t.lieuPaiementFret)}</span><span><b>Incoterm :</b>${e(t.inconterme?.abreviation)}</span><span><b>Phase :</b>${e(t.phaseDossier?.nom)}</span><span><b>Statut :</b>${e(t.statutDossier?.nom)}</span></div>
    </div>
    <div class="reference-checks"><span>Prépayé ${s("",t.estPrepaye)}</span><span>Assurance ${s("",o.estAssure)}</span><span>Groupage ${s("",t.estGroupage)}</span><span>Provision ${s("",t.estProvisionne)}</span><span>Montant (Ar) ${e(t.provisionDemandee)}</span></div>
    <div class="reference-section">INFORMATIONS SUR LES MARCHANDISES</div>
    <table class="reference-table"><thead><tr><th style="width:22%">Marques &amp; Numéros</th><th style="width:9%">Nbre CTN</th><th style="width:10%">Nbre Colis</th><th style="width:32%">Nature Marchandise</th><th style="width:12%">Poids brut (Kg)</th><th style="width:8%">Volume (m³)</th><th style="width:7%">Observation</th></tr></thead><tbody>${I.map(n=>`<tr><td>${e([n.marque,n.modele].filter(Boolean).join(" / "))}</td><td>${e(h.length||"")}</td><td>${e(n.quantite)}</td><td>${e(n.description||o.natureMarchandise?.nom)}</td><td>${e(n.poids)}</td><td>${e(n.volume)}</td><td></td></tr>`).join("")}<tr><td colspan="4" style="text-align:right">TOTAL</td><td>${e(l.reduce((n,a)=>n+Number(a.poids||0),0))}</td><td>${e(l.reduce((n,a)=>n+Number(a.volume||0),0))}</td><td></td></tr></tbody></table>
    <div class="reference-section">SUIVI DES OPÉRATIONS</div>
    <table class="reference-table reference-operations"><tbody>${Array.from({length:T},(n,a)=>`<tr>${y(v[a])}${y($[a])}</tr>`).join("")}</tbody></table>
    <div class="reference-section">SUIVI DES DOCUMENTS</div>
    <table class="reference-table"><thead><tr><th style="width:28%">DOCUMENT JOINT</th><th style="width:13%">Reçus le</th><th style="width:8%">Nb orig</th><th style="width:8%">Nb copie</th><th style="width:25%">DOCUMENT MANQUANT</th><th style="width:7%">Sous Miss</th><th style="width:11%">Relance</th></tr></thead><tbody>${Array.from({length:A},(n,a)=>{const f=b[a],w=x[a];return`<tr><td>${f?e(f.typeDocument?.nom):""}</td><td>${f?e(p(D.get(f.typeDocument?.id))):""}</td><td></td><td></td><td>${w?e(w.nom):""}</td><td></td><td></td></tr>`}).join("")}</tbody></table>
  </div>`}function C(t,{contrat:l=null,marchandises:b=[],instructions:m=[],documents:g=[]}={}){const d=t.expedition||{},h=String(d.modeTransport?.nom||d.modeTransport||t.typeProduction?.nom||"").toLowerCase(),o=t.client||{},r=t.destinationFacture||o;return`<div class="sheet">
    <div class="reference-ot-header"><div>${s("MARITIME",h.includes("maritime"))}${s("AÉRIEN",h.includes("aérien")||h.includes("aerien"))}${s("COMBINÉ",h.includes("combin"))}</div><div class="reference-title">ORDRE DE TRANSIT IMPORT<br><span style="font-size:12px;font-weight:400">À LA SOCIÉTÉ <span class="brand-inline"><img src="/logo/1000130987.webp" alt="">AUXIMAD</span><br>Agence ${e(t.agence?.nom)} · OT N° ${e(t.numeroOrdreTransit)}</span></div></div>
    <div class="reference-ot-body"><div class="tall"><span class="small-title">DONNEUR D’ORDRE</span><strong>${e(c(o))}</strong><br>${e(o.adresse)}<br>Tél : ${e(o.telephone)}</div><div class="tall">${s("Constats systématiques à chaque transfert de responsabilité",!1)}<br>${s("Constat en cas d’avaries ou pertes apparentes",!1)}<br>${s("Pas de constats",!1)}</div>
    <div class="tall"><span class="small-title">IMPORTATEUR</span><strong>${e(c(r))}</strong><br>${e(r.adresse)}<br>Tél : ${e(r.telephone)}</div><div class="tall"><div class="center">NOM DE L’EXPERT</div><hr>Acheminement jusqu’à destination finale<br>${s("Par train",!1)} &nbsp; ${s("Par camion",!1)} &nbsp; ${s("Par avion",!1)} &nbsp; ${s("Par nos soins",!1)}</div>
    <div class="tall"><span class="small-title">CAUTION</span>${e(t.caution)}</div><div class="tall">Bureau de dédouanement : <strong>${e(t.bureauDedouanement?.nom)}</strong><br><br>Livraison à<br><strong>${e(t.destinationFinale||c(t.destinataire))}</strong></div>
    <div><span class="small-title">B.L. ${e(d.numeroBl||d.numeroLta)}</span>Point d’émission : ${e(d.lieuBl||d.lieuLta)}</div><div>Facture à : ${e(c(r))}</div>
    <div>Navire(s) : ${e(d.navire?.nom||d.numeroVol)}<br>Attendu / Arrivée le : ${e(p(d.dateArrivee||d.datePrevue))}</div><div>C/L N° ${e(l?.numero)} &nbsp; du ${e(p(l?.dateCreation))}<br>Point d’émission : ${e(l?.lieuEmission)}</div>
    <div>Point de déchargement : ${e(d.lieuDechargement)}</div><div>Destination finale : ${e(t.destinationFinale)}</div></div>
    <table class="reference-table reference-ot-merch"><thead><tr><th style="width:55%">Marques et nombre des colis : Nature de la marchandise</th><th style="width:15%">Poids net</th><th style="width:15%">Poids brut</th><th style="width:15%">Volume</th></tr></thead><tbody>${(b.length?b:[{}]).map(i=>`<tr><td><b>${e(i.quantite)} ${e(i.uniteMesure?.nom)} — ${e(i.description||t.natureMarchandise?.nom)}</b><br>${e(i.marque)}</td><td>${e(i.poids)}</td><td>${e(i.poids)}</td><td>${e(i.volume)}</td></tr>`).join("")}</tbody></table>
    <div class="reference-ot-end"><div><u>DOCUMENTS JOINTS :</u><br>${g.length?g.map(i=>`${e(i.typeDocument?.nom)} ${s("",!0)}`).join("<br>"):"—"}</div><div></div></div>
    <div class="reference-section" style="text-align:left;border-bottom:0">INSTRUCTIONS PARTICULIÈRES</div><div class="notes">${m.length?m.map(i=>e(i.instruction||i.contenu||i.description||i.texte)).join("<br>"):"—"}</div>
    <div class="reference-ot-end"><div><b>Veuillez délivrer le BAD à AUXIMAD</b><br><br>Le donneur d’ordre confirme les renseignements et les documents transmis pour établir la déclaration en douane.</div><div>Fait à ${e(t.agence?.nom)} le ${e(p(t.dateCreation))}<br>Le donneur d’ordre<br><br><br><div class="center">(Signature)</div></div></div>
  </div>`}export{S as a,C as f,P as s};
