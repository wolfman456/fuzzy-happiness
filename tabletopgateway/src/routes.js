export const SRD_TARGET = 'https://www.dnd5eapi.co/api/2014';

export const SRD_COLLECTIONS = [
  'races',
  'classes',
  'subclasses',
  'subraces',
  'ability-scores',
  'skills',
  'proficiencies',
  'equipment',
  'equipment-categories',
  'spells',
  'features',
  'traits',
  'feats',
  'conditions',
  'languages',
  'monsters',
];

export const SRD_ALLOWED_PARAMS = ['level', 'school', 'name', 'index'];

export const SRD_TIMEOUT_MS = 8000;
export const SRD_MAX_BYTES = 2 * 1024 * 1024;
export const SRD_LIST_TTL_MS = 60 * 60 * 1000;
export const SRD_DETAIL_TTL_MS = 15 * 60 * 1000;

export function defaultRoutes() {
  return [
    {
      name: 'srd',
      path: '/api/srd',
      target: SRD_TARGET,
      collections: SRD_COLLECTIONS,
      allowQuery: new Set(SRD_ALLOWED_PARAMS),
      indexPattern: /^[A-Za-z0-9_-]+$/,
      timeoutMs: SRD_TIMEOUT_MS,
      maxBytes: SRD_MAX_BYTES,
      cacheTtlMs: { list: SRD_LIST_TTL_MS, detail: SRD_DETAIL_TTL_MS },
    },
  ];
}