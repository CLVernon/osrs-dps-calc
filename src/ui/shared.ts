import type { Monster } from '../model/types';

/** Neutral monster used when computing display-only equipment totals. */
export const dummyTotalsMonster: Monster = {
  id: -999,
  name: 'Totals dummy',
  version: '',
  image: '',
  level: 1,
  speed: 4,
  size: 1,
  skills: { atk: 1, def: 1, hp: 1, magic: 1, ranged: 1, str: 1 },
  offensive: { atk: 0, magic: 0, magic_str: 0, ranged: 0, ranged_str: 0, str: 0 },
  defensive: {
    flat_armour: 0, crush: 0, magic: 0, heavy: 0, standard: 0, light: 0, slash: 0, stab: 0,
  },
  attributes: [],
  weakness: null,
};

export const formatSeconds = (seconds: number): string => {
  if (!Number.isFinite(seconds)) return '∞';
  if (seconds >= 120) {
    return `${Math.trunc(seconds / 60)}m ${String(Math.trunc(seconds % 60)).padStart(2, '0')}s`;
  }
  return `${seconds.toFixed(1)}s`;
};

export const scaleSuffix = (m: Monster): string => {
  if ((m.attributes ?? []).includes('xerician')
    && (((m.partySize ?? 1) > 1) || m.coxChallengeMode)) {
    return ` [${m.partySize ?? 1}p${m.coxChallengeMode ? ' CM' : ''}]`;
  }
  if ((m.toaInvocationLevel ?? 0) > 0) {
    return ` [${m.toaInvocationLevel} inv]`;
  }
  return '';
};
