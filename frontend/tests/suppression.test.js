import test from 'node:test'
import assert from 'node:assert/strict'
import { requeteSuppression, messageSuppression } from '../src/services/SuppressionService.js'

const impact = { confirmation: 'impact-1', suppressions: [{objet:'ordres de transit',nombre:1}], liensRetires:[{objet:'dossiers',nombre:2}], dossiersClotures:2 }
function confirmationRequired(value = impact) {
    return { response:{status:409,data:{code:'CONFIRMATION_SUPPRESSION',impact:value}} }
}

test('the popup describes deletion, preserved records and dossier closures', () => {
    const message = messageSuppression(impact)
    assert.match(message,/Éléments supprimés/)
    assert.match(message,/éléments conservés/)
    assert.match(message,/2 dossier\(s\) seront automatiquement clôturés/)
})

test('no confirmed request is sent when the user cancels', async () => {
    let requests = 0
    global.window = { confirm: () => false }
    const client = { request: async () => { requests++; throw confirmationRequired() } }
    await assert.rejects(requeteSuppression(client,{method:'delete',url:'/ot/1'}), error => error.code === 'ERR_CANCELED')
    assert.equal(requests,1)
})

test('confirmation sends the exact impact token and preserves auth and parameters', async () => {
    const requests = []
    global.window = { confirm: () => true }
    const client = { request: async config => { requests.push(config); if(requests.length===1) throw confirmationRequired(); return {data:'deleted'} } }
    const result = await requeteSuppression(client,{method:'delete',url:'/conteneurs/1?dossierId=2',headers:{Authorization:'Bearer test'}})
    assert.equal(result.data,'deleted')
    assert.equal(requests[0].headers['X-Confirmation-Suppression'],undefined)
    assert.equal(requests[1].headers['X-Confirmation-Suppression'],'impact-1')
    assert.equal(requests[1].headers.Authorization,'Bearer test')
    assert.equal(requests[1].url,'/conteneurs/1?dossierId=2')
})

test('a changed impact requires a new user decision, with cancellation leaving the request unconfirmed', async () => {
    let prompts = 0, requests = 0
    global.window = { confirm: () => ++prompts === 1 }
    const client = { request: async () => { requests++; throw confirmationRequired({...impact,confirmation:`impact-${requests}`}) } }
    await assert.rejects(requeteSuppression(client,{method:'delete',url:'/ot/1'}),error=>error.code==='ERR_CANCELED')
    assert.equal(prompts,2)
    assert.equal(requests,2)
})

test('unrelated errors are surfaced without another popup or retry', async () => {
    let prompts = 0, requests = 0
    global.window = { confirm: () => { prompts++; return true } }
    const error = {response:{status:403,data:{message:'Forbidden'}}}
    const client = {request:async()=>{requests++;throw error}}
    await assert.rejects(requeteSuppression(client,{method:'delete',url:'/ot/1'}),e=>e===error)
    assert.equal(prompts,0); assert.equal(requests,1)
})

test('batch requests retain their method and selected IDs on confirmation', async () => {
    const requests = []
    global.window = {confirm:()=>true}
    const client = {request:async config=>{requests.push(config);if(requests.length===1)throw confirmationRequired();return {data:'ok'}}}
    await requeteSuppression(client,{method:'post',url:'/operations-dossier/batch-delete',data:[1,2]})
    assert.equal(requests[1].method,'post');assert.deepEqual(requests[1].data,[1,2])
})