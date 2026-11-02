import test from 'node:test'
import assert from 'node:assert/strict'
import { construireEvolutionMensuelle } from '../src/utils/dashboard-chart.js'

test('point heights and axis graduations use the same zero-based scale', () => {
  const chart = construireEvolutionMensuelle([
    { mois: '2026-10', valeur: '50' },
    { mois: '2026-11', valeur: 100 },
    { mois: '2026-12', valeur: 0 },
  ])
  const [half, full, zero] = chart.points
  assert.equal(zero.y, chart.bas)
  assert.equal(full.y, chart.haut)
  assert.equal(half.y, (chart.haut + chart.bas) / 2)
  assert.equal(full.y, chart.graduations.find(tick => tick.valeur === 100).y)
  assert.equal(half.y, chart.graduations.find(tick => tick.valeur === 50).y)
  assert.deepEqual(chart.points.map(point => point.labelCourt), ['10/26', '11/26', '12/26'])
})

test('small counts and zero-only months have integer graduations and finite coordinates', () => {
  for (const valeur of [0, 1, 2, 3, 7, 137, 10001]) {
    const chart = construireEvolutionMensuelle([{ mois: '2026-10', valeur }])
    assert.ok(chart.plafond >= valeur)
    assert.ok(chart.graduations.every(tick => Number.isInteger(tick.valeur)))
    assert.ok(chart.points.every(point => Number.isFinite(point.x) && Number.isFinite(point.y)))
    assert.equal(chart.points[0].x, (chart.gauche + chart.droite) / 2)
    if (valeur === 0) assert.equal(chart.points[0].y, chart.bas)
  }
})

test('months remain chronological across a year boundary', () => {
  const chart = construireEvolutionMensuelle([
    { mois: '2026-01', valeur: 8 }, { mois: '2025-12', valeur: 4 },
  ])
  assert.deepEqual(chart.points.map(point => point.mois), ['2025-12', '2026-01'])
  assert.deepEqual(chart.points.map(point => point.labelCourt), ['12/25', '01/26'])
  assert.ok(chart.points[0].x < chart.points[1].x)
  assert.ok(chart.points[0].y > chart.points[1].y)
})

test('an empty response produces no curve', () => {
  const chart = construireEvolutionMensuelle([])
  assert.deepEqual(chart.points, [])
  assert.equal(chart.ligne, '')
  assert.equal(chart.aire, '')
})
