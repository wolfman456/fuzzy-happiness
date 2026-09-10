# Maps Resources — Free Battle Map Research

**Context:** we need battle maps beyond the plain 2D grid before we start on the 3D
generation work (draft-design §17). The 2D grid battle map track 1 is landed; this
research covers where to source map *images* and assets to drop under/over the grid,
and what map-making tooling exists for producing our own later.

**Scope:** free sources only, link-only research (nothing bundled into the repo).
Downloads are a manual curation step when we actually need maps.

---

## Lost Atlas — https://lostatlas.co/

- **Type:** battle-map **search engine / index** (JS app — not a direct source).
- **What it offers:** indexes **5,000+ free maps** aggregated from creators. Filters by
  environment, size, creator, keyword, animated. "Optimized for Roll20 free" tag,
  bookmarking, "Trending" sort. A paid premium tier surfaces maps from top creators.
- **Format:** links out to the map on the original creator's site — you download from
  the *creator*, not from Lost Atlas.
- **License:** per-creator (each map links back to its origin). Nothing hosted here is
  ours to redistribute.
- **Verdict:** the best **discovery layer** over every other source in this doc. Use it
  to find "docks / desert / mansion" maps quickly and jump to the creator's download,
  rather than browsing each site blindly.

## 2-Minute Tabletop — https://2minutetabletop.com/product-category/free/

- **Type:** direct **source** of maps, assets, and tools.
- **What it offers:** ~**386 free items**, most PWYW ("$1 or FREE" with a free account).
  Battle maps (small 13x17 up to large sets), **map assets** (props/decorations for
  Dungeondraft/Wonderdraft), spell templates, papercraft, plus a free Token Editor and
  token collections. A paid "Everything Pack" bundle ($70) exists but is not needed.
- **Formats:** Roll20 / Fantasy Grounds optimized web images, plus easy-to-print PDFs for
  several packs. Sizes listed per map (e.g. 16x22, 22x32).
- **License:** clear licensing & attribution FAQ
  (https://2minutetabletop.com/faq/license-and-attribution/) — free maps are usable with
  attribution; supports **redistribution under their terms** (which the Fateful Force
  below is *not*). WooCommerce account required to download.
- **Verdict:** **primary curated source.** Largest licensed free pool, consistent style,
  multiple output formats, and its asset packs map directly to future procedural map
  building / 3D scene texturing.

## The Fateful Force — https://thefatefulforce.com/battle-resources/battle-maps/

- **Type:** direct **source** of finished HD battle-map **sets**.
- **What it offers:** free HD map sets as zips. Includes full re-reads of **Lost Mine of
  Phandelver** and **Curse of Strahd** plus ~20 generic maps (arena, airship, forest,
  dungeon, graveyard, shipwreck, bridges, taverns…). Maps come in day/night variants with
  grid sizes on each (e.g. 30x30, 40x30, 70x97). Bonus: free VTT Token Maker tool
  (https://thefatefulforce.com/battle-resources/token-creator/).
- **Format:** HD PNG map sets bundled in zip downloads.
- **License:** **"personal use only"** — maps are made with Dungeondraft using 2-Minute
  Tabletop / Forgotten Adventures assets and copyrighted. **Cannot be bundled or
  redistributed in the app**; link-only.
- **Verdict:** good for **scenario-specific** maps and quick drop-in sessions we grab by
  hand. Not a candidate for shipping content. The free token maker is a nice side tool.

## Dungeon Scrawl — https://www.dungeonscrawl.com/

- **Type:** **map-making tool** (now powered by Roll20), not a map library.
- **What it offers:** free in-browser dungeon/battle-map builder — no downloads, no
  sign-up. Simple shape tools, one-click styles, free props/markers library, random
  dungeon import from popular generators, "tabletop view" for live play, Roll20 sync.
- **Formats:** export PNG, UVTT (universal VTT format with walls/dynamic lighting), print.
- **License:** free core; **commercial-use rights require the Pro subscription**.
- **Verdict:** our on-demand **procedural generation** path later — make custom maps for
  specific encounters instead of searching. Not needed now (2D grid uses image
  backgrounds), but relevant when we want generated terrain for the 3D floor (§17).

---

## Cross-cutting takeaways

| Source | Kind | Redistributable? | Use for us |
|---|---|---|---|
| Lost Atlas | Search index (5,000+) | n/a (links out) | Discovery layer to find any theme fast |
| 2-Minute Tabletop | Source (~386 free) | Yes (terms + attribution) | **Primary** curated map/asset library |
| The Fateful Force | Source (scenario sets) | **No** (personal use) | Hand-grab specific quest maps |
| Dungeon Scrawl | Maker | Pro = commercial | Future on-demand/procedural maps, 3D terrain |

**Recommendation:** use **2-Minute Tabletop** as the primary licensed pool for map images
and assets, with **Lost Atlas** as the search layer across all free creators; treat
**The Fateful Force** as personal-use-only scenario grabs; keep **Dungeon Scrawl** in mind
for generated maps once we need custom terrain for the 3D viewport. Downloads stay a
manual step — nothing is committed to `tabletopweb/`.