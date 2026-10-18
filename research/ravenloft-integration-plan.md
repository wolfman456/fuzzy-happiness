# Ravenloft integration plan (2026-10-15)

## Background
- BootstrapGameRunner currently seeds `dnd-5e` (slug) with a minimal sheetSchema.
- Game model (domain + API) exists; we could add a new game slug `ravenloft-2024` or similar.
- SRD gateway routes to `https://www.dnd5eapi.co/api/2014` (deny-by-default). 2014 rules differ from 2024 Ravenloft/updated rules.

## Questions / decisions needed
1. Game selection: support Ravenloft as a separate game in games catalog (Game entity + bootstrap) or as module/ruleset overlay on dnd-5e?
2. SRD source: use a different upstream? Or mirror rules? SRD_TARGET is hardcoded in gateway; collections/params/subresources allowlist. Need to understand Ravenloft data shape.
3. Fear/horror/stress mechanics: if present, need to model as optional house rules (Wants.md has R45 for house rules).
4. Content licensing: only use open/authorized sources. Scribd reference was problematic.

## Recommended approach (incremental)
- Step 1: Add Ravenloft game entry to Game catalog (bootstrap + migrations if needed; schema for games exists). Define sheetSchema to be compatible or extendable.
- Step 2: Evaluate SRD access. If using official 5e SRD (2014) most core data overlaps; Ravenloft-specific content may require custom data or a different source.
- Step 3: Gateway: if different target needed, make SRD_TARGET configurable per game context? Current routing is single defaultRoutes for /api/srd. Could extend to support multiple rule sources.
- Step 4: Frontend: allow game selection (games list exists via GameApi). Update UI to handle different rule sets.

## Touchpoints
- tabletopserv: BootstrapGameRunner, domain/Game, GameApi/GameSummary, SrdControllerImpl/SrdClient
- tabletopgateway: routes.js (SRD_TARGET, collections, subresources)
- tabletopweb: games/SRD consumption, character wizard (uses SRD data)
