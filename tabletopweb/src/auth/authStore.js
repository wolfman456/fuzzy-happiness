const STORAGE_KEY = 'tt.auth'

export function loadStoredSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (parsed && typeof parsed.token === 'string' && parsed.token) return parsed
    return null
  } catch {
    return null
  }
}

export function saveSession(token, user) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ token, user }))
}

export function clearSession() {
  localStorage.removeItem(STORAGE_KEY)
}