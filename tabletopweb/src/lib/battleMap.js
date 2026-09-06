import { api } from './api'

export function getMap(sessionId) {
  return api(`/api/sessions/${sessionId}/map`)
}

export function createMap(sessionId, body = {}) {
  return api(`/api/sessions/${sessionId}/map`, { method: 'POST', body })
}

export function updateMap(sessionId, body) {
  return api(`/api/sessions/${sessionId}/map`, { method: 'PATCH', body })
}

export function addToken(sessionId, body) {
  return api(`/api/sessions/${sessionId}/map/tokens`, { method: 'POST', body })
}

export function updateToken(sessionId, tokenId, body) {
  return api(`/api/sessions/${sessionId}/map/tokens/${tokenId}`, {
    method: 'PATCH',
    body,
  })
}

export function removeToken(sessionId, tokenId) {
  return api(`/api/sessions/${sessionId}/map/tokens/${tokenId}`, {
    method: 'DELETE',
  })
}

export function moveToken(sessionId, tokenId, body) {
  return api(`/api/sessions/${sessionId}/map/tokens/${tokenId}/move`, {
    method: 'POST',
    body,
  })
}

export function turnCommand(sessionId, body) {
  return api(`/api/sessions/${sessionId}/map/turn`, { method: 'POST', body })
}