// Helpers for stubbing the backend from Playwright. The E2E suite runs the UI in a
// real browser without the Spring backend, so every API call a spec exercises is
// intercepted with `page.route`. VITE_API_URL is pinned to the dev-server origin
// (playwright.config.js) so these calls are same-origin and need no CORS headers.

export const E2E_USER = {
  id: 1,
  username: 'aria',
  displayName: 'Aria',
  email: 'aria@example.com',
  role: 'USER',
  emailVerified: true,
}

export function jsonResponse(status, body) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  }
}

export function mockLoginSuccess(page, user = E2E_USER) {
  return page.route('**/api/auth/login', (route) =>
    route.fulfill(
      jsonResponse(200, {
        token: 'e2e-token',
        tokenType: 'Bearer',
        expiresInSeconds: 86_400,
        user,
      }),
    ),
  )
}

export function mockLoginFailure(page, message = 'Authentication required') {
  return page.route('**/api/auth/login', (route) =>
    route.fulfill(jsonResponse(401, { status: 401, message })),
  )
}

export function mockRegisterSuccess(page, user = E2E_USER) {
  return page.route('**/api/auth/register', (route) => route.fulfill(jsonResponse(201, user)))
}

export function mockMe(page, user = E2E_USER) {
  return page.route('**/api/users/me', (route) => route.fulfill(jsonResponse(200, user)))
}

const SRD_LISTS = {
  races: [{ index: 'dwarf', name: 'Dwarf' }],
  classes: [{ index: 'cleric', name: 'Cleric' }],
  backgrounds: [{ index: 'acolyte', name: 'Acolyte' }],
  skills: [
    { index: 'history', name: 'History' },
    { index: 'medicine', name: 'Medicine' },
    { index: 'religion', name: 'Religion' },
  ],
  equipment: [{ index: 'shield', name: 'Shield' }],
}

const SRD_DETAILS = {
  'races/dwarf': {
    index: 'dwarf',
    name: 'Dwarf',
    ability_bonuses: [{ ability_score: { index: 'con' }, bonus: 2 }],
    size: 'Medium',
    size_description: 'Dwarves stand between 4 and 5 feet tall and average about 150 pounds.',
    age: 'Dwarves mature at the same rate as humans, but live about 350 years.',
    alignment: 'Most dwarves are lawful, tending toward good.',
    languages: [
      { index: 'common', name: 'Common' },
      { index: 'dwarvish', name: 'Dwarvish' },
    ],
  },
  'classes/cleric': {
    index: 'cleric',
    name: 'Cleric',
    hit_die: 8,
    proficiency_choices: [],
    subclasses: [],
    subclass_level: 1,
    starting_equipment: [],
    starting_equipment_options: [],
  },
}

/**
 * Stubs the SRD proxy the character wizard reads from. Pass `lists`/`details` to
 * override the built-in fixtures. All calls are same-origin (VITE_API_URL is the
 * dev-server origin), so no CORS headers are needed.
 */
export function mockSrd(page, { lists = {}, details = {} } = {}) {
  const allLists = { ...SRD_LISTS, ...lists }
  const allDetails = { ...SRD_DETAILS, ...details }
  return page.route('**/api/srd/**', (route) => {
    const { pathname } = new URL(route.request().url())
    const rest = pathname.replace(/^.*\/api\/srd\//, '')
    if (rest.includes('/')) {
      const detail = allDetails[rest]
      if (detail === undefined) return route.fulfill(jsonResponse(404, { message: 'not found' }))
      return route.fulfill(jsonResponse(200, detail))
    }
    const rows = allLists[rest]
    if (!rows) return route.fulfill(jsonResponse(404, { message: 'not found' }))
    return route.fulfill(jsonResponse(200, { count: rows.length, results: rows }))
  })
}

export function mockChargenCatalog(page, catalog = { backgrounds: [], subclasses: [] }) {
  return page.route('**/api/characters/catalog', (route) => route.fulfill(jsonResponse(200, catalog)))
}

export const E2E_CHARACTER = {
  id: 7,
  name: 'Tordek',
  level: 3,
  scoreSource: 'STANDARD_ARRAY',
  sex: 'FEMALE',
  appearance: { skinTone: 120, hairTone: 300 },
  raceIndex: 'dwarf',
  classIndex: 'cleric',
  subclassIndex: 'life',
  backgroundIndex: 'acolyte',
  strength: 15,
  dexterity: 14,
  constitution: 13,
  intelligence: 12,
  wisdom: 10,
  charisma: 8,
  proficiencyBonus: 2,
  hitPoints: 22,
  armorClass: 16,
  speedFeet: 25,
  savingThrows: ['wis', 'cha'],
  classSkills: ['medicine', 'religion'],
  backgroundSkills: [],
  skillPicks: ['medicine', 'religion'],
  spellIndexes: ['sacred-flame', 'bless'],
  featureIndexes: ['spellcasting', 'disciple-of-life', 'darkvision'],
  equipmentIndexes: ['leather-armor', 'shield'],
}

export function mockCharacter(page, character = E2E_CHARACTER) {
  return page.route(`**/api/users/me/characters/${character.id}`, (route) =>
    route.fulfill(jsonResponse(200, character)),
  )
}
