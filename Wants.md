## Instructions :
Please frame all asks as follows :
* As a Player I want to be able to ....
* As a DM I want to be able to .... 

After placing the request in this file and commit it, place a pull request and I will review then commit.

## Feature Requests

Status key: ✅ landed · 🔨 in progress · ⏭ deferred (post-MVP)

- As a Player I want to create a game session (room) and get an invite code so I can invite my friends to play with me. (R2) ✅ landed
- As a Player I want to join a session with an invite code so all of us can be in the same room at the same time. (R3) ✅ landed
- As a Player I want to see who is in the session and chat live so I can interact with the group in real time. (R4) ✅ landed
- As a Player I want to roll dice inside the session (e.g. `2d6+3`) and see the outcome shared with the table so we don't have to trust each other's honour rolls. (R5) ✅ landed
- As a DM I want to make a secret (GM-only) roll whose result is hidden from the players, so I can fudge perception checks and enemy skill checks without tipping off the party. (R6) ✅ landed
- As a DM I want to set an initiative order for the current encounter (players, monsters and custom entries, with blank scores auto-rolled by the server) so everyone knows who acts when. (R7) ✅ landed
- As a DM I want to advance the initiative order on the shared table — re-rolling and removing entries as needed, with each creature's turn activating their movement budget — so combat flows without everybody shouting turn order. (R8) ✅ landed
- As a DM I want to generate a homebrew monster statblock from a Challenge Rating and a combat role so I can drop custom creatures into my encounters without hand-drawing every number. (R9) ✅ landed (draft-design §9b, PR #25)
- As a DM I want all external data and generation calls (SRD lookups, monster generation, future integrations) to route through a single secure Express gateway so our backend only talks to vetted, allowlisted upstream services. (R10) ✅ landed
- As a DM I want to create homebrew content (e.g. monsters) that my GM tooling can persist and reuse across sessions. (R11) ✅ landed (ships with R9)
- As a Player I want an optional 3D view of the battle map with player avatars and monster minis so the table feels more immersive alongside the existing 2D grid. (R12) ⏭ deferred (post-MVP, draft-design §17)
- As a Player I want to create a D&D character through a guided wizard or a "surprise me" quick-build so I can bring a legal level 1–3 hero to my sessions. (R13) ✅ landed (draft-design §8, PR #31)
- As a DM I want the platform to run on a production database behind CI so we can actually play with friends online. (R14) ✅ landed (draft-design §11/§13 Stage 5; Railway infra as code — §19, PR #31)
- As a Player I want a polished sign-in/sign-up page with a themed background so the entry screen feels part of the game. (R15) ✅ landed
- As a Player I want to confirm my password twice on registration so typos don't lock me out. (R16) ✅ landed
- As a Player I want to supply my real name separately from my username and display name so I can be addressed properly without exposing my account handle. (R17) ✅ landed
- As a Player I want my personal details (email, names, date of birth) encrypted at rest so a database leak doesn't expose my identity. (R18) ✅ landed
- As a Player I want to sign in with my Google, Facebook or GitHub account so I can start playing without yet another password. (R19) 🔨 in progress
- As an admin I want the production database seeded with a bootstrap administrator so I can administer the platform from day one. (R20) ✅ landed
- As a Player I want to change my username after registering so I can pick a handle that grows with my campaign. (R21) ✅ landed (PR #35)
- As a Player I want a dragon-themed background on the sign-in/sign-up screens so the entry screen fits the game's fantasy feel. (R22) ✅ landed (PR #35)
- As a Player I want the character wizard to generate my ability scores using the dice-roll method I chose (server-side rolls for rolled methods) instead of letting me assign any numbers, so my hero is legal. (R23) ✅ landed
- As a Player I want the class list to include expanded classes beyond the free SRD core (e.g. Artificer) so I can play the class I want. (R24) ⏭ deferred (needs a non-SRD data source / homebrew expansion, draft-design research)
- As a Player I want my starting equipment to be purchased from my class/background starting gold rather than granted for free, so my kit matches my wealth. (R25) ✅ landed
- As a Player I want registration not to hard-fail when the verification email can't be sent, so I'm still signed up and can verify through a resent link once the mail server is reachable. (R26) ✅ landed
- As a Player I want to change my password and edit my profile details (display name, real name) after signing in, so I can keep my account secure and my name accurate. (R27) ✅ landed
- As a Player I want to roll my ability scores one at a time (or all at once) with an animated per-ability dice roll in the wizard, and see the equipment shop price everything against live SRD costs — with the class kit trimming itself to my starting gold — so the totals match what compiling actually charges. (R28) ✅ landed (PR #39)
- As a Player I want a themed backdrop across all screens of the app — not empty white pages — so the whole app feels part of the game rather than a form tool. (R29) ✅ landed (PR #43)
- As a Player I want the app's navigation tucked into a slide-in sidebar that opens from a menu button in the top bar, so screens stay uncluttered and the top header stays slim. (R30) ✅ landed (PR #43)

## MVP definition (agreed 2026-09-08)

A playable D&D 5e table in one app: accounts → sessions/chat → dice/initiative/battle map →
character creation (wizard + quick-build) → monster generation (deterministic statblocks, persisted
and reusable) → deployed on PostgreSQL behind CI. **Out of MVP:** Discord voice, 3D viewport (R12),
LLM monster flavor (template flavor ships instead), 2024 ruleset, multilingual, JWT refresh.
Tracking board: https://github.com/users/wolfman456/projects/1

## Expectation
Once we have gathered input from all of you we will create a MVP (Minimum Viable Product). This will be the starting goal and what we design the original architecture to. I Do not plan to do all of this on my own or to hold your hands through this process.
Before you can even make the first commit and PR you will need to learn how to clone a repo, create a branch update a .md file, commit and then push that branch back up to github. These are minimum requirements to take part in this project. 
I fill also be creating a learning file, that will provide sites, exercises, and basic information on how to wrote in Java and JavaScript, as well as some SQL. The SQL will mainly be handle through the java back end itself. but some basic understand helps.
I will going forward also create a message page. That I will use to give instructions: additionally when you create a pr I am the only one that can merge it to develop or master. I will comment is I do not like something or have suggestion. These have to be resolve before merger.