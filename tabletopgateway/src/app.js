import express from 'express';
import { defaultRoutes } from './routes.js';
import { createTtlCache } from './cache.js';
import { createForwarder } from './forward.js';

export function defaultLogger(entry) {
  console.log(JSON.stringify(entry));
}

export function createApp(options = {}) {
  const token = options.token ?? process.env.GATEWAY_TOKEN ?? 'dev-gateway-token';
  const routes = options.routes ?? defaultRoutes();
  const now = options.now ?? Date.now;
  const cache = options.cache ?? createTtlCache(now);
  const fetchFn = options.fetchFn;
  const dnsLookup = options.dnsLookup;
  const logger = options.logger ?? defaultLogger;

  const app = express();
  app.disable('x-powered-by');

  app.use((req, res, next) => {
    const startedAt = Date.now();
    res.on('finish', () => {
      const correlationId = res.getHeader('x-correlation-id');
      const cacheStatus = res.getHeader('x-gateway-cache');
      logger({
        ts: new Date().toISOString(),
        method: req.method,
        path: req.originalUrl,
        status: res.statusCode,
        corr: correlationId ?? '',
        cache: cacheStatus ?? '',
        ms: Date.now() - startedAt,
      });
    });
    next();
  });

  app.get('/health', (req, res) => res.status(200).json({ status: 'ok' }));

  app.use((req, res, next) => {
    if (req.path === '/health') return next();
    if (req.get('x-gateway-token') !== token) {
      return res.status(401).json({ status: 401, message: 'missing or invalid gateway token' });
    }
    return next();
  });

  for (const route of routes) {
    app.use(route.path ?? `/${route.name}`, createForwarder({ route, cache, fetchFn, dnsLookup }));
  }

  app.use((req, res) => res.status(403).json({ status: 403, message: `no route for ${req.method} ${req.path}` }));

  return app;
}