import type { PlayerCharacter } from '../model/types';

// The wiki hosts a CORS passthrough for the official hiscores
const PROXY = 'https://oldschool.runescape.wiki/cors';

export type GameMode = 'regular' | 'ironman' | 'hardcore_ironman' | 'ultimate';

export const GAME_MODES: { id: GameMode; label: string; endpoint: string; image: string | null }[] = [
  { id: 'regular', label: 'Regular', endpoint: 'hiscore_oldschool', image: null },
  { id: 'ironman', label: 'Ironman', endpoint: 'hiscore_oldschool_ironman', image: 'Ironman chat badge.png' },
  { id: 'hardcore_ironman', label: 'Hardcore Ironman', endpoint: 'hiscore_oldschool_hardcore_ironman', image: 'Hardcore ironman chat badge.png' },
  { id: 'ultimate', label: 'Ultimate Ironman', endpoint: 'hiscore_oldschool_ultimate', image: 'Ultimate ironman chat badge.png' },
];

// Skill line indices in the index_lite CSV (rank,level,xp per line)
const ATTACK = 1;
const DEFENCE = 2;
const STRENGTH = 3;
const HITPOINTS = 4;
const RANGED = 5;
const MAGIC = 7;
const MINING = 15;

const level = (lines: string[], index: number): number => {
  const parts = lines[index]?.split(',');
  if (!parts || parts.length < 2) {
    throw new Error('Unexpected hiscores response format');
  }
  const value = parseInt(parts[1], 10);
  if (Number.isNaN(value)) {
    throw new Error('Unexpected hiscores response format');
  }
  return Math.max(1, value); // unranked skills are -1
};

export const fetchCharacter = async (username: string, mode: GameMode): Promise<PlayerCharacter> => {
  const endpoint = GAME_MODES.find((g) => g.id === mode)?.endpoint ?? 'hiscore_oldschool';
  const url = `${PROXY}/m=${endpoint}/index_lite.ws?player=${encodeURIComponent(username.trim().replaceAll(' ', '_'))}`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Player "${username}" was not found on the ${mode} hiscores`);
  }
  const lines = (await res.text()).split(/\r?\n/);
  return {
    name: username.trim(),
    attack: level(lines, ATTACK),
    defence: level(lines, DEFENCE),
    strength: level(lines, STRENGTH),
    hitpoints: Math.max(10, level(lines, HITPOINTS)),
    ranged: level(lines, RANGED),
    magic: level(lines, MAGIC),
    mining: level(lines, MINING),
    currentHitpoints: 0,
  };
};
