// Core data types matching the wiki dataset (weirdgloop/osrs-dps-calc cdn/json)

export interface EquipmentItem {
  name: string;
  id: number;
  version: string;
  slot: string;
  image: string;
  speed: number;
  category: string;
  bonuses: {
    str: number;
    ranged_str: number;
    /** Magic damage bonus in tenths of a percent (50 = +5.0%) */
    magic_str: number;
    prayer: number;
  };
  offensive: { stab: number; slash: number; crush: number; magic: number; ranged: number };
  defensive: { stab: number; slash: number; crush: number; magic: number; ranged: number };
  isTwoHanded: boolean;
}

export interface Monster {
  id: number;
  name: string;
  version: string;
  image: string;
  level: number;
  speed: number;
  size: number;
  skills: { atk: number; def: number; hp: number; magic: number; ranged: number; str: number };
  offensive: {
    atk: number; magic: number; magic_str: number; ranged: number; ranged_str: number; str: number;
  };
  defensive: {
    flat_armour: number; crush: number; magic: number;
    heavy: number; standard: number; light: number; slash: number; stab: number;
  };
  attributes: string[];
  weakness: { element: string; severity: number } | null;

  // Raid scaling inputs (not part of the wiki data; defaulted on load)
  toaInvocationLevel?: number;
  partySize?: number;
  partyMaxCombatLevel?: number;
  partyMaxHpLevel?: number;
  partyAvgMiningLevel?: number;
  coxChallengeMode?: boolean;
}

export interface SpellData {
  name: string;
  image: string;
  max_hit: number;
  spellbook: 'standard' | 'ancient' | 'lunar' | 'arceuus';
  element: 'air' | 'water' | 'earth' | 'fire' | null;
}

export type AttackType = 'stab' | 'slash' | 'crush' | 'ranged' | 'magic';

export type Stance =
  | 'Accurate'
  | 'Aggressive'
  | 'Controlled'
  | 'Defensive'
  | 'Rapid'
  | 'Longrange'
  | 'Autocast'
  | 'Defensive Autocast'
  | 'Manual Cast';

export interface CombatStyle {
  name: string;
  type: AttackType;
  stance: Stance;
}

export type EquipmentSlotName =
  | 'head' | 'cape' | 'neck' | 'ammo' | 'weapon' | 'shield'
  | 'body' | 'legs' | 'hands' | 'feet' | 'ring';

export const EQUIPMENT_SLOTS: EquipmentSlotName[] = [
  'head', 'cape', 'neck', 'ammo', 'weapon', 'shield', 'body', 'legs', 'hands', 'feet', 'ring',
];

/** The character: stats shared by all gear setups. */
export interface PlayerCharacter {
  name: string;
  attack: number;
  strength: number;
  defence: number;
  ranged: number;
  magic: number;
  hitpoints: number;
  mining: number;
  /** Current HP for Dharok's; 0 = full health */
  currentHitpoints: number;
}

export const defaultCharacter = (): PlayerCharacter => ({
  name: '',
  attack: 99,
  strength: 99,
  defence: 99,
  ranged: 99,
  magic: 99,
  hitpoints: 99,
  mining: 99,
  currentHitpoints: 0,
});

/** A gear loadout; stats come from the shared character. */
export interface PlayerSetup {
  /** Unique id for UI list handling */
  uid: string;
  name: string;
  equipment: Partial<Record<EquipmentSlotName, EquipmentItem>>;
  styleName: string;
  attackType: AttackType;
  stance: Stance;
  prayerId: string;
  potionId: string;
  spellName: string | null;
  onSlayerTask: boolean;
  inWilderness: boolean;
  forinthrySurge: boolean;
  markOfDarkness: boolean;
  chargeSpell: boolean;
  kandarinDiary: boolean;
  sunfireRunes: boolean;
  chinchompaDistance: number;
}

let uidCounter = 0;

export const newSetup = (name: string): PlayerSetup => ({
  uid: `setup-${Date.now()}-${uidCounter++}`,
  name,
  equipment: {},
  styleName: 'Punch',
  attackType: 'crush',
  stance: 'Accurate',
  prayerId: 'NONE',
  potionId: 'NONE',
  spellName: null,
  onSlayerTask: false,
  inWilderness: false,
  forinthrySurge: false,
  markOfDarkness: false,
  chargeSpell: false,
  kandarinDiary: false,
  sunfireRunes: false,
  chinchompaDistance: 5,
});

export const copySetup = (s: PlayerSetup): PlayerSetup => ({
  ...s,
  uid: `setup-${Date.now()}-${uidCounter++}`,
  name: `${s.name} (copy)`,
  equipment: { ...s.equipment },
});

export const displayName = (e: { name: string; version?: string }): string =>
  e.version && e.version !== '' ? `${e.name} (${e.version})` : e.name;

export const hasAttribute = (m: Monster, attribute: string): boolean =>
  (m.attributes ?? []).some((a) => a?.toLowerCase().startsWith(attribute.toLowerCase()));

/** Effective element for weakness/tome purposes; ancient spells map to elements. */
export const spellElement = (spell: SpellData | null): string | null => {
  if (!spell) return null;
  if (spell.name.includes('Smoke')) return 'air';
  if (spell.name.includes('Ice')) return 'water';
  if (spell.name.includes('Shadow')) return 'earth';
  if (spell.name.includes('Blood')) return 'fire';
  return spell.element;
};

export const isBindSpell = (spell: SpellData | null): boolean =>
  spell != null && ['Bind', 'Snare', 'Entangle'].includes(spell.name);

/** Standard elemental spells scale: highest same-class tier castable at the level. */
export const spellMaxHitAtLevel = (
  spell: SpellData,
  magicLevel: number,
  allSpells: SpellData[],
): number => {
  if (!spell.element || !spell.name.includes(' ')) return spell.max_hit;
  const spellClass = spell.name.split(' ')[1];
  const thresholds: Record<string, [number, number, number]> = {
    Strike: [13, 9, 5],
    Bolt: [35, 29, 23],
    Blast: [59, 53, 47],
    Wave: [75, 70, 65],
    Surge: [95, 90, 85],
  };
  const tiers = thresholds[spellClass];
  if (!tiers) return spell.max_hit;
  const elements = ['Fire', 'Earth', 'Water'];
  for (let i = 0; i < 3; i++) {
    if (magicLevel >= tiers[i]) {
      const target = `${elements[i]} ${spellClass}`;
      return allSpells.find((s) => s.name === target)?.max_hit ?? spell.max_hit;
    }
  }
  return allSpells.find((s) => s.name === `Wind ${spellClass}`)?.max_hit ?? spell.max_hit;
};
