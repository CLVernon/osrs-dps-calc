import { useMemo, useState } from 'react';
import { useStore } from '../state/store';
import { type Monster, displayName, hasAttribute } from '../model/types';
import { wikiImageUrl } from '../data/repository';
import { loadMonsterPresets, saveMonsterPreset } from '../data/presets';
import { SearchableDropdown } from './SearchableDropdown';
import { monsterTooltip } from './statTooltips';
import { scaleSuffix } from './shared';
import { MonsterEditorModal } from './MonsterEditorModal';

export const TargetsPanel = () => {
  const repo = useStore((s) => s.repo);
  const targets = useStore((s) => s.targets);
  const { addTarget, removeTarget, replaceTarget, setTargets } = useStore();
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);
  const [editing, setEditing] = useState<Monster | null>(null);
  const [showRaid, setShowRaid] = useState(false);

  const pickable = useMemo(() => {
    if (!repo) return [];
    const presets = Object.values(loadMonsterPresets());
    return [...presets, ...repo.monsters];
  }, [repo, targets]);

  const selected = selectedIndex != null ? targets[selectedIndex] : null;
  const coxTargets = targets.filter((m) => hasAttribute(m, 'xerician'));

  return (
    <div>
      <div className="section-title">Targets</div>
      <SearchableDropdown
        items={pickable}
        label={displayName}
        image={(m) => m.image}
        tooltip={monsterTooltip}
        placeholder="Type to add a target..."
        clearOnSelect
        onSelect={addTarget}
      />
      <div className="item-list">
        {targets.map((m, i) => (
          <div
            key={`${displayName(m)}-${i}`}
            className={`entry${i === selectedIndex ? ' selected' : ''}`}
            onClick={() => setSelectedIndex(i)}
          >
            {wikiImageUrl(m.image)
              ? <img src={wikiImageUrl(m.image)!} alt="" loading="lazy" />
              : <span style={{ width: 22 }} />}
            <span>{displayName(m)}{scaleSuffix(m)}</span>
          </div>
        ))}
      </div>
      <div className="row">
        <button title="Remove the selected target" disabled={!selected}
          onClick={() => {
            if (selectedIndex != null) {
              removeTarget(selectedIndex);
              setSelectedIndex(null);
            }
          }}>🗑</button>
        <button title="Edit / customise the selected target's stats" disabled={!selected}
          onClick={() => selected && setEditing(selected)}>✎</button>
        <button title="Save the selected target as a monster preset" disabled={!selected}
          onClick={() => selected && saveMonsterPreset(selected)}>💾</button>
        <button title="Raid scaling: apply party settings to all CoX targets"
          disabled={coxTargets.length === 0}
          onClick={() => setShowRaid(true)}>⚙ Raid</button>
      </div>

      {editing && selectedIndex != null && (
        <MonsterEditorModal
          monster={editing}
          onCancel={() => setEditing(null)}
          onSave={(m) => {
            replaceTarget(selectedIndex, m);
            setEditing(null);
          }}
        />
      )}

      {showRaid && (
        <RaidSettingsModal
          targets={targets}
          onCancel={() => setShowRaid(false)}
          onApply={(settings) => {
            setTargets(targets.map((m) => (hasAttribute(m, 'xerician')
              ? { ...m, ...settings }
              : m)));
            setShowRaid(false);
          }}
        />
      )}
    </div>
  );
};

interface RaidSettings {
  partySize: number;
  partyMaxCombatLevel: number;
  partyMaxHpLevel: number;
  partyAvgMiningLevel: number;
  coxChallengeMode: boolean;
}

const RaidSettingsModal = ({ targets, onApply, onCancel }: {
  targets: Monster[];
  onApply: (settings: RaidSettings) => void;
  onCancel: () => void;
}) => {
  const first = targets.find((m) => hasAttribute(m, 'xerician'));
  const [partySize, setPartySize] = useState(first?.partySize ?? 1);
  const [maxCombat, setMaxCombat] = useState(first?.partyMaxCombatLevel ?? 126);
  const [maxHp, setMaxHp] = useState(first?.partyMaxHpLevel ?? 99);
  const [avgMining, setAvgMining] = useState(first?.partyAvgMiningLevel ?? 99);
  const [cm, setCm] = useState(first?.coxChallengeMode ?? false);
  const count = targets.filter((m) => hasAttribute(m, 'xerician')).length;

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>CoX raid scaling</h3>
        <div className="subtle" style={{ marginBottom: 10 }}>
          Applies to all {count} CoX target(s)
        </div>
        <div className="form-grid">
          <label>Party size</label>
          <input type="number" min={1} max={100} value={partySize}
            onChange={(e) => setPartySize(parseInt(e.target.value, 10) || 1)} />
          <label>Highest combat level</label>
          <input type="number" min={3} max={126} value={maxCombat}
            onChange={(e) => setMaxCombat(parseInt(e.target.value, 10) || 126)} />
          <label>Highest Hitpoints</label>
          <input type="number" min={10} max={99} value={maxHp}
            onChange={(e) => setMaxHp(parseInt(e.target.value, 10) || 99)} />
          <label>Avg Mining (Guardians)</label>
          <input type="number" min={1} max={99} value={avgMining}
            onChange={(e) => setAvgMining(parseInt(e.target.value, 10) || 99)} />
          <label>Challenge Mode</label>
          <input type="checkbox" checked={cm} onChange={(e) => setCm(e.target.checked)} />
        </div>
        <div className="actions">
          <button onClick={onCancel}>Cancel</button>
          <button onClick={() => onApply({
            partySize,
            partyMaxCombatLevel: maxCombat,
            partyMaxHpLevel: maxHp,
            partyAvgMiningLevel: avgMining,
            coxChallengeMode: cm,
          })}>Apply</button>
        </div>
      </div>
    </div>
  );
};
