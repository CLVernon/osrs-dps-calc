import type { Monster, PlayerCharacter, PlayerSetup } from '../model/types';
import { defaultCharacter, displayName, newSetup } from '../model/types';
import type { Repository } from './repository';
import { findEquipment } from './repository';
import { EQUIPMENT_SLOTS } from '../model/types';

// localStorage-backed persistence for the character and presets

const CHARACTER_KEY = 'osrs-dps.character';
const PLAYER_PRESETS_KEY = 'osrs-dps.playerPresets';
const MONSTER_PRESETS_KEY = 'osrs-dps.monsterPresets';
const SESSION_KEY = 'osrs-dps.session';

export interface PlayerPresetDto {
  name: string;
  equipment: Record<string, string>;
  styleName: string;
  attackType: string;
  stance: string;
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
  soulreaperStacks?: number;
}

const read = <T>(key: string, fallback: T): T => {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
};

const write = (key: string, value: unknown): void => {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (e) {
    console.error('Could not save to localStorage', e);
  }
};

// --- Working session (auto-saved live state) ---

export interface Session {
  setups: PlayerSetup[];
  targets: Monster[];
  selectedSetupUid: string | null;
}

export const loadSession = (): Session | null => {
  const s = read<Session | null>(SESSION_KEY, null);
  if (!s || !Array.isArray(s.setups) || !Array.isArray(s.targets)) return null;
  return s;
};

export const saveSession = (session: Session): void => write(SESSION_KEY, session);

// --- Character ---

export const loadCharacter = (): PlayerCharacter =>
  ({ ...defaultCharacter(), ...read<Partial<PlayerCharacter>>(CHARACTER_KEY, {}) });

export const saveCharacter = (character: PlayerCharacter): void =>
  write(CHARACTER_KEY, character);

// --- Player presets ---

export const setupToDto = (s: PlayerSetup): PlayerPresetDto => {
  const equipment: Record<string, string> = {};
  for (const slot of EQUIPMENT_SLOTS) {
    const item = s.equipment[slot];
    if (item) equipment[slot] = displayName(item);
  }
  return {
    name: s.name,
    equipment,
    styleName: s.styleName,
    attackType: s.attackType,
    stance: s.stance,
    prayerId: s.prayerId,
    potionId: s.potionId,
    spellName: s.spellName,
    onSlayerTask: s.onSlayerTask,
    inWilderness: s.inWilderness,
    forinthrySurge: s.forinthrySurge,
    markOfDarkness: s.markOfDarkness,
    chargeSpell: s.chargeSpell,
    kandarinDiary: s.kandarinDiary,
    sunfireRunes: s.sunfireRunes,
    chinchompaDistance: s.chinchompaDistance,
    soulreaperStacks: s.soulreaperStacks,
  };
};

export const dtoToSetup = (dto: PlayerPresetDto, repo: Repository): PlayerSetup => {
  const setup = newSetup(dto.name || 'Unnamed');
  for (const slot of EQUIPMENT_SLOTS) {
    const itemName = dto.equipment?.[slot];
    if (itemName) {
      const item = findEquipment(repo, itemName);
      if (item) setup.equipment[slot] = item;
    }
  }
  setup.styleName = dto.styleName ?? setup.styleName;
  setup.attackType = (dto.attackType as PlayerSetup['attackType']) ?? setup.attackType;
  setup.stance = (dto.stance as PlayerSetup['stance']) ?? setup.stance;
  setup.prayerId = dto.prayerId ?? 'NONE';
  setup.potionId = dto.potionId ?? 'NONE';
  setup.spellName = dto.spellName ?? null;
  setup.onSlayerTask = !!dto.onSlayerTask;
  setup.inWilderness = !!dto.inWilderness;
  setup.forinthrySurge = !!dto.forinthrySurge;
  setup.markOfDarkness = !!dto.markOfDarkness;
  setup.chargeSpell = !!dto.chargeSpell;
  setup.kandarinDiary = !!dto.kandarinDiary;
  setup.sunfireRunes = !!dto.sunfireRunes;
  setup.chinchompaDistance = dto.chinchompaDistance ?? 5;
  setup.soulreaperStacks = dto.soulreaperStacks ?? 0;
  return setup;
};

export const loadPlayerPresets = (): Record<string, PlayerPresetDto> =>
  read(PLAYER_PRESETS_KEY, {});

export const savePlayerPreset = (setup: PlayerSetup): void => {
  const presets = loadPlayerPresets();
  presets[setup.name] = setupToDto(setup);
  write(PLAYER_PRESETS_KEY, presets);
};

export const deletePlayerPreset = (name: string): void => {
  const presets = loadPlayerPresets();
  delete presets[name];
  write(PLAYER_PRESETS_KEY, presets);
};

// --- Monster presets ---

export const loadMonsterPresets = (): Record<string, Monster> =>
  read(MONSTER_PRESETS_KEY, {});

export const saveMonsterPreset = (monster: Monster): void => {
  const presets = loadMonsterPresets();
  presets[displayName(monster)] = monster;
  write(MONSTER_PRESETS_KEY, presets);
};

export const deleteMonsterPreset = (name: string): void => {
  const presets = loadMonsterPresets();
  delete presets[name];
  write(MONSTER_PRESETS_KEY, presets);
};
