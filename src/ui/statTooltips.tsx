import type { ReactNode } from 'react';
import type { EquipmentItem, Monster, SpellData } from '../model/types';
import { displayName, spellElement } from '../model/types';

const Value = ({ v, suffix = '' }: { v: number; suffix?: string }) => (
  <td className={v > 0 ? 'value-pos' : v < 0 ? 'value-neg' : 'value-zero'}>
    {v > 0 ? '+' : ''}{v}{suffix}
  </td>
);

export const equipmentTooltip = (item: EquipmentItem): ReactNode => (
  <div>
    <div className="title">{displayName(item)}</div>
    <table>
      <thead>
        <tr><th /><th>Stab</th><th>Slash</th><th>Crush</th><th>Magic</th><th>Ranged</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Attack</td>
          <Value v={item.offensive.stab} /><Value v={item.offensive.slash} />
          <Value v={item.offensive.crush} /><Value v={item.offensive.magic} />
          <Value v={item.offensive.ranged} />
        </tr>
        <tr>
          <td>Defence</td>
          <Value v={item.defensive.stab} /><Value v={item.defensive.slash} />
          <Value v={item.defensive.crush} /><Value v={item.defensive.magic} />
          <Value v={item.defensive.ranged} />
        </tr>
      </tbody>
    </table>
    <table>
      <tbody>
        <tr>
          <td>Melee str</td><Value v={item.bonuses.str} />
          <td>Ranged str</td><Value v={item.bonuses.ranged_str} />
        </tr>
        <tr>
          <td>Magic dmg</td><Value v={item.bonuses.magic_str / 10} suffix="%" />
          <td>Prayer</td><Value v={item.bonuses.prayer} />
        </tr>
      </tbody>
    </table>
    {item.slot === 'weapon' && (
      <div className="subtle">
        Speed {item.speed} ticks
        {item.category ? ` • ${item.category}` : ''}
        {item.isTwoHanded ? ' • two-handed' : ''}
      </div>
    )}
  </div>
);

export const monsterTooltip = (m: Monster): ReactNode => (
  <div>
    <div className="title">{displayName(m)}</div>
    <table>
      <tbody>
        <tr>
          <td>HP</td><td>{m.skills.hp}</td>
          <td>Defence</td><td>{m.skills.def}</td>
        </tr>
        <tr>
          <td>Magic</td><td>{m.skills.magic}</td>
          <td>Size</td><td>{m.size}</td>
        </tr>
      </tbody>
    </table>
    <table>
      <thead>
        <tr><th /><th>Stab</th><th>Slash</th><th>Crush</th><th>Magic</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Def bonus</td>
          <Value v={m.defensive.stab} /><Value v={m.defensive.slash} />
          <Value v={m.defensive.crush} /><Value v={m.defensive.magic} />
        </tr>
      </tbody>
    </table>
    <table>
      <thead>
        <tr><th /><th>Light</th><th>Standard</th><th>Heavy</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>Ranged def</td>
          <Value v={m.defensive.light} /><Value v={m.defensive.standard} />
          <Value v={m.defensive.heavy} />
        </tr>
      </tbody>
    </table>
    {m.defensive.flat_armour > 0 && <div>Flat armour {m.defensive.flat_armour}</div>}
    {m.attributes?.length > 0 && (
      <div className="subtle">Attributes: {m.attributes.join(', ')}</div>
    )}
    {m.weakness && m.weakness.element && m.weakness.element !== 'none' && (
      <div className="value-pos">
        Weak to {m.weakness.element} (+{m.weakness.severity}% severity)
      </div>
    )}
  </div>
);

export const spellTooltip = (spell: SpellData): ReactNode => (
  <div>
    <div className="title">{spell.name}</div>
    <div>Max hit {spell.max_hit}</div>
    <div className="subtle">
      {spell.spellbook} spellbook
      {spellElement(spell) ? ` • ${spellElement(spell)} element` : ''}
    </div>
  </div>
);
