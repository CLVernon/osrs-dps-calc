import { useState } from 'react';
import { useStore } from '../state/store';
import { wikiImageUrl } from '../data/repository';
import {
  dtoToSetup, loadPlayerPresets, savePlayerPreset,
} from '../data/presets';

export const SetupsPanel = () => {
  const setups = useStore((s) => s.setups);
  const selectedUid = useStore((s) => s.selectedSetupUid);
  const repo = useStore((s) => s.repo);
  const { addSetup, duplicateSetup, removeSetup, selectSetup, addLoadedSetup } = useStore();
  const [showLoad, setShowLoad] = useState(false);

  const selected = setups.find((s) => s.uid === selectedUid);
  const presets = loadPlayerPresets();
  const presetNames = Object.keys(presets).sort();

  return (
    <div>
      <div className="section-title">Gear setups</div>
      <div className="item-list">
        {setups.map((s) => {
          const weaponImage = s.equipment.weapon ? wikiImageUrl(s.equipment.weapon.image) : null;
          return (
            <div
              key={s.uid}
              className={`entry${s.uid === selectedUid ? ' selected' : ''}`}
              onClick={() => selectSetup(s.uid)}
            >
              {weaponImage ? <img src={weaponImage} alt="" /> : <span style={{ width: 22 }} />}
              <span>{s.name}</span>
            </div>
          );
        })}
      </div>
      <div className="row">
        <button title="Add a new gear setup" onClick={addSetup}>＋</button>
        <button title="Duplicate the selected setup" disabled={!selected}
          onClick={() => selected && duplicateSetup(selected.uid)}>⧉</button>
        <button title="Remove the selected setup" disabled={!selected}
          onClick={() => selected && removeSetup(selected.uid)}>🗑</button>
        <button title="Save the selected setup as a preset" disabled={!selected}
          onClick={() => selected && (savePlayerPreset(selected), selectSetup(selected.uid))}>💾</button>
        <button title="Load a saved gear preset" disabled={presetNames.length === 0}
          onClick={() => setShowLoad((v) => !v)}>📂</button>
      </div>
      {showLoad && (
        <div className="item-list">
          {presetNames.map((name) => (
            <div key={name} className="entry" onClick={() => {
              if (repo) {
                addLoadedSetup(dtoToSetup(presets[name], repo));
                setShowLoad(false);
              }
            }}>
              <span>{name}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
