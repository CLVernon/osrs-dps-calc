import type { AttackType, Monster, PlayerSetup } from '../model/types';
import { TOA_MONSTER_IDS } from './constants';
import type { Gear } from './gear';

export interface EquipmentTotals {
  str: number;
  rangedStr: number;
  /** Magic damage bonus in tenths of a percent */
  magicStrTenths: number;
  stab: number;
  slash: number;
  crush: number;
  ranged: number;
  magic: number;
}

/** Base max hit formula from effective level and a strength-style bonus. */
export const maxHitFromEffective = (effectiveLevel: number, strengthBonus: number): number =>
  Math.trunc((effectiveLevel * (strengthBonus + 64) + 320) / 640);

export const attackBonus = (t: EquipmentTotals, type: AttackType): number => t[type];

/**
 * Aggregated equipment bonuses with gear-level adjustments (Tumeken's shadow
 * tripling, Dinh's bulwark strength, Virtus + ancients, elite void mage,
 * Keris partisan of amascut penalty outside ToA).
 */
export const computeTotals = (p: PlayerSetup, m: Monster, g: Gear,
  spellbook: string | null): EquipmentTotals => {
  const t: EquipmentTotals = {
    str: 0, rangedStr: 0, magicStrTenths: 0,
    stab: 0, slash: 0, crush: 0, ranged: 0, magic: 0,
  };
  for (const item of Object.values(p.equipment)) {
    if (!item) continue;
    t.str += item.bonuses.str;
    t.rangedStr += item.bonuses.ranged_str;
    t.magicStrTenths += item.bonuses.magic_str;
    t.stab += item.offensive.stab;
    t.slash += item.offensive.slash;
    t.crush += item.offensive.crush;
    t.ranged += item.offensive.ranged;
    t.magic += item.offensive.magic;
  }

  // The shadow's multiplier does not apply when manually casting
  const manualCast = p.stance === 'Manual Cast';
  if (g.tumekensShadow() && !manualCast) {
    const factor = TOA_MONSTER_IDS.has(m.id) ? 4 : 3;
    t.magicStrTenths = Math.min(1000, t.magicStrTenths * factor);
    t.magic *= factor;
  }

  if (g.weaponName() === 'Keris partisan of amascut' && !TOA_MONSTER_IDS.has(m.id)) {
    t.str -= 22;
    t.stab -= 50;
  }

  if (g.wearing("Dinh's bulwark", "Dinh's blazing bulwark")) {
    let defenceSum = 0;
    for (const item of Object.values(p.equipment)) {
      if (!item) continue;
      defenceSum += item.defensive.stab + item.defensive.slash
        + item.defensive.crush + item.defensive.ranged;
    }
    t.str += Math.max(0, Math.trunc((defenceSum - 800) / 12) - 38);
  }

  const casting = p.attackType === 'magic'
    && (p.stance === 'Autocast' || p.stance === 'Defensive Autocast' || manualCast);
  if (spellbook === 'ancient' && casting) {
    t.magicStrTenths += 30 * g.virtusPieces();
  }

  if (g.eliteMagicVoid()) {
    t.magicStrTenths += 50;
  }

  return t;
};
