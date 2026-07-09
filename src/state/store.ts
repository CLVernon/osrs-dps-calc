import { create } from 'zustand';
import type { Monster, PlayerCharacter, PlayerSetup } from '../model/types';
import { copySetup, newSetup } from '../model/types';
import type { Repository } from '../data/repository';
import { findMonster } from '../data/repository';
import { loadCharacter, saveCharacter } from '../data/presets';

export interface AppState {
  repo: Repository | null;
  dataStatus: string;
  character: PlayerCharacter;
  setups: PlayerSetup[];
  selectedSetupUid: string | null;
  targets: Monster[];

  setRepo: (repo: Repository) => void;
  setCharacter: (character: PlayerCharacter) => void;
  addSetup: () => void;
  addLoadedSetup: (setup: PlayerSetup) => void;
  duplicateSetup: (uid: string) => void;
  removeSetup: (uid: string) => void;
  selectSetup: (uid: string) => void;
  updateSetup: (uid: string, patch: Partial<PlayerSetup>) => void;
  addTarget: (monster: Monster) => void;
  removeTarget: (index: number) => void;
  replaceTarget: (index: number, monster: Monster) => void;
  setTargets: (targets: Monster[]) => void;
}

export const useStore = create<AppState>((set, get) => ({
  repo: null,
  dataStatus: 'Loading wiki data...',
  character: loadCharacter(),
  setups: [newSetup('Setup 1')],
  selectedSetupUid: null,
  targets: [],

  setRepo: (repo) => {
    const targets: Monster[] = [];
    const zulrah = findMonster(repo, 'Zulrah (Serpentine)');
    if (zulrah) targets.push(zulrah);
    set((state) => ({
      repo,
      targets: state.targets.length > 0 ? state.targets : targets,
      dataStatus: repo.fresh ? 'Using latest wiki data' : 'Using bundled wiki data (offline)',
      selectedSetupUid: state.selectedSetupUid ?? state.setups[0]?.uid ?? null,
    }));
  },

  setCharacter: (character) => {
    saveCharacter(character);
    set({ character });
  },

  addSetup: () => {
    const setup = newSetup(`Setup ${get().setups.length + 1}`);
    set((state) => ({ setups: [...state.setups, setup], selectedSetupUid: setup.uid }));
  },

  addLoadedSetup: (setup) => {
    set((state) => ({ setups: [...state.setups, setup], selectedSetupUid: setup.uid }));
  },

  duplicateSetup: (uid) => {
    const original = get().setups.find((s) => s.uid === uid);
    if (!original) return;
    const copy = copySetup(original);
    set((state) => ({ setups: [...state.setups, copy], selectedSetupUid: copy.uid }));
  },

  removeSetup: (uid) => {
    set((state) => {
      const setups = state.setups.filter((s) => s.uid !== uid);
      return {
        setups,
        selectedSetupUid: state.selectedSetupUid === uid
          ? setups[0]?.uid ?? null
          : state.selectedSetupUid,
      };
    });
  },

  selectSetup: (uid) => set({ selectedSetupUid: uid }),

  updateSetup: (uid, patch) => {
    set((state) => ({
      setups: state.setups.map((s) => (s.uid === uid ? { ...s, ...patch } : s)),
    }));
  },

  addTarget: (monster) => {
    set((state) => (state.targets.includes(monster)
      ? state
      : { targets: [...state.targets, monster] }));
  },

  removeTarget: (index) => {
    set((state) => ({ targets: state.targets.filter((_, i) => i !== index) }));
  },

  replaceTarget: (index, monster) => {
    set((state) => ({ targets: state.targets.map((m, i) => (i === index ? monster : m)) }));
  },

  setTargets: (targets) => set({ targets }),
}));
