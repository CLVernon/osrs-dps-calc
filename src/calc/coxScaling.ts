import { type Monster, hasAttribute } from '../model/types';
import * as C from './constants';

// Chambers of Xeric monster scaling, ported from the wiki DPS tool

const CM_SCALE_PERCENT = 50;
const t = Math.trunc;

const addPercent = (value: number, percent: number): number =>
  value + t((value * percent) / 100);

const iSqrt = (value: number): number => Math.floor(Math.sqrt(value));

interface SkillMeta {
  scaleAtk: boolean;
  scaleStr: boolean;
  scaleRanged: boolean;
  scaleMagic: boolean;
  magicIsDefensive: boolean;
  scaleDef: boolean;
  baseOffensive: number;
  baseDefensive: number;
  baseHp: number;
}

const inputs = (m: Monster) => ({
  partySize: m.partySize ?? 1,
  maxCombat: m.partyMaxCombatLevel ?? 126,
  maxHp: m.partyMaxHpLevel ?? 99,
  avgMining: m.partyAvgMiningLevel ?? 99,
  cm: m.coxChallengeMode ?? false,
});

const skillMeta = (m: Monster): SkillMeta => {
  const magicIsDefensive = C.COX_MAGIC_IS_DEFENSIVE_IDS.has(m.id);
  const { partySize, avgMining } = inputs(m);

  const scaleAtk = m.skills.atk !== 1;
  const scaleStr = m.skills.str !== 1;
  const scaleRanged = m.skills.ranged !== 1;
  const scaleMagic = m.skills.magic !== 1;
  const scaleDef = m.skills.def !== 1;

  let baseOffensive = 1;
  if (scaleAtk) baseOffensive = Math.max(baseOffensive, m.skills.atk);
  if (scaleStr) baseOffensive = Math.max(baseOffensive, m.skills.str);
  if (scaleRanged) baseOffensive = Math.max(baseOffensive, m.skills.ranged);
  if (scaleMagic && !magicIsDefensive) baseOffensive = Math.max(baseOffensive, m.skills.magic);

  let baseDefensive = 1;
  if (scaleDef) baseDefensive = Math.max(baseDefensive, m.skills.def);
  if (scaleMagic && magicIsDefensive) baseDefensive = Math.max(baseDefensive, m.skills.magic);

  const baseHp = C.GUARDIAN_IDS.has(m.id)
    ? 151 + t((avgMining * Math.max(1, partySize)) / Math.max(1, partySize))
    : m.skills.hp;

  return {
    scaleAtk, scaleStr, scaleRanged, scaleMagic, magicIsDefensive, scaleDef,
    baseOffensive, baseDefensive, baseHp,
  };
};

const applyStats = (m: Monster, meta: SkillMeta, hp: number, offensive: number,
  defensive: number): void => {
  m.skills.hp = hp;
  if (meta.scaleAtk) m.skills.atk = offensive;
  if (meta.scaleStr) m.skills.str = offensive;
  if (meta.scaleRanged) m.skills.ranged = offensive;
  if (meta.scaleMagic) m.skills.magic = meta.magicIsDefensive ? defensive : offensive;
  if (meta.scaleDef) m.skills.def = defensive;
};

const copyWithSkills = (m: Monster): Monster => ({ ...m, skills: { ...m.skills } });

const applySinglesScaling = (m: Monster): Monster => {
  const meta = skillMeta(m);
  const { maxCombat, maxHp, cm } = inputs(m);

  let hpScaler = Math.max(Math.min(maxCombat, 126), 60);
  let statScaler = Math.max(Math.min(maxHp, 99), 55);
  if (cm) {
    statScaler = addPercent(statScaler, CM_SCALE_PERCENT);
    hpScaler = addPercent(hpScaler, CM_SCALE_PERCENT);
  }

  const hp = Math.max(t((meta.baseHp * hpScaler) / 126), 5);
  const offensive = Math.max(t((meta.baseOffensive * statScaler) / 99), 1);
  const defensive = Math.max(t((meta.baseDefensive * statScaler) / 99), 1);
  applyStats(m, meta, hp, offensive, defensive);
  return m;
};

const applyMultiScaling = (m: Monster): Monster => {
  const meta = skillMeta(m);
  const { cm } = inputs(m);

  const partySize = Math.min(Math.max(inputs(m).partySize, 1), 100);
  const partySizeM1 = partySize - 1;
  const highestCombatLevel = Math.max(Math.min(inputs(m).maxCombat, 126), 60);
  const highestHp = Math.max(Math.min(55 + t((44 * inputs(m).maxHp) / 99), 99), 55);

  let offensive = t((meta.baseOffensive * highestHp) / 99);
  let defensive = t((meta.baseDefensive * highestHp) / 99);
  let hp = t((meta.baseHp * highestCombatLevel) / 126);

  const offensiveScalePct = 100 + iSqrt(partySizeM1) * 7 + partySizeM1;
  offensive = t((offensive * offensiveScalePct) / 100);

  const defensiveScalePct = 100 + iSqrt(partySizeM1) + t((partySizeM1 * 7) / 10);
  defensive = t((defensive * defensiveScalePct) / 100);

  hp += hp * t((partySize * 50) / 100);

  if (cm) {
    offensive = addPercent(offensive, CM_SCALE_PERCENT);
    const glowingCrystal = C.GLOWING_CRYSTAL_IDS.has(m.id);
    if (!glowingCrystal) {
      hp = addPercent(hp, CM_SCALE_PERCENT);
    }
    if (glowingCrystal) {
      // defence not scaled
    } else if (C.TEKTON_IDS.has(m.id)) {
      defensive = addPercent(defensive, partySize < 4 ? 20 : 35);
    } else {
      defensive = addPercent(defensive, CM_SCALE_PERCENT);
    }
  }

  hp = Math.max(Math.min(hp, 30000), 50);
  offensive = Math.max(Math.min(offensive, 5000), 50);
  defensive = Math.max(Math.min(defensive, 20000), 50);
  applyStats(m, meta, hp, offensive, defensive);
  return m;
};

const applyOlmScaling = (m: Monster): Monster => {
  const meleeHand = C.OLM_MELEE_HAND_IDS.has(m.id);
  const mageHand = C.OLM_MAGE_HAND_IDS.has(m.id);
  const { partySize } = inputs(m);

  const partySizeScaleFactor = Math.min(partySize - 1, 50) - 3 * t(Math.min(partySize, 50) / 8);

  const scaled = applyMultiScaling(m);
  if (mageHand) {
    scaled.skills.magic = t(scaled.skills.magic / 2);
  }
  scaled.skills.hp = meleeHand || mageHand
    ? 600 + 300 * partySizeScaleFactor
    : 800 + 400 * partySizeScaleFactor;
  return scaled;
};

/** Returns a scaled copy for Xerician monsters, or the monster unchanged. */
export const scaleCoxMonster = (m: Monster): Monster => {
  if (!hasAttribute(m, 'xerician')) return m;
  if (C.COX_SINGLES_SCALING_IDS.has(m.id)) return applySinglesScaling(copyWithSkills(m));
  if (C.OLM_IDS.has(m.id)) return applyOlmScaling(copyWithSkills(m));
  return applyMultiScaling(copyWithSkills(m));
};
