import type { AttackType } from './types';

export interface Potion {
  id: string;
  label: string;
  image: string | null;
  styles: AttackType[] | 'all';
  attackBoost: (level: number) => number;
  strengthBoost: (level: number) => number;
  rangedBoost: (level: number) => number;
  magicBoost: (level: number) => number;
}

const none = () => 0;
const melee: AttackType[] = ['stab', 'slash', 'crush'];
const t = Math.trunc;

export const POTIONS: Potion[] = [
  { id: 'NONE', label: 'None', image: null, styles: 'all', attackBoost: none, strengthBoost: none, rangedBoost: none, magicBoost: none },
  { id: 'ATTACK_POTION', label: 'Attack potion (+3 +10%)', image: 'Attack potion(4).png', styles: melee, attackBoost: (l) => 3 + t(l / 10), strengthBoost: none, rangedBoost: none, magicBoost: none },
  { id: 'STRENGTH_POTION', label: 'Strength potion (+3 +10%)', image: 'Strength potion(4).png', styles: melee, attackBoost: none, strengthBoost: (l) => 3 + t(l / 10), rangedBoost: none, magicBoost: none },
  { id: 'COMBAT_POTION', label: 'Combat potion (+3 +10% Atk/Str)', image: 'Combat potion(4).png', styles: melee, attackBoost: (l) => 3 + t(l / 10), strengthBoost: (l) => 3 + t(l / 10), rangedBoost: none, magicBoost: none },
  { id: 'SUPER_ATTACK', label: 'Super attack (+5 +15%)', image: 'Super attack(4).png', styles: melee, attackBoost: (l) => 5 + t(l * 0.15), strengthBoost: none, rangedBoost: none, magicBoost: none },
  { id: 'SUPER_STRENGTH', label: 'Super strength (+5 +15%)', image: 'Super strength(4).png', styles: melee, attackBoost: none, strengthBoost: (l) => 5 + t(l * 0.15), rangedBoost: none, magicBoost: none },
  { id: 'SUPER_COMBAT', label: 'Super combat (+5 +15% Atk/Str)', image: 'Super combat potion(4).png', styles: melee, attackBoost: (l) => 5 + t(l * 0.15), strengthBoost: (l) => 5 + t(l * 0.15), rangedBoost: none, magicBoost: none },
  { id: 'RANGING_POTION', label: 'Ranging potion (+4 +10%)', image: 'Ranging potion(4).png', styles: ['ranged'], attackBoost: none, strengthBoost: none, rangedBoost: (l) => 4 + t(l / 10), magicBoost: none },
  { id: 'BASTION_POTION', label: 'Bastion potion (+4 +10% Ranged)', image: 'Bastion potion(4).png', styles: ['ranged'], attackBoost: none, strengthBoost: none, rangedBoost: (l) => 4 + t(l / 10), magicBoost: none },
  { id: 'MAGIC_POTION', label: 'Magic potion (+4)', image: 'Magic potion(4).png', styles: ['magic'], attackBoost: none, strengthBoost: none, rangedBoost: none, magicBoost: () => 4 },
  { id: 'IMBUED_HEART', label: 'Imbued heart (+1 +10% Magic)', image: 'Imbued heart.png', styles: ['magic'], attackBoost: none, strengthBoost: none, rangedBoost: none, magicBoost: (l) => 1 + t(l / 10) },
  { id: 'SATURATED_HEART', label: 'Saturated heart (+4 +10% Magic)', image: 'Saturated heart.png', styles: ['magic'], attackBoost: none, strengthBoost: none, rangedBoost: none, magicBoost: (l) => 4 + t(l / 10) },
  { id: 'SMELLING_SALTS', label: 'Smelling salts (+11 +16% all)', image: 'Smelling salts (2).png', styles: 'all', attackBoost: (l) => 11 + t(l * 0.16), strengthBoost: (l) => 11 + t(l * 0.16), rangedBoost: (l) => 11 + t(l * 0.16), magicBoost: (l) => 11 + t(l * 0.16) },
  { id: 'OVERLOAD_RAID', label: 'Overload (raids, +6 +16% all)', image: 'Overload (+)(4).png', styles: 'all', attackBoost: (l) => 6 + t(l * 0.16), strengthBoost: (l) => 6 + t(l * 0.16), rangedBoost: (l) => 6 + t(l * 0.16), magicBoost: (l) => 6 + t(l * 0.16) },
];

export const potionById = (id: string): Potion =>
  POTIONS.find((p) => p.id === id) ?? POTIONS[0];

export const potionsForType = (type: AttackType): Potion[] =>
  POTIONS.filter((p) => p.styles === 'all' || p.styles.includes(type));
