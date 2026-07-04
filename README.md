# OSRS DPS Calculator

A desktop DPS calculator for Old School RuneScape, built with Java 21 and JavaFX.

Unlike single-loadout web calculators, this app is built around **comparison**: create any
number of player setups and see them ranked side by side against any monster, with presets
for both players and monsters.

## Features

- **Multiple player setups** — add, duplicate, and remove loadouts freely; each has its own
  levels, equipment (searchable pickers over the full wiki item database), attack style,
  prayer, potion, spell, and slayer-task flag.
- **Any monster** — 2,800+ monsters bundled from the OSRS Wiki dataset, plus an editor to
  tweak stats or create fully custom monsters.
- **Presets** — save/load both player setups and monsters as JSON under
  `%APPDATA%\osrs-dps-calc\presets`.
- **Comparison table** — all setups ranked by DPS against the current target, showing max
  hit, accuracy, average damage per attack, attack interval, and estimated time-to-kill.
  The best setup is highlighted; selecting a row lists the special effects that applied.

### Combat mechanics covered

Standard melee/ranged/magic formulas from the
[OSRS wiki](https://oldschool.runescape.wiki/w/Damage_per_second), plus:

- Void knight sets (regular and elite, all three styles)
- Salve amulet (e)/(i)/(ei) vs undead; slayer helmet / black mask (i) on task
  (salve takes priority, no stacking)
- Twisted bow magic-level scaling (capped at 250)
- Tumeken's shadow (triples equipment magic bonuses, damage capped at +100%)
- Osmumten's fang (accuracy re-roll, 15%–85% damage range)
- Scythe of Vitur multi-hits vs size-2+ targets
- Dragon hunter lance / dragon hunter crossbow vs dragons
- Arclight/Emberlight and Silverlight/Darklight vs demons
- Keris vs kalphites (incl. 1/51 triple hit)
- Leaf-bladed battleaxe vs leafy; Inquisitor's set vs crush
- Crystal armour bonus with crystal bow / bow of faerdhinen
- Powered staves (tridents, Sanguinesti, shadow, sceptres, Gauntlet staves…)
- Elemental spell weaknesses (accuracy bonus by severity)
- Separate monster ranged defences vs heavy/standard/light weapons
- Flat armour damage reduction (e.g. Tormented demons)

### Known simplifications

- TTK is `hp / dps` (no overkill/hitpoint-state simulation).
- ToA invocation scaling, Vardorvis/Corp-style custom defence mechanics, demonbane spells,
  Mark of Darkness, tomes, chinchompa fuse styles, and salamanders are not modelled.
- Newer Varlamore prayers (Deadeye, Mystic Vigour) are not included.
- Item effect values reflect the post-"Project Rebalance" data where the wiki dataset does
  (e.g. occult necklace at +5%).

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

## Data

Item and monster stats are bundled at `src/main/resources/data/` and come from the
[weirdgloop/osrs-dps-calc](https://github.com/weirdgloop/osrs-dps-calc) dataset (the same
data behind the wiki's DPS tool). To refresh them, re-download:

```powershell
Invoke-WebRequest https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/equipment.json -OutFile src/main/resources/data/equipment.json
Invoke-WebRequest https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/monsters.json -OutFile src/main/resources/data/monsters.json
```

## Project layout

```
src/main/java/com/osrs/dps/
  App.java          JavaFX application and main window
  model/            Equipment, monsters, player setups, prayers/potions/spells
  data/             Bundled wiki data loading
  calc/             DPS engine (wiki formulas + special cases)
  preset/           JSON preset persistence
  ui/               Setup editor, monster editor, comparison table, search picker
```
