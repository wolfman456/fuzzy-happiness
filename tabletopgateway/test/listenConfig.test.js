import { describe, expect, it } from 'vitest';
import { resolveListenConfig } from '../src/listenConfig.js';

describe('resolveListenConfig', () => {
  it('defaults to loopback + 3001 in dev (no PORT)', () => {
    expect(resolveListenConfig({})).toEqual({ port: 3001, host: '127.0.0.1' });
  });

  it('uses GATEWAY_PORT when PORT is absent', () => {
    expect(resolveListenConfig({ GATEWAY_PORT: '4100' })).toEqual({ port: 4100, host: '127.0.0.1' });
  });

  it('PORT wins over GATEWAY_PORT (Railway/container)', () => {
    expect(resolveListenConfig({ PORT: '8080', GATEWAY_PORT: '3001' })).toEqual({ port: 8080, host: '0.0.0.0' });
  });

  it('binds 0.0.0.0 when PORT is present (container)', () => {
    expect(resolveListenConfig({ PORT: '3001' }).host).toBe('0.0.0.0');
  });

  it('GATEWAY_HOST overrides the automatic host', () => {
    expect(resolveListenConfig({ PORT: '3001', GATEWAY_HOST: '10.0.0.5' })).toEqual({ port: 3001, host: '10.0.0.5' });
  });
});