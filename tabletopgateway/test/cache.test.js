import { describe, expect, it } from 'vitest';
import { createTtlCache } from '../src/cache.js';

describe('createTtlCache', () => {
  it('stores and returns a value', () => {
    const cache = createTtlCache();
    cache.set('a', { value: 1 });
    expect(cache.get('a')).toEqual({ value: 1 });
    expect(cache.size()).toBe(1);
  });

  it('reports freshness against a moving clock', () => {
    let now = 0;
    const cache = createTtlCache(() => now);
    cache.set('k', 'v');
    expect(cache.isFresh('k', 100)).toBe(true);
    now = 100;
    expect(cache.isFresh('k', 100)).toBe(false);
    expect(cache.hasExpired('k', 100)).toBe(true);
  });

  it('keeps expired values available for stale fallback', () => {
    let now = 0;
    const cache = createTtlCache(() => now);
    cache.set('k', 'stale-body');
    now = 1000;
    expect(cache.hasExpired('k', 100)).toBe(true);
    expect(cache.get('k')).toBe('stale-body');
  });

  it('deletes and clears', () => {
    const cache = createTtlCache();
    cache.set('a', 1);
    cache.set('b', 2);
    cache.delete('a');
    expect(cache.get('a')).toBeUndefined();
    cache.clear();
    expect(cache.size()).toBe(0);
  });
});