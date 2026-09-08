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
- As a Player I want to create a D&D character through a guided wizard or a "surprise me" quick-build so I can bring a legal level 1–3 hero to my sessions. (R13) 🔨 in progress (draft-design §8)
- As a DM I want the platform to run on a production database behind CI so we can actually play with friends online. (R14) 🔨 in progress (draft-design §11/§13 Stage 5)

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