import type { AttackType } from './types';

export interface Prayer {
  id: string;
  label: string;
  /** Accuracy multiplier as percent (120 = x1.20), applied with truncation */
  accuracyPercent: number;
  /** Strength/damage multiplier as percent */
  strengthPercent: number;
  /** Magic damage bonus in tenths of a percent (Augury = 40 = +4%) */
  magicDamageTenths: number;
  image: string | null;
  styles: AttackType[] | 'all';
}

const melee: AttackType[] = ['stab', 'slash', 'crush'];

export const PRAYERS: Prayer[] = [
  { id: 'NONE', label: 'None', accuracyPercent: 100, strengthPercent: 100, magicDamageTenths: 0, image: null, styles: 'all' },
  { id: 'TIER_1_MELEE', label: 'Clarity of Thought + Burst of Strength (5%)', accuracyPercent: 105, strengthPercent: 105, magicDamageTenths: 0, image: 'Burst of Strength.png', styles: melee },
  { id: 'TIER_2_MELEE', label: 'Improved Reflexes + Superhuman Strength (10%)', accuracyPercent: 110, strengthPercent: 110, magicDamageTenths: 0, image: 'Superhuman Strength.png', styles: melee },
  { id: 'TIER_3_MELEE', label: 'Incredible Reflexes + Ultimate Strength (15%)', accuracyPercent: 115, strengthPercent: 115, magicDamageTenths: 0, image: 'Ultimate Strength.png', styles: melee },
  { id: 'CHIVALRY', label: 'Chivalry (15% acc / 18% str)', accuracyPercent: 115, strengthPercent: 118, magicDamageTenths: 0, image: 'Chivalry.png', styles: melee },
  { id: 'PIETY', label: 'Piety (20% acc / 23% str)', accuracyPercent: 120, strengthPercent: 123, magicDamageTenths: 0, image: 'Piety.png', styles: melee },
  { id: 'SHARP_EYE', label: 'Sharp Eye (5%)', accuracyPercent: 105, strengthPercent: 105, magicDamageTenths: 0, image: 'Sharp Eye.png', styles: ['ranged'] },
  { id: 'HAWK_EYE', label: 'Hawk Eye (10%)', accuracyPercent: 110, strengthPercent: 110, magicDamageTenths: 0, image: 'Hawk Eye.png', styles: ['ranged'] },
  { id: 'EAGLE_EYE', label: 'Eagle Eye (15%)', accuracyPercent: 115, strengthPercent: 115, magicDamageTenths: 0, image: 'Eagle Eye.png', styles: ['ranged'] },
  { id: 'DEADEYE', label: 'Deadeye (18%)', accuracyPercent: 118, strengthPercent: 118, magicDamageTenths: 0, image: 'Deadeye.png', styles: ['ranged'] },
  { id: 'RIGOUR', label: 'Rigour (20% acc / 23% str)', accuracyPercent: 120, strengthPercent: 123, magicDamageTenths: 0, image: 'Rigour.png', styles: ['ranged'] },
  { id: 'MYSTIC_WILL', label: 'Mystic Will (5%)', accuracyPercent: 105, strengthPercent: 100, magicDamageTenths: 0, image: 'Mystic Will.png', styles: ['magic'] },
  { id: 'MYSTIC_LORE', label: 'Mystic Lore (10% acc / +1% dmg)', accuracyPercent: 110, strengthPercent: 100, magicDamageTenths: 10, image: 'Mystic Lore.png', styles: ['magic'] },
  { id: 'MYSTIC_MIGHT', label: 'Mystic Might (15% acc / +2% dmg)', accuracyPercent: 115, strengthPercent: 100, magicDamageTenths: 20, image: 'Mystic Might.png', styles: ['magic'] },
  { id: 'MYSTIC_VIGOUR', label: 'Mystic Vigour (18% acc / +3% dmg)', accuracyPercent: 118, strengthPercent: 100, magicDamageTenths: 30, image: 'Mystic Vigour.png', styles: ['magic'] },
  { id: 'AUGURY', label: 'Augury (25% acc / +4% dmg)', accuracyPercent: 125, strengthPercent: 100, magicDamageTenths: 40, image: 'Augury.png', styles: ['magic'] },
];

export const prayerById = (id: string): Prayer =>
  PRAYERS.find((p) => p.id === id) ?? PRAYERS[0];

export const prayersForType = (type: AttackType): Prayer[] =>
  PRAYERS.filter((p) => p.styles === 'all' || p.styles.includes(type));

export const applyPrayerAccuracy = (p: Prayer, level: number): number =>
  Math.trunc((level * p.accuracyPercent) / 100);

export const applyPrayerStrength = (p: Prayer, level: number): number =>
  Math.trunc((level * p.strengthPercent) / 100);
