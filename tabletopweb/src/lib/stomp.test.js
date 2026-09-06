import { beforeEach, describe, expect, it, vi } from 'vitest'

const stompClient = vi.hoisted(() => {
  const instances = []
  class Client {
    constructor(config) {
      Client.lastConfig = config
      Client.instances = instances
      this.subscribe = vi.fn()
      this.publish = vi.fn()
      this.activate = vi.fn()
      this.deactivate = vi.fn()
      instances.push(this)
    }
  }
  return { Client }
})

vi.mock('@stomp/stompjs', () => ({ Client: stompClient.Client }))

vi.mock('../auth/authStore', () => ({
  loadStoredSession: vi.fn(() => ({ token: 'jwt-abc' })),
}))

import { createRealtimeClient, stompBrokerUrl } from './stomp'

const onSnapshot = vi.fn()
const onEvent = vi.fn()
const onPrivateRoll = vi.fn()
const onError = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  stompClient.Client.lastConfig = null
  stompClient.Client.instances = []
})

describe('stompBrokerUrl', () => {
  it('builds a ws:// broker URL carrying the auth token', () => {
    const url = stompBrokerUrl()
    expect(url).toMatch(/^ws:\/\/localhost:8080\/ws\?token=jwt-abc$/)
  })
})

describe('createRealtimeClient', () => {
  function lastClient() {
    return stompClient.Client.instances.at(-1)
  }

  it('subscribes to snapshot and topic destinations on connect', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).connect()
    const config = stompClient.Client.lastConfig
    expect(config.brokerURL).toBe(stompBrokerUrl())
    expect(config.onConnect).toBeTypeOf('function')

    config.onConnect()

    const subscriptions = lastClient().subscribe.mock.calls.map(([dest]) => dest)
    expect(subscriptions).toContain('/app/sessions/7')
    expect(subscriptions).toContain('/topic/sessions/7')
    expect(subscriptions).toContain('/user/queue/dice')
    expect(lastClient().activate).toHaveBeenCalledTimes(1)
  })

  it('forwards user-queue dice frames to onPrivateRoll', () => {
    createRealtimeClient({
      sessionId: 7,
      onSnapshot,
      onEvent,
      onPrivateRoll,
      onError,
    }).connect()
    const config = stompClient.Client.lastConfig
    config.onConnect()

    const [, queueHandler] = lastClient().subscribe.mock.calls.find(
      ([d]) => d === '/user/queue/dice',
    )
    queueHandler({ body: '{"type":"DICE","payload":{"rollId":"abc","total":14,"hidden":true}}' })
    expect(onPrivateRoll).toHaveBeenCalledWith({
      type: 'DICE',
      payload: { rollId: 'abc', total: 14, hidden: true },
    })
  })

  it('parses snapshot and event payloads into the callbacks', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).connect()
    const config = stompClient.Client.lastConfig
    config.onConnect()

    const [snapshotDest, snapshotHandler] = lastClient().subscribe.mock.calls.find(([d]) => d === '/app/sessions/7')
    snapshotHandler({ body: '{"id":7,"name":"Tower"}' })
    expect(snapshotDest).toBe('/app/sessions/7')
    expect(onSnapshot).toHaveBeenCalledWith({ id: 7, name: 'Tower' })

    const [, topicHandler] = lastClient().subscribe.mock.calls.find(([d]) => d === '/topic/sessions/7')
    topicHandler({ body: '{"type":"CHAT","payload":{"text":"hi"}}' })
    expect(onEvent).toHaveBeenCalledWith({ type: 'CHAT', payload: { text: 'hi' } })
  })

  it('publishes chat to the session destination', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).sendChat('Hello table!')

    expect(lastClient().publish).toHaveBeenCalledWith({
      destination: '/app/sessions/7/chat',
      body: '{"text":"Hello table!"}',
    })
  })

  it('disconnects via deactivate', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).disconnect()

    expect(lastClient().deactivate).toHaveBeenCalledTimes(1)
  })

  it('forwards STOMP errors to onError', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).connect()
    const config = stompClient.Client.lastConfig

    config.onStompError({ headers: { message: 'not a participant' } })
    expect(onError).toHaveBeenCalledWith('not a participant')
  })

  it('reports web socket failures as connection loss', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).connect()
    const config = stompClient.Client.lastConfig

    config.onWebSocketError()
    expect(onError).toHaveBeenCalledWith('Connection lost')
  })

  it('tolerates malformed JSON in frames', () => {
    createRealtimeClient({ sessionId: 7, onSnapshot, onEvent, onError }).connect()
    const config = stompClient.Client.lastConfig
    config.onConnect()

    const [, snapshotHandler] = lastClient().subscribe.mock.calls.find(([d]) => d === '/app/sessions/7')
    snapshotHandler({ body: 'not-json' })
    expect(onSnapshot).not.toHaveBeenCalled()
  })
})