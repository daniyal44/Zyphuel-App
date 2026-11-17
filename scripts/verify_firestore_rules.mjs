#!/usr/bin/env node
/**
 * Verifies firestore.rules against the local Firestore emulator.
 *
 * The production bug was that `system_config` was entirely admin-only, so customers
 * could never read `system_config/fuel_rates` and fuel prices stuck at 0/stale on
 * both Android and iOS. These checks pin that contract:
 *
 *   - any signed-in user may READ system_config/fuel_rates + app_version
 *   - only an admin may WRITE system_config/fuel_rates
 *   - only an admin may READ/WRITE system_config/email_gateway
 *   - anonymous callers are denied everywhere in system_config
 *
 * Usage:
 *   npx firebase-tools emulators:exec --only firestore --project ai-420 \
 *     "node scripts/verify_firestore_rules.mjs"
 */

const BASE =
  process.env.FIRESTORE_EMULATOR_HOST
    ? `http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/ai-420/databases/(default)/documents`
    : 'http://127.0.0.1:8080/v1/projects/ai-420/databases/(default)/documents'

const b64url = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url')

/** The emulator accepts unsigned JWTs; it does not verify the signature. */
function tokenFor(claims) {
  return `${b64url({ alg: 'none', typ: 'JWT' })}.${b64url({ iat: 1700000000, exp: 4102444800, ...claims })}.`
}

const customerToken = tokenFor({ sub: 'uid_customer', email: 'customer@example.com' })
const adminToken = tokenFor({ sub: 'uid_admin', email: 'm.daniyalkhan490@gmail.com' })

const results = []

function record(name, expected, actual) {
  const pass = Array.isArray(expected) ? expected.includes(actual) : actual === expected
  results.push({ name, expected, actual, pass })
  console.log(`${pass ? 'PASS' : 'FAIL'}  ${name}  (expected ${expected}, got ${actual})`)
}

async function call(method, path, token, body) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  return res.status
}

const fuelRates = '/system_config/fuel_rates'
const appVersion = '/system_config/app_version'
const emailGateway = '/system_config/email_gateway'
const fuelBody = { fields: { petrol: { doubleValue: 328.00 }, diesel: { doubleValue: 358.00 }, highOctane: { doubleValue: 399.00 } } }

// Clean slate for the write tests (admin-only deletes are fine to attempt).
await call('DELETE', fuelRates, adminToken)

record('anonymous read fuel_rates is denied', 403, await call('GET', fuelRates))
record('customer read fuel_rates is allowed', [200, 404], await call('GET', fuelRates, customerToken))
record('customer create fuel_rates is denied', 403, await call('PATCH', fuelRates, customerToken, fuelBody))
record('admin create fuel_rates is allowed', 200, await call('PATCH', fuelRates, adminToken, fuelBody))
record('customer read fuel_rates after admin write is allowed', 200, await call('GET', fuelRates, customerToken))
record('customer read app_version is allowed', [200, 404], await call('GET', appVersion, customerToken))
record('customer write app_version is denied', 403, await call('PATCH', appVersion, customerToken, { fields: { latestVersionCode: { integerValue: '1' } } }))
record('anonymous read email_gateway is denied', 403, await call('GET', emailGateway))
record('customer read email_gateway is denied', 403, await call('GET', emailGateway, customerToken))
record('admin read email_gateway is allowed', [200, 404], await call('GET', emailGateway, adminToken))

// ----- Production‑data guard: ensure fuel_rates has positive values -----
async function callGetBody(method, path, token) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${BASE}${path}`, { method, headers })
  const body = await res.json().catch(() => ({}))
  return { status: res.status, body }
}

record('fuel_rates document readable by customer', [200, 404], await call('GET', fuelRates, customerToken))

// Check that when the document is present, all rate fields are > 0
;(async () => {
  const { status, body } = await callGetBody('GET', fuelRates, customerToken)
  const docPresent = status === 200
  record('fuel_rates document exists', 200, status)
  if (docPresent) {
    const petrolOk = (body.fields?.petrol?.doubleValue ?? 0) > 0
    const dieselOk  = (body.fields?.diesel?.doubleValue  ?? 0) > 0
    const octaneOk  = (body.fields?.highOctane?.doubleValue ?? 0) > 0
    record('fuel_rates petrol > 0', true, petrolOk)
    record('fuel_rates diesel > 0', true, dieselOk)
    record('fuel_rates highOctane > 0', true, octaneOk)
  }
})()

const failed = results.filter((r) => !r.pass)
console.log(`\n${results.length - failed.length}/${results.length} rule checks passed`)
if (failed.length) {
  console.error(`\n${failed.length} Firestore rule check(s) FAILED`)
  process.exit(1)
}
console.log('firestore.rules verified: fuel rates readable by all signed-in devices, admin-only writes, and non‑zero values present.')
