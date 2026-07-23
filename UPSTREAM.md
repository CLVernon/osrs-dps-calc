# Upstream sync state

The calculation engine (`src/calc/`) is a TypeScript port of the wiki DPS tool's core:
https://github.com/weirdgloop/osrs-dps-calc

**Synced to upstream commit: `54ab4b01` (2026-07-23, "Add summer sweepup sang staff buffs")**

To update the logic, diff upstream against this commit and port relevant changes:

```
gh api 'repos/weirdgloop/osrs-dps-calc/compare/54ab4b01...main' --jq '.files[].filename'
```

Files that matter for our port (everything else is their UI/state):
- `src/lib/PlayerVsNPCCalc.ts` → `src/calc/calc.ts`
- `src/lib/BaseCalc.ts` (gear detection, demonbane vulnerability) → `src/calc/gear.ts`, `src/calc/calc.ts`
- `src/lib/constants.ts` → `src/calc/constants.ts`
- `src/lib/Equipment.ts` (equipment totals, attack speed) → `src/calc/equipmentTotals.ts`
- `src/lib/scaling/ChambersOfXeric.ts` → `src/calc/coxScaling.ts`
- `src/lib/dists/bolts.ts` → bolt effects in `src/calc/calc.ts`
- `src/utils.ts` (combat styles per category) → `src/model/weaponStyles.ts`

Not ported (out of scope): special attacks, monster phases requiring manual input
(Tormented demon shield, Yama/Maggot King phases), ToB/ToA HP scaling, thralls,
seeking-arrow ammo/weapon compatibility tables (approximated as bows-only).

Data (equipment/monsters/spells JSON) needs no porting — fetched live from their
`cdn/json/` at runtime; refresh the bundled fallbacks in `public/data/` when convenient.

After porting, update the pinned commit above and add regression tests with
hand-computed values (see `tests/summerSweepUp.test.ts` for the pattern).
