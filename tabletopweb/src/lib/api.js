const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

let authToken = null
let unauthorizedHandler = null

export function setAuthToken(token) {
  authToken = token
}

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (authToken) headers.Authorization = `Bearer ${authToken}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401 && authToken) {
    setAuthToken(null)
    unauthorizedHandler?.();
  }

  const text = await response.text()

  if (!response.ok) {
    let message = `Request failed (${response.status})`
    try {
      const data = JSON.parse(text)
      if (data && typeof data.message === 'string') message = data.message
    } catch {
      // keep the fallback message
    }
    throw new ApiError(response.status, message)
  }

  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}