import { lookup as defaultLookup } from 'node:dns/promises';

function ipv4ToNumber(v4) {
  return v4.split('.').reduce((acc, part, i) => acc | (Number(part) << (8 * (3 - i))), 0) >>> 0;
}

const PRIVATE_IPV4_RANGES = [
  [ipv4ToNumber('0.0.0.0'), ipv4ToNumber('0.255.255.255')],
  [ipv4ToNumber('10.0.0.0'), ipv4ToNumber('10.255.255.255')],
  [ipv4ToNumber('100.64.0.0'), ipv4ToNumber('100.127.255.255')],
  [ipv4ToNumber('127.0.0.0'), ipv4ToNumber('127.255.255.255')],
  [ipv4ToNumber('169.254.0.0'), ipv4ToNumber('169.254.255.255')],
  [ipv4ToNumber('172.16.0.0'), ipv4ToNumber('172.31.255.255')],
  [ipv4ToNumber('192.168.0.0'), ipv4ToNumber('192.168.255.255')],
  [ipv4ToNumber('198.18.0.0'), ipv4ToNumber('198.19.255.255')],
  [ipv4ToNumber('224.0.0.0'), ipv4ToNumber('239.255.255.255')],
  [ipv4ToNumber('240.0.0.0'), ipv4ToNumber('255.255.255.255')],
];

export function isPrivateIpv4(address) {
  const octets = address.split('.').map(Number);
  if (octets.length !== 4 || octets.some((o) => o < 0 || o > 255)) return true;
  const value = ipv4ToNumber(address);
  return PRIVATE_IPV4_RANGES.some(([start, end]) => value >= start && value <= end);
}

function hexToBigInt(hex) {
  return BigInt(hex ? `0x${hex}` : 0);
}

function expandGroup(group) {
  if (group.includes('.')) {
    const n = ipv4ToNumber(group);
    return [((n >> 16) & 0xffff).toString(16), (n & 0xffff).toString(16)];
  }
  return [group];
}

function parseIpv6Groups(address) {
  const lower = address.toLowerCase();
  const hasDouble = lower.includes('::');
  const parts = lower.split('::');
  const left = (parts[0] || '').split(':').filter(Boolean).flatMap(expandGroup);
  const right = (parts[1] || '').split(':').filter(Boolean).flatMap(expandGroup);
  if (!hasDouble) {
    if (left.length !== 8) return null;
    return left.map(hexToBigInt);
  }
  const missing = 8 - left.length - right.length;
  if (missing < 0 || left.length + right.length > 8) return null;
  return [...left, ...Array(missing).fill(0n), ...right].map(hexToBigInt);
}

export function isPrivateIpv6(address) {
  const groups = parseIpv6Groups(address);
  if (!groups) return true;
  const [a, b, c, d, e, f, g, h] = groups;
  if (groups.every((n) => n === 0n)) return true;
  if (a === 0n && b === 0n && c === 0n && d === 0n && e === 0n && f === 0n && g === 0n && h === 1n) return true;
  if ((a & 0xfe00n) === 0xfc00n) return true;
  if ((a & 0xffc0n) === 0xfe80n) return true;
  if (a >= 0xff00n) return true;
  if (a === 0x2001n && b === 0x0db8n) return true;
  if (a === 0n && b === 0n && c === 0n && d === 0n && e === 0n && f === 0xffffn) {
    const v4 = (g << 16n) | h;
    const octets = [(v4 >> 24n) & 0xffn, (v4 >> 16n) & 0xffn, (v4 >> 8n) & 0xffn, v4 & 0xffn];
    return isPrivateIpv4(octets.join('.'));
  }
  if (a === 0x0064n && b === 0xff9bn) {
    const v4 = (g << 16n) | h;
    const octets = [(v4 >> 24n) & 0xffn, (v4 >> 16n) & 0xffn, (v4 >> 8n) & 0xffn, v4 & 0xffn];
    return isPrivateIpv4(octets.join('.'));
  }
  return false;
}

function isIpv4(address) {
  return /^\d{1,3}(\.\d{1,3}){3}$/.test(address);
}

export function isPrivateAddress(address) {
  if (isIpv4(address)) return isPrivateIpv4(address);
  if (address.includes(':')) return isPrivateIpv6(address);
  return true;
}

export async function assertPublicHost(hostname, dnsLookup = defaultLookup) {
  const result = await dnsLookup(hostname, { all: true });
  const addresses = Array.isArray(result) ? result : [result];
  for (const entry of addresses) {
    const address = entry?.address ?? entry;
    if (isPrivateAddress(address)) {
      throw new Error(`SSRF blocked: ${hostname} resolved to private address ${address}`);
    }
  }
}