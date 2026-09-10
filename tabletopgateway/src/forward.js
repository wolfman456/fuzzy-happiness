import { randomUUID } from 'node:crypto';
import { assertPublicHost } from './ssrf.js';

class UpstreamError extends Error {}

function respondError(res, status, message) {
  res.status(status).json({ status, message });
}

function buildCacheKey(route, collection, index, subresource, params) {
  const paramKey = Object.keys(params)
    .sort()
    .map((k) => `${k}=${params[k]}`)
    .join('&');
  const suffix = index ? `/${index}${subresource ? `/${subresource}` : ''}` : '';
  return `${route.name}:${collection}${suffix}${paramKey ? `?${paramKey}` : ''}`;
}

function filterQuery(route, query, res) {
  const params = {};
  for (const [key, value] of Object.entries(query)) {
    if (!route.allowQuery.has(key)) {
      respondError(res, 403, `query param not allowed: ${key}`);
      return undefined;
    }
    if (typeof value !== 'string' || value.length === 0) {
      respondError(res, 403, `query param must be a non-empty string: ${key}`);
      return undefined;
    }
    params[key] = value;
  }
  return params;
}

async function readCapped(response, maxBytes) {
  if (!response.body) return '';
  const reader = response.body.getReader();
  const chunks = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > maxBytes) {
      await reader.cancel().catch(() => {});
      throw new UpstreamError('upstream response exceeds size limit');
    }
    chunks.push(value);
  }
  return Buffer.concat(chunks).toString('utf8');
}

async function fetchWithRetry(url, { headers, timeoutMs, maxBytes, fetchFn }) {
  let lastError;
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(new Error('upstream timeout')), timeoutMs);
    try {
      const response = await fetchFn(url, { headers, signal: controller.signal });
      if (response.status >= 500) {
        lastError = new UpstreamError(`upstream returned status ${response.status}`);
        continue;
      }
      const body = await readCapped(response, maxBytes);
      return { status: response.status, contentType: response.headers.get('content-type'), body };
    } catch (error) {
      lastError = error;
      if (error instanceof UpstreamError) throw error;
    } finally {
      clearTimeout(timer);
    }
  }
  throw lastError ?? new UpstreamError('upstream unreachable');
}

export function createForwarder({ route, cache, fetchFn = fetch, dnsLookup }) {
  return async function forward(req, res) {
    if (req.method !== 'GET') {
      return respondError(res, 403, 'only GET is allowed');
    }

    const correlationId = req.get('x-correlation-id') || randomUUID();
    res.set('x-correlation-id', correlationId);

    const segments = req.path.split('/').filter(Boolean);
    const [collection, index, subresource] = segments;
    if (!collection || !route.collections.includes(collection)) {
      return respondError(res, 403, `path not allowed: ${req.path}`);
    }
    if (segments.length > 3) {
      return respondError(res, 403, `path not allowed: ${req.path}`);
    }
    if (subresource !== undefined
        && !(route.subresources?.[collection]?.includes(subresource))) {
      return respondError(res, 403, `path not allowed: ${req.path}`);
    }
    if (index !== undefined && !route.indexPattern.test(index)) {
      return respondError(res, 403, `path not allowed: ${req.path}`);
    }

    const params = filterQuery(route, req.query, res);
    if (params === undefined) return undefined;

    const key = buildCacheKey(route, collection, index, subresource, params);
    const ttlMs = index !== undefined ? route.cacheTtlMs.detail : route.cacheTtlMs.list;

    if (cache.isFresh(key, ttlMs)) {
      res.set('x-gateway-cache', 'HIT');
      return res.json(JSON.parse(cache.get(key)));
    }

    const url = new URL(route.target);
    url.pathname = `${url.pathname.replace(/\/$/, '')}/${collection}`
      + `${index ? `/${index}` : ''}${subresource ? `/${subresource}` : ''}`;
    for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);

    try {
      await assertPublicHost(url.hostname, dnsLookup);
    } catch {
      return respondError(res, 502, 'upstream host rejected');
    }

    try {
      const upstream = await fetchWithRetry(url.href, {
        headers: {
          accept: 'application/json',
          'x-correlation-id': correlationId,
          'user-agent': 'tabletopgateway',
        },
        timeoutMs: route.timeoutMs,
        maxBytes: route.maxBytes,
        fetchFn,
      });
      if (upstream.status >= 200 && upstream.status < 300) cache.set(key, upstream.body);
      res.set('x-gateway-cache', 'MISS');
      if (upstream.contentType) res.set('content-type', upstream.contentType);
      return res.status(upstream.status).send(upstream.body);
    } catch {
      if (cache.hasExpired(key, ttlMs)) {
        res.set('x-gateway-cache', 'STALE');
        return res.json(JSON.parse(cache.get(key)));
      }
      return respondError(res, 502, `upstream unavailable: ${route.name}`);
    }
  };
}