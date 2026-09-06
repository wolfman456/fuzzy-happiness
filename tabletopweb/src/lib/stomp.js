import { Client } from '@stomp/stompjs'
import { loadStoredSession } from '../auth/authStore'

const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export function stompBrokerUrl() {
  const stored = loadStoredSession()
  const base = API_BASE.replace(/^http/, 'ws')
  return `${base}/ws?token=${encodeURIComponent(stored?.token ?? '')}`
}

export function createRealtimeClient({ sessionId, onSnapshot, onEvent, onError }) {
  const client = new Client({
    brokerURL: stompBrokerUrl(),
    reconnectDelay: 3000,
    heartbeatIncoming: 0,
    heartbeatOutgoing: 0,
    onConnect() {
      client.subscribe(`/app/sessions/${sessionId}`, (frame) => {
        const snapshot = parseJson(frame.body)
        if (snapshot) onSnapshot?.(snapshot)
      })
      client.subscribe(`/topic/sessions/${sessionId}`, (frame) => {
        const event = parseJson(frame.body)
        if (event) onEvent?.(event)
      })
    },
    onStompError(frame) {
      onError?.(frame.headers.message)
    },
    onWebSocketError() {
      onError?.('Connection lost')
    },
  })
  return {
    connect: () => client.activate(),
    disconnect: () => client.deactivate(),
    sendChat: (text) =>
      client.publish({
        destination: `/app/sessions/${sessionId}/chat`,
        body: JSON.stringify({ text }),
      }),
  }
}

function parseJson(text) {
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}