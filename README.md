# OSRS DPS Calculator

A web-based DPS calculator for Old School RuneScape, built around **comparison**: create
any number of gear setups for your character and see them ranked side by side against any
number of target monsters.

**Live app: https://clvernon.github.io/osrs-dps-calc/**

## Features

- **Your character** — import levels from the official OSRS hiscores by username
  (regular/ironman/HCIM/UIM), or enter them manually. Stats are shared by every gear
  setup and remembered between visits.
- **Gear setups** — OSRS-style equipment grid with item icons and searchable pickers;
  combat styles come from the equipped weapon's real style options; prayers, potions,
  spells (all spellbooks) and situational buffs (slayer task, wilderness, Mark of
  Darkness, Charge, Kandarin diary, sunfire runes).
- **Targets** — 2,800+ monsters from the OSRS Wiki dataset with stat tooltips, custom
  monster editing, and one-click **CoX raid scaling** (party size, highest combat/HP,
  Mining for Guardians, Challenge Mode) applied to all CoX targets at once.
- **Comparison matrix** — setups as rows, monsters as columns, DPS per cell with
  best-per-monster highlighting, hover details (max hit, accuracy, overkill-aware
  time-to-kill), and a Total TTK column across all targets.
- **Presets** — gear setups and custom monsters saved in your browser (localStorage).
- **Always current** — item/monster/spell data loads from the wiki DPS tool's public
  dataset on each visit, with a bundled fallback for offline use.

## Combat engine

The calculation engine is a TypeScript port of the
[wiki DPS tool](https://tools.runescape.wiki/osrs-dps/)
([weirdgloop/osrs-dps-calc](https://github.com/weirdgloop/osrs-dps-calc)) core, using
full hit distributions so proc effects compose correctly. Covered mechanics include void
sets, salve/slayer helm stacking rules, twisted bow scaling, Tumeken's shadow, Osmumten's
fang, scythe multi-hits, enchanted bolts, vampyre tiers, Barrows set effects, demonbane,
elemental weaknesses, chinchompas, salamanders, boss-specific caps/immunities (Zulrah,
Tekton, Olm, Corp, CoX Guardians...), ToA invocation defence scaling, and full CoX party
scaling. Special attacks are not modelled.

## Development

```bash
npm install
npm run dev     # dev server
npm test        # vitest engine suite
npm run build   # type-check + production build
```

Deployed automatically to GitHub Pages by `.github/workflows/deploy.yml` on push to main.

## Data & attribution

Item/monster/spell stats come from the
[weirdgloop/osrs-dps-calc](https://github.com/weirdgloop/osrs-dps-calc) dataset, and the
engine is ported from the same project. Icons load from the
[OSRS Wiki](https://oldschool.runescape.wiki/). Hiscores lookups use the wiki's public
CORS proxy.

The original JavaFX desktop version of this project is preserved in git history
(up to commit `8c2afab`).
