import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import type { EquipmentItem, Monster, PlayerCharacter, SpellData } from '../src/model/types';
import { defaultCharacter, displayName } from '../src/model/types';
import type { CalcContext } from '../src/calc/calc';

const dataDir = join(__dirname, '..', 'public', 'data');

const load = <T>(name: string): T[] =>
  JSON.parse(readFileSync(join(dataDir, name), 'utf-8'));

export const equipment: EquipmentItem[] = load('equipment.json');
export const monsters: Monster[] = load('monsters.json');
export const spells: SpellData[] = load('spells.json');

export const findEquipment = (name: string): EquipmentItem => {
  const item = equipment.find((e) => displayName(e).toLowerCase() === name.toLowerCase());
  if (!item) throw new Error(`Equipment not found: ${name}`);
  return item;
};

export const findSpell = (name: string): SpellData => {
  const spell = spells.find((s) => s.name === name);
  if (!spell) throw new Error(`Spell not found: ${name}`);
  return spell;
};

export const maxedCharacter = (): PlayerCharacter => defaultCharacter();

export const ctx = (character: PlayerCharacter = maxedCharacter()): CalcContext => ({
  character,
  allSpells: spells,
});

export const dummyMonster = (overrides: {
  def?: number; stab?: number; slash?: number; crush?: number;
  magicLevel?: number; magicDef?: number;
  heavy?: number; standard?: number; light?: number;
  hp?: number; flatArmour?: number; attributes?: string[]; size?: number;
} = {}): Monster => ({
  id: -1,
  name: 'Test dummy',
  version: '',
  image: '',
  level: 1,
  speed: 4,
  size: overrides.size ?? 1,
  skills: {
    atk: 1,
    def: overrides.def ?? 100,
    hp: overrides.hp ?? 100,
    magic: overrides.magicLevel ?? 1,
    ranged: 1,
    str: 1,
  },
  offensive: { atk: 0, magic: 0, magic_str: 0, ranged: 0, ranged_str: 0, str: 0 },
  defensive: {
    flat_armour: overrides.flatArmour ?? 0,
    stab: overrides.stab ?? 0,
    slash: overrides.slash ?? 0,
    crush: overrides.crush ?? 0,
    magic: overrides.magicDef ?? 0,
    heavy: overrides.heavy ?? 0,
    standard: overrides.standard ?? 0,
    light: overrides.light ?? 0,
  },
  attributes: overrides.attributes ?? [],
  weakness: null,
});
