import { api } from './api'

export const MONSTER_ROLES = [
  'AUTO',
  'BALANCED',
  'BRUTE',
  'DEFENDER',
  'SKIRMISHER',
  'ARTILLERY',
  'CONTROLLER',
  'LURKER',
  'SUPPORT',
  'SWARM',
]

export const MONSTER_EDITIONS = ['SRD_2014', 'SRD_2024']

export const MONSTER_CRS = [
  '0',
  '1/8',
  '1/4',
  '1/2',
  '1',
  '2',
  '3',
  '4',
  '5',
  '6',
  '7',
  '8',
  '9',
  '10',
  '11',
  '12',
  '13',
  '14',
  '15',
  '16',
  '17',
  '18',
  '19',
  '20',
  '21',
  '22',
  '23',
  '24',
  '25',
  '26',
  '27',
  '28',
  '29',
  '30',
]

export function generateMonster(body) {
  return api('/api/monsters/generate', { method: 'POST', body })
}

export function listMyMonsters() {
  return api('/api/monsters/mine')
}