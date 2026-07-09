import { useState } from 'react';
import type { Monster } from '../model/types';

const ATTRIBUTES = [
  'undead', 'demon', 'dragon', 'kalphite', 'leafy', 'golem', 'rat', 'shade',
  'fiery', 'flying', 'xerician', 'vampyre1', 'vampyre2', 'vampyre3',
];

const ELEMENTS = ['none', 'air', 'water', 'earth', 'fire'];

/** Edits a copy of the monster's combat stats. */
export const MonsterEditorModal = ({ monster, onSave, onCancel }: {
  monster: Monster;
  onSave: (m: Monster) => void;
  onCancel: () => void;
}) => {
  const [m, setM] = useState<Monster>(() => ({
    ...monster,
    skills: { ...monster.skills },
    defensive: { ...monster.defensive },
    attributes: [...(monster.attributes ?? [])],
    weakness: monster.weakness ? { ...monster.weakness } : null,
  }));

  const num = (value: number, apply: (v: number) => void) => (
    <input type="number" value={value}
      onChange={(e) => apply(parseInt(e.target.value, 10) || 0)} />
  );

  const patchSkills = (p: Partial<Monster['skills']>) =>
    setM((prev) => ({ ...prev, skills: { ...prev.skills, ...p } }));
  const patchDef = (p: Partial<Monster['defensive']>) =>
    setM((prev) => ({ ...prev, defensive: { ...prev.defensive, ...p } }));

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h3>Edit monster</h3>
        <div className="form-grid">
          <label>Name</label>
          <input type="text" value={m.name}
            onChange={(e) => setM({ ...m, name: e.target.value, version: '' })} />
          <label>Hitpoints</label>
          {num(m.skills.hp, (v) => patchSkills({ hp: v }))}
          <label>Defence level</label>
          {num(m.skills.def, (v) => patchSkills({ def: v }))}
          <label>Magic level</label>
          {num(m.skills.magic, (v) => patchSkills({ magic: v }))}
          <label>Size (tiles)</label>
          {num(m.size, (v) => setM({ ...m, size: Math.max(1, v) }))}
          <label>Def: Stab</label>
          {num(m.defensive.stab, (v) => patchDef({ stab: v }))}
          <label>Def: Slash</label>
          {num(m.defensive.slash, (v) => patchDef({ slash: v }))}
          <label>Def: Crush</label>
          {num(m.defensive.crush, (v) => patchDef({ crush: v }))}
          <label>Def: Magic</label>
          {num(m.defensive.magic, (v) => patchDef({ magic: v }))}
          <label>Def: Ranged (light)</label>
          {num(m.defensive.light, (v) => patchDef({ light: v }))}
          <label>Def: Ranged (standard)</label>
          {num(m.defensive.standard, (v) => patchDef({ standard: v }))}
          <label>Def: Ranged (heavy)</label>
          {num(m.defensive.heavy, (v) => patchDef({ heavy: v }))}
          <label>Flat armour</label>
          {num(m.defensive.flat_armour, (v) => patchDef({ flat_armour: v }))}
          <label>ToA invocation</label>
          {num(m.toaInvocationLevel ?? 0, (v) => setM({ ...m, toaInvocationLevel: v }))}
          <label>Weakness</label>
          <div className="row">
            <select
              value={m.weakness?.element ?? 'none'}
              onChange={(e) => setM({
                ...m,
                weakness: e.target.value === 'none'
                  ? null
                  : { element: e.target.value, severity: m.weakness?.severity ?? 50 },
              })}
            >
              {ELEMENTS.map((el) => <option key={el} value={el}>{el}</option>)}
            </select>
            {m.weakness && num(m.weakness.severity, (v) => setM({
              ...m, weakness: { ...m.weakness!, severity: v },
            }))}
          </div>
        </div>
        <div className="section-title">Attributes</div>
        <div className="buff-grid">
          {ATTRIBUTES.map((attr) => (
            <label key={attr}>
              <input
                type="checkbox"
                checked={m.attributes.includes(attr)}
                onChange={(e) => setM({
                  ...m,
                  attributes: e.target.checked
                    ? [...m.attributes, attr]
                    : m.attributes.filter((a) => a !== attr),
                })}
              />
              {attr}
            </label>
          ))}
        </div>
        <div className="actions">
          <button onClick={onCancel}>Cancel</button>
          <button onClick={() => onSave(m)}>Save</button>
        </div>
      </div>
    </div>
  );
};
