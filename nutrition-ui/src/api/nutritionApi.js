const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

function getCsrfToken() {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : ''
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE_URL}${path}`, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-XSRF-TOKEN': getCsrfToken(),
      ...options.headers,
    },
    ...options,
  })
  const json = await res.json()
  if (!res.ok || json.status === 'error') {
    throw new Error(json.message || `Request failed: ${res.status}`)
  }
  return json.data
}

// ── Auth ─────────────────────────────────────────────────────────────────────

export async function getMe() {
  const res = await fetch(`${BASE_URL}/api/me`, { credentials: 'include' })
  if (!res.ok) throw new Error('Failed to fetch user info')
  return res.json()
}

export function signIn() {
  window.location.href = `${BASE_URL}/oauth2/authorization/google`
}

export function signOut() {
  window.location.href = `${BASE_URL}/logout`
}

// ── Weeks ────────────────────────────────────────────────────────────────────

export function getWeeks() {
  return request('/api/weeks')
}

export function createWeek(startDate, people = 2) {
  return request('/api/weeks', {
    method: 'POST',
    body: JSON.stringify({ startDate, people }),
  })
}

// ── Items ────────────────────────────────────────────────────────────────────

/**
 * @param {string} weekId
 * @param {{ name, category, servingSizeG, servingsPerWeek, price }[]} items
 * @param {number|null} receiptTotal
 */
export function saveItems(weekId, items, receiptTotal = null) {
  return request('/api/items', {
    method: 'POST',
    body: JSON.stringify({ weekId, items, receiptTotal }),
  })
}

// ── Receipt upload ────────────────────────────────────────────────────────────

/**
 * Upload a receipt image; returns ExtractedGroceryReceipt.
 */
export async function extractReceipt(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`${BASE_URL}/api/receipt/extract`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-XSRF-TOKEN': getCsrfToken() },
    body: form,
  })
  if (!res.ok) throw new Error(`Extract failed: ${res.status}`)
  return res.json()
}

/**
 * Confirm reviewed items; saves to DB and triggers USDA lookup.
 * @param {string} weekId
 * @param {{ name, category, servingSizeG, servingsPerWeek, price }[]} items
 * @param {number|null} total
 */
export async function confirmReceipt(weekId, items, total) {
  const form = new FormData()
  form.append('weekId', weekId)
  form.append('data', JSON.stringify(items))
  if (total != null) form.append('total', String(total))
  const res = await fetch(`${BASE_URL}/api/receipt/confirm`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'X-XSRF-TOKEN': getCsrfToken() },
    body: form,
  })
  if (!res.ok) throw new Error(`Confirm failed: ${res.status}`)
}

// ── Nutrition ────────────────────────────────────────────────────────────────

export function getNutritionReport(weekId) {
  return request(`/api/nutrition/${weekId}`)
}

export function exportToSheets(weekId, weekStart) {
  const params = weekStart ? `?weekStart=${weekStart}` : ''
  return request(`/api/nutrition/${weekId}/export${params}`, { method: 'POST' })
}
