import { describe, expect, it, vi } from 'vitest';
import request from 'supertest';
import { createApp } from '../src/app.js';
import { defaultRoutes } from '../src/routes.js';

const TOKEN = 'test-gateway-token';

function okResponse(body) {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: { 'content-type': 'application/json' },
  });
}

function makeApp(overrides = {}) {
  let clock = 0;
  const fetchFn = overrides.fetchFn ?? (async () => okResponse({ name: 'races' }));
  const dnsLookup = overrides.dnsLookup ?? (async () => [{ address: '8.8.8.8' }]);
  return {
    app: createApp({
      token: TOKEN,
      now: () => clock,
      fetchFn,
      dnsLookup,
      ...overrides.options,
    }),
    setClock: (value) => {
      clock = value;
    },
  };
}

describe('createApp', () => {
  it('serves /health without a token', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: 'ok' });
  });

  it('rejects requests without the gateway token', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/api/srd/races');
    expect(res.status).toBe(401);
    expect(res.body.status).toBe(401);
  });

  it('rejects requests with the wrong token', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', 'wrong');
    expect(res.status).toBe(401);
  });

  it('returns 403 for unknown routes', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/api/monsters').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(403);
    expect(res.body.status).toBe(403);
  });

  it('forwards GET /srd/{collection} to the SRD target', async () => {
    const fetchFn = vi.fn(async (url) => {
      expect(url).toBe('https://www.dnd5eapi.co/api/2014/races');
      return okResponse({ count: 1 });
    });
    const { app } = makeApp({ fetchFn });
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ count: 1 });
    expect(res.headers['x-gateway-cache']).toBe('MISS');
    expect(fetchFn).toHaveBeenCalledTimes(1);
  });

  it('forwards GET /srd/{collection}/{index} with allowed query params only', async () => {
    const fetchFn = vi.fn(async (url) => {
      const parsed = new URL(url);
      expect(parsed.origin).toBe('https://www.dnd5eapi.co');
      expect(parsed.pathname).toBe('/api/2014/spells/fireball');
      expect(parsed.searchParams.get('school')).toBe('evocation');
      expect(parsed.searchParams.get('level')).toBe('3');
      expect(parsed.searchParams.get('rogue')).toBeNull();
      expect(parsed.searchParams.get('extra')).toBeNull();
      // should be dropped entirely (no undefined params in cache/timeouts)
      expect(parsed.searchParams.toString()).not.toContain('rogue');
      return okResponse({ name: 'Fireball' });
    });
    const { app } = makeApp({ fetchFn });
    const res = await request(app)
      .get('/api/srd/spells/fireball?school=evocation&level=3')
      .set('x-gateway-token', TOKEN);
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ name: 'Fireball' });
  });

  it('rejects disallowed query params with 403', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/api/srd/spells?rogue=1').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(403);
    expect(res.body.status).toBe(403);
  });

  it('rejects unknown collections with 403', async () => {
    const { app } = makeApp();
    const res = await request(app).get('/api/srd/not-a-collection').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(403);
  });

  it('rejects too-deep paths and invalid index tokens with 403', async () => {
    const { app } = makeApp();
    expect((await request(app).get('/api/srd/spells/fireball/extra').set('x-gateway-token', TOKEN)).status).toBe(403);
    expect((await request(app).get('/api/srd/spells/../api').set('x-gateway-token', TOKEN)).status).toBe(403);
  });

  it('rejects non-GET methods with 403', async () => {
    const { app } = makeApp();
    const res = await request(app).post('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(403);
  });

  it('serves a cached copy on repeated requests (HIT)', async () => {
    const fetchFn = vi.fn(async () => okResponse({ count: 1 }));
    const { app } = makeApp({ fetchFn });
    await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    await request(app).get('/api/srd/races/elf').set('x-gateway-token', TOKEN);
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(200);
    expect(res.headers['x-gateway-cache']).toBe('HIT');
    expect(fetchFn).toHaveBeenCalledTimes(2);
  });

  it('falls back to a stale cached copy when upstream fails (STALE)', async () => {
    let fail = false;
    const fetchFn = vi.fn(async () => {
      if (fail) throw new Error('upstream down');
      return okResponse({ name: 'elf' });
    });
    const { app, setClock } = makeApp({ fetchFn });
    await request(app).get('/api/srd/races/elf').set('x-gateway-token', TOKEN);
    setClock(60 * 60 * 1000 + 1);
    fail = true;
    const res = await request(app).get('/api/srd/races/elf').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(200);
    expect(res.headers['x-gateway-cache']).toBe('STALE');
    expect(res.body).toEqual({ name: 'elf' });
  });

  it('returns 502 when upstream is unreachable and no cache exists', async () => {
    const fetchFn = vi.fn(async () => {
      throw new Error('ECONNREFUSED');
    });
    const { app } = makeApp({ fetchFn });
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(502);
    expect(res.body.status).toBe(502);
  });

  it('returns 502 after persistent upstream 5xx', async () => {
    const fetchFn = vi.fn(async () => new Response('boom', { status: 503 }));
    const { app } = makeApp({ fetchFn });
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(502);
    expect(res.body.status).toBe(502);
  });

  it('blocks hosts that resolve to private ranges with 502', async () => {
    const { app } = makeApp({ dnsLookup: async () => [{ address: '10.0.0.1' }] });
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(502);
  });

  it('enforces the per-route response size cap', async () => {
    const routes = defaultRoutes();
    routes[0].maxBytes = 16;
    const fetchFn = vi.fn(async () => okResponse({ very: 'long', payload: 'that exceeds the cap' }));
    const { app } = makeApp({ options: { routes }, fetchFn });
    const res = await request(app).get('/api/srd/races').set('x-gateway-token', TOKEN);
    expect(res.status).toBe(502);
  });
});