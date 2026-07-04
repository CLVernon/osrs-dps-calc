# OSRS DPS Calculator

A desktop DPS calculator for Old School RuneScape, built with Java 21 and JavaFX
(AtlantaFX dark theme).

Unlike single-loadout web calculators, this app is built around **comparison**: create any
number of player setups and see them ranked side by side against any monster, with presets
for both players and monsters.

## Features

- **Your character** — import levels straight from the official OSRS hiscores by username
  (regular/ironman/HCIM/UIM boards), or enter them manually. Stats are saved and shared by
  every gear setup, so comparisons are always for the same character.
- **Multiple gear setups** — add, duplicate, remove loadouts; each has its own equipment
  with item icons, attack style, prayer, potion, spell, and situational buffs (slayer
  task, wilderness, Mark of Darkness, Charge, Kandarin diary, sunfire runes...).
- **Any monster** — 2,800+ monsters bundled from the OSRS Wiki dataset with icons, plus an
  editor for custom monsters (all attributes, elemental weaknesses, flat armour,
  ToA invocation level).
- **Auto-updating data** — on startup (at most once per day) the app downloads the latest
  equipment/monster/spell data from the wiki DPS tool's dataset into
  `%APPDATA%\osrs-dps-calc\data`, falling back to bundled data offline.
- **Presets** — save/load gear setups and monsters as JSON under
  `%APPDATA%\osrs-dps-calc\presets`; the character is remembered between sessions.
- **Comparison table** — all setups ranked by DPS with max hit, accuracy, average damage
  per attack, attack interval, average hits-to-kill and **overkill-aware time-to-kill**
  (computed from the full hit distribution, not `hp / dps`). Best setup highlighted;
  selecting a row lists every special effect that applied.

## Combat mechanics

The engine is a Java port of the calculation core of the
[wiki DPS tool](https://tools.runescape.wiki/osrs-dps/)
([weirdgloop/osrs-dps-calc](https://github.com/weirdgloop/osrs-dps-calc)), using full hit
distributions so proc effects compose correctly. Covered mechanics include:

- Void / elite void (all styles), salve amulet variants, black mask / slayer helmet (i)
  with correct additive stacking rules per style
- Twisted bow (exact scaling, 350 cap vs Xerician monsters, P2 Wardens double-dip)
- Tumeken's shadow (3x bonuses, 4x inside ToA, +100% damage cap)
- Osmumten's fang (exact double-roll accuracy formula, ToA variant, 15%–85% range)
- Scythe of Vitur multi-hits; two-hit weapons (Torag's, Dual macuahuitl, sulphur blades...)
- Dragon hunter lance / crossbow / wand; arclight, emberlight, silverlight/darklight,
  scorching bow, burning claws (demonbane %)
- Demonbane spells with Mark of Darkness and Purging staff
- Keris variants (incl. partisan of breaching accuracy and 1/51 triple)
- Enchanted bolts: opal, pearl, diamond, dragonstone, onyx, ruby (Zaryte crossbow and
  Kandarin diary modifiers, ruby HP caps)
- Vampyre tiers 1-3: silver weapons, vampyrebane weapons, Efaritay's aid caps/immunities
- Barrows set effects: Dharok's (current HP), Verac's, Karil's, Ahrim's
- Obsidian set + Tzhaar weapons + berserker necklace; Inquisitor's (incl. mace rules);
  crystal armour + crystal bow/bowfa; Virtus + ancient spells
- Chinchompa fuse/distance accuracy; salamanders; ogre bows; holy water; eclipse atlatl
  (strength scaling); tonalztics of ralos; colossal blade; rat bone weapons; gadderhammer;
  barronite mace; granite hammer; leaf-bladed battleaxe (and leafy immunity)
- Amulet of avarice (+ Forinthry Surge) vs revenants; wilderness weapons (Craw's/Webweaver,
  Thammaron's/Accursed, Viggora's/Ursine)
- Smoke battlestaff, elemental tomes, chaos gauntlets, Charge, sunfire runes, brimstone
  ring (25% defence-roll proc), harmonised nightmare staff 4t autocast, Twinflame staff
- Powered staves (tridents, sanguinesti, shadow, sceptres, wands, Gauntlet staves, bone
  staff...) and level-scaled standard spells; Magic Dart with Slayer's staff (e)
- Elemental weaknesses (accuracy and damage); monster ranged defences by weapon class
  (light/standard/heavy/mixed); NPC magic defence via Defence level for the right NPCs
- ToA invocation defence scaling; flat armour (Tormented demons)
- Boss-specific damage caps/reductions: Zulrah, Kraken, Tekton, Olm hands/head, ice demon,
  Glowing crystal, Verzik P1, Nightmare totems, Corporeal Beast (corpbane), Zogres,
  Slagilith, CoX Guardians (pickaxe + Mining scaling), Fragment of Seren
- Style/weapon immunities (flying + melee, aviansies, leafy, rat weapons, CoX Guardians...)

### Known simplifications

- Special attacks are not modelled (regular attacks only).
- Monster phase mechanics needing manual input (Tormented demon shield, Hueycoatl pillars,
  Doom of Mokhaiotl) are not modelled; ToA path/invocation HP scaling applies to defence
  only, not monster HP.
- Blood moon set proc speed, Soulreaper axe stacks, and thralls are not modelled.

## Requirements

- JDK 21 (no Maven install needed — the Maven wrapper downloads it)

## Run

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-21"
.\mvnw.cmd javafx:run
```

## Test

```powershell
.\mvnw.cmd test
```

## Data & attribution

Item/monster/spell stats come from the
[weirdgloop/osrs-dps-calc](https://github.com/weirdgloop/osrs-dps-calc) dataset (the data
behind the wiki's DPS tool); the calculation engine is ported from the same project.
Item and monster icons are loaded on demand from the OSRS Wiki and cached in
`%APPDATA%\osrs-dps-calc\cache`.

## Project layout

```
src/main/java/com/osrs/dps/
  App.java          JavaFX application, startup data update, main window
  model/            Equipment, monsters, spells, player setups, prayers/potions
  data/             Data loading, auto-updater, image cache
  calc/             Hit-distribution DPS engine (wiki tool port)
  calc/dist/        Hit distribution primitives and transformers
  preset/           JSON preset persistence
  ui/               Setup editor, monster editor, comparison table, search picker
```
