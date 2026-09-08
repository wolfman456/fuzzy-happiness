import { describe, expect, it } from 'vitest';
import { isPrivateAddress, isPrivateIpv4, isPrivateIpv6 } from '../src/ssrf.js';

describe('isPrivateIpv4', () => {
  it('flags private and special-purpose ranges', () => {
    for (const ip of [
      '10.0.0.1',
      '172.16.0.1',
      '172.31.255.255',
      '192.168.1.1',
      '127.0.0.1',
      '169.254.169.254',
      '0.0.0.0',
      '100.64.0.1',
      '224.0.0.1',
      '240.0.0.1',
    ]) {
      expect(isPrivateIpv4(ip), ip).toBe(true);
    }
  });

  it('treats garbage and out-of-range octets as private', () => {
    expect(isPrivateIpv4('999.1.1.1')).toBe(true);
    expect(isPrivateIpv4('not-an-ip')).toBe(true);
  });

  it('accepts public addresses', () => {
    expect(isPrivateIpv4('8.8.8.8')).toBe(false);
    expect(isPrivateIpv4('1.1.1.1')).toBe(false);
    expect(isPrivateIpv4('93.184.216.34')).toBe(false);
  });
});

describe('isPrivateIpv6', () => {
  it('flags private, link-local, multicast and loopback ranges', () => {
    for (const ip of [
      '::1',
      '::',
      'fc00::1',
      'fdf8::2',
      'fe80::1',
      'ff02::1',
      '2001:db8::1',
      '64:ff9b::1',
      '::ffff:127.0.0.1',
      '::ffff:10.0.0.1',
    ]) {
      expect(isPrivateIpv6(ip), ip).toBe(true);
    }
  });

  it('accepts public global unicast addresses', () => {
    for (const ip of ['2001:4860:4860::8888', '2606:4700:4700::1111', '2a00:1450:4001::111']) {
      expect(isPrivateIpv6(ip), ip).toBe(false);
    }
  });

  it('flags invalid input as private', () => {
    expect(isPrivateIpv6('garbage')).toBe(true);
  });
});

describe('isPrivateAddress', () => {
  it('dispatches on address family', () => {
    expect(isPrivateAddress('8.8.8.8')).toBe(false);
    expect(isPrivateAddress('2001:4860:4860::8888')).toBe(false);
    expect(isPrivateAddress('172.16.0.4')).toBe(true);
    expect(isPrivateAddress('something-else')).toBe(true);
  });
});