import { useState } from 'react';
import { useStore } from '../state/store';
import { fetchCharacter, GAME_MODES, type GameMode } from '../data/hiscores';
import { wikiImageUrl } from '../data/repository';
import type { PlayerCharacter } from '../model/types';

const SkillCell = ({ icon, title, value, onChange }: {
  icon: string; title: string; value: number; onChange: (v: number) => void;
}) => (
  <div className="skill-cell" title={title}>
    <img src={wikiImageUrl(icon)!} alt={title} />
    <input
      type="number"
      min={1}
      max={126}
      value={value}
      onChange={(e) => onChange(Math.max(0, parseInt(e.target.value, 10) || 0))}
    />
  </div>
);

export const CharacterPanel = () => {
  const character = useStore((s) => s.character);
  const setCharacter = useStore((s) => s.setCharacter);
  const [mode, setMode] = useState<GameMode>('regular');
  const [status, setStatus] = useState('');
  const [importing, setImporting] = useState(false);

  const patch = (p: Partial<PlayerCharacter>) => setCharacter({ ...character, ...p });

  const doImport = async () => {
    if (!character.name.trim()) {
      setStatus('Enter a username to import.');
      return;
    }
    setImporting(true);
    setStatus(`Importing ${character.name.trim()}...`);
    try {
      const imported = await fetchCharacter(character.name, mode);
      setCharacter(imported);
      setStatus(`Imported ${imported.name} from the ${mode.replaceAll('_', ' ')} hiscores.`);
    } catch (e) {
      setStatus(e instanceof Error ? e.message : 'Import failed');
    } finally {
      setImporting(false);
    }
  };

  return (
    <div>
      <div className="section-title">Character</div>
      <div className="row">
        <input
          type="text"
          className="grow"
          placeholder="Username"
          value={character.name}
          onChange={(e) => patch({ name: e.target.value })}
          onKeyDown={(e) => e.key === 'Enter' && doImport()}
        />
        <button onClick={doImport} disabled={importing}
          title="Import levels from the official OSRS hiscores">
          {importing ? '...' : '⭳ Import'}
        </button>
      </div>
      <div className="row">
        <select className="grow" value={mode} onChange={(e) => setMode(e.target.value as GameMode)}>
          {GAME_MODES.map((g) => <option key={g.id} value={g.id}>{g.label}</option>)}
        </select>
      </div>
      {status && <div className="subtle">{status}</div>}
      <div className="skill-grid" style={{ marginTop: 8 }}>
        <SkillCell icon="Attack icon.png" title="Attack" value={character.attack}
          onChange={(v) => patch({ attack: v })} />
        <SkillCell icon="Strength icon.png" title="Strength" value={character.strength}
          onChange={(v) => patch({ strength: v })} />
        <SkillCell icon="Defence icon.png" title="Defence" value={character.defence}
          onChange={(v) => patch({ defence: v })} />
        <SkillCell icon="Ranged icon.png" title="Ranged" value={character.ranged}
          onChange={(v) => patch({ ranged: v })} />
        <SkillCell icon="Magic icon.png" title="Magic" value={character.magic}
          onChange={(v) => patch({ magic: v })} />
        <SkillCell icon="Hitpoints icon.png" title="Hitpoints" value={character.hitpoints}
          onChange={(v) => patch({ hitpoints: v })} />
        <SkillCell icon="Hitpoints icon.png" title="Current HP (for Dharok's set)"
          value={character.currentHitpoints || character.hitpoints}
          onChange={(v) => patch({ currentHitpoints: v })} />
        <SkillCell icon="Mining icon.png" title="Mining (for CoX Guardians)" value={character.mining}
          onChange={(v) => patch({ mining: v })} />
      </div>
    </div>
  );
};
