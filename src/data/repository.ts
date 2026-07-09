import type { EquipmentItem, Monster, SpellData } from '../model/types';
import { displayName } from '../model/types';

const REMOTE_BASE = 'https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/';

export interface Repository {
  equipment: EquipmentItem[];
  monsters: Monster[];
  spells: SpellData[];
  /** true when the latest data was fetched from the wiki dataset */
  fresh: boolean;
}

const loadFile = async <T>(name: string): Promise<{ data: T[]; fresh: boolean }> => {
  // Try the live dataset first (browser HTTP cache applies), fall back to bundled
  try {
    const res = await fetch(`${REMOTE_BASE}${name}`, { cache: 'default' });
    if (res.ok) {
      return { data: await res.json(), fresh: true };
    }
  } catch {
    // offline or blocked; fall through to bundled copy
  }
  const bundled = await fetch(`${import.meta.env.BASE_URL}data/${name}`);
  if (!bundled.ok) {
    throw new Error(`Could not load bundled data file ${name}`);
  }
  return { data: await bundled.json(), fresh: false };
};

export const loadRepository = async (): Promise<Repository> => {
  const [equipment, monsters, spells] = await Promise.all([
    loadFile<EquipmentItem>('equipment.json'),
    loadFile<Monster>('monsters.json'),
    loadFile<SpellData>('spells.json'),
  ]);
  const byName = (a: { name: string; version?: string }, b: { name: string; version?: string }) =>
    displayName(a).localeCompare(displayName(b), undefined, { sensitivity: 'base' });
  equipment.data.sort(byName);
  monsters.data.sort(byName);
  return {
    equipment: equipment.data,
    monsters: monsters.data,
    spells: spells.data,
    fresh: equipment.fresh && monsters.fresh && spells.fresh,
  };
};

export const equipmentForSlot = (repo: Repository, slot: string): EquipmentItem[] =>
  repo.equipment.filter((e) => e.slot === slot);

export const findEquipment = (repo: Repository, name: string): EquipmentItem | undefined =>
  repo.equipment.find((e) => displayName(e).toLowerCase() === name.toLowerCase());

export const findMonster = (repo: Repository, name: string): Monster | undefined =>
  repo.monsters.find((m) => displayName(m).toLowerCase() === name.toLowerCase());

export const findSpell = (repo: Repository, name: string): SpellData | undefined =>
  repo.spells.find((s) => s.name.toLowerCase() === name.toLowerCase());

/** Wiki image URL for an image file name from the dataset. */
export const wikiImageUrl = (imageName: string | null | undefined): string | null => {
  if (!imageName) return null;
  return `https://oldschool.runescape.wiki/images/${encodeURIComponent(imageName.replaceAll(' ', '_'))}`;
};
