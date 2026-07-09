import { useEffect, useMemo, useState } from 'react';
import { useStore } from '../state/store';
import {
  type CombatStyle, type EquipmentItem, type EquipmentSlotName,
  type PlayerSetup,
} from '../model/types';
import { stylesForWeapon } from '../model/weaponStyles';
import { prayersForType } from '../model/prayers';
import { potionsForType } from '../model/potions';
import { equipmentForSlot, wikiImageUrl } from '../data/repository';
import { SearchableDropdown } from './SearchableDropdown';
import { SlotPicker } from './SlotPicker';
import { Tooltip } from './Tooltip';
import { equipmentTooltip, spellTooltip } from './statTooltips';
import { computeTotals } from '../calc/equipmentTotals';
import { Gear } from '../calc/gear';
import { attackSpeedTicks } from '../calc/calc';
import { dummyTotalsMonster } from './shared';

const SLOT_PLACEHOLDERS: Record<EquipmentSlotName, string> = {
  head: 'Head slot.png',
  cape: 'Cape slot.png',
  neck: 'Neck slot.png',
  ammo: 'Ammo slot.png',
  weapon: 'Weapon slot.png',
  shield: 'Shield slot.png',
  body: 'Body slot.png',
  legs: 'Legs slot.png',
  hands: 'Hands slot.png',
  feet: 'Feet slot.png',
  ring: 'Ring slot.png',
};

// OSRS equipment tab layout: [col, row] per slot
const SLOT_LAYOUT: [EquipmentSlotName | null, EquipmentSlotName | null, EquipmentSlotName | null][] = [
  [null, 'head', null],
  ['cape', 'neck', 'ammo'],
  ['weapon', 'body', 'shield'],
  [null, 'legs', null],
  ['hands', 'feet', 'ring'],
];

const BUFFS: { key: keyof PlayerSetup; label: string; icon: string; tip: string }[] = [
  { key: 'onSlayerTask', label: 'On Slayer task', icon: 'Slayer icon.png', tip: 'Enables black mask / slayer helmet bonuses' },
  { key: 'inWilderness', label: 'In Wilderness', icon: 'Skull (status) icon.png', tip: 'Enables revenant (wilderness) weapon bonuses' },
  { key: 'forinthrySurge', label: 'Forinthry Surge', icon: 'Skull (status) icon.png', tip: 'Boosts the Amulet of avarice bonus vs revenants' },
  { key: 'markOfDarkness', label: 'Mark of Darkness', icon: 'Mark of Darkness.png', tip: 'Boosts demonbane spells (accuracy + 25% damage)' },
  { key: 'chargeSpell', label: 'Charge', icon: 'Charge.png', tip: '+10 max hit to god spells with the matching cape' },
  { key: 'kandarinDiary', label: 'Kandarin diary', icon: 'Achievement Diaries icon.png', tip: '+10% enchanted bolt proc chance' },
  { key: 'sunfireRunes', label: 'Sunfire runes', icon: 'Sunfire rune.png', tip: 'Fire spells: minimum hit of 10% of max' },
];

const Value = ({ v, suffix = '' }: { v: number; suffix?: string }) => (
  <td className={v > 0 ? 'value-pos' : v < 0 ? 'value-neg' : 'value-zero'}>
    {v > 0 ? '+' : ''}{v}{suffix}
  </td>
);

export const SetupEditor = () => {
  const repo = useStore((s) => s.repo);
  const setups = useStore((s) => s.setups);
  const selectedUid = useStore((s) => s.selectedSetupUid);
  const updateSetup = useStore((s) => s.updateSetup);
  const [picking, setPicking] = useState<{ slot: EquipmentSlotName; anchor: DOMRect } | null>(null);

  const setup = setups.find((s) => s.uid === selectedUid);

  const styles = useMemo(
    () => (setup ? stylesForWeapon(setup.equipment.weapon) : []),
    [setup?.equipment.weapon],
  );

  const currentStyle = setup
    ? (styles.find((st) => st.name === setup.styleName
        && st.type === setup.attackType && st.stance === setup.stance)
      ?? styles.find((st) => st.type === setup.attackType && st.stance !== 'Manual Cast')
      ?? styles[0])
    : undefined;

  // Keep the stored style consistent when the weapon changes
  useEffect(() => {
    if (!setup || !currentStyle) return;
    if (currentStyle.name !== setup.styleName
      || currentStyle.type !== setup.attackType
      || currentStyle.stance !== setup.stance) {
      updateSetup(setup.uid, {
        styleName: currentStyle.name,
        attackType: currentStyle.type,
        stance: currentStyle.stance,
      });
    }
  }, [setup, currentStyle, updateSetup]);

  if (!repo || !setup || !currentStyle) {
    return <div className="subtle">Select or add a gear setup.</div>;
  }

  const patch = (p: Partial<PlayerSetup>) => updateSetup(setup.uid, p);

  const applyStyle = (style: CombatStyle) =>
    patch({ styleName: style.name, attackType: style.type, stance: style.stance });

  const equip = (slot: EquipmentSlotName, item: EquipmentItem | null) => {
    const equipment = { ...setup.equipment };
    if (item) {
      equipment[slot] = item;
      // A two-handed weapon and a shield cannot be worn together
      if (slot === 'weapon' && item.isTwoHanded) delete equipment.shield;
      if (slot === 'shield' && equipment.weapon?.isTwoHanded) delete equipment.weapon;
    } else {
      delete equipment[slot];
    }
    patch({ equipment });
  };

  const prayers = prayersForType(setup.attackType);
  const potions = potionsForType(setup.attackType);
  if (!prayers.some((p) => p.id === setup.prayerId)) patch({ prayerId: 'NONE' });
  if (!potions.some((p) => p.id === setup.potionId)) patch({ potionId: 'NONE' });

  const spell = setup.spellName
    ? repo.spells.find((s) => s.name === setup.spellName) ?? null : null;
  const isMagic = setup.attackType === 'magic';

  const gear = new Gear(setup);
  const totals = computeTotals(setup, dummyTotalsMonster, gear, spell?.spellbook ?? null);
  const speed = attackSpeedTicks(setup, spell);

  const styleImage = (st: CombatStyle) => ({
    stab: 'White dagger.png',
    slash: 'White scimitar.png',
    crush: 'White warhammer.png',
    ranged: 'Ranged icon.png',
    magic: 'Magic icon.png',
  }[st.type]);

  return (
    <div>
      <div className="row">
        <label className="subtle">Name</label>
        <input type="text" className="grow" value={setup.name}
          onChange={(e) => patch({ name: e.target.value })} />
      </div>

      <div className="editor-columns">
        <div>
          <div className="section-title">Equipment</div>
          <div className="equipment-grid">
            {SLOT_LAYOUT.flat().map((slot, i) => {
              if (!slot) return <span key={`sp-${i}`} className="slot-button spacer" />;
              const item = setup.equipment[slot];
              const imageUrl = item
                ? wikiImageUrl(item.image)
                : wikiImageUrl(SLOT_PLACEHOLDERS[slot]);
              const button = (
                <button
                  key={slot}
                  className="slot-button"
                  onClick={(e) => setPicking({
                    slot,
                    anchor: (e.currentTarget as HTMLElement).getBoundingClientRect(),
                  })}
                  onContextMenu={(e) => {
                    e.preventDefault();
                    equip(slot, null);
                  }}
                >
                  {imageUrl && <img src={imageUrl} alt={slot} className={item ? '' : 'placeholder'} />}
                </button>
              );
              return item
                ? <Tooltip key={slot} content={equipmentTooltip(item)}>{button}</Tooltip>
                : (
                  <Tooltip key={slot} content={(
                    <div>
                      <div className="title">{slot} slot</div>
                      <div className="subtle">Click to choose; right-click to clear</div>
                    </div>
                  )}>{button}</Tooltip>
                );
            })}
          </div>
        </div>

        <div className="grow">
          <div className="section-title">Combat</div>
          <div className="combat-grid">
            <label>Combat style</label>
            <div style={{ gridColumn: '2 / 5' }} className="row">
              {wikiImageUrl(styleImage(currentStyle)) && (
                <img className="icon-note" src={wikiImageUrl(styleImage(currentStyle))!} alt="" />
              )}
              <select
                className="grow"
                value={styles.indexOf(currentStyle)}
                onChange={(e) => applyStyle(styles[parseInt(e.target.value, 10)])}
              >
                {styles.map((st, i) => (
                  <option key={`${st.name}-${st.stance}`} value={i}>
                    {st.name} ({st.stance} · {st.type})
                  </option>
                ))}
              </select>
            </div>

            <label>Prayer</label>
            <select value={setup.prayerId} onChange={(e) => patch({ prayerId: e.target.value })}>
              {prayers.map((p) => <option key={p.id} value={p.id}>{p.label}</option>)}
            </select>
            <label>Potion</label>
            <select value={setup.potionId} onChange={(e) => patch({ potionId: e.target.value })}>
              {potions.map((p) => <option key={p.id} value={p.id}>{p.label}</option>)}
            </select>

            <label>Spell</label>
            <div style={{ gridColumn: '2 / 3' }} className="row">
              <SearchableDropdown
                items={repo.spells}
                label={(s) => `${s.name} (${s.spellbook})`}
                image={(s) => s.image}
                tooltip={spellTooltip}
                value={spell}
                placeholder="(no spell)"
                disabled={!isMagic}
                onSelect={(s) => patch({ spellName: s.name })}
              />
              <button disabled={!isMagic || !spell} onClick={() => patch({ spellName: null })}>✕</button>
            </div>
            <label>Chin distance</label>
            <input
              type="number" min={1} max={10}
              value={setup.chinchompaDistance}
              disabled={setup.attackType !== 'ranged'}
              onChange={(e) => patch({ chinchompaDistance: parseInt(e.target.value, 10) || 5 })}
            />
          </div>

          <div className="section-title">Buffs</div>
          <div className="buff-grid">
            {BUFFS.map((b) => (
              <label key={b.key} title={b.tip}>
                <input
                  type="checkbox"
                  checked={!!setup[b.key]}
                  onChange={(e) => patch({ [b.key]: e.target.checked } as Partial<PlayerSetup>)}
                />
                {wikiImageUrl(b.icon) && <img src={wikiImageUrl(b.icon)!} alt="" />}
                {b.label}
              </label>
            ))}
          </div>
        </div>
      </div>

      <div className="section-title">Bonuses</div>
      <div className="bonus-card">
        <table>
          <thead>
            <tr>
              <th /><th>Stab</th><th>Slash</th><th>Crush</th><th>Magic</th><th>Ranged</th>
              <th>Melee str</th><th>Ranged str</th><th>Magic dmg</th><th>Speed</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Attack</td>
              <Value v={totals.stab} /><Value v={totals.slash} /><Value v={totals.crush} />
              <Value v={totals.magic} /><Value v={totals.ranged} />
              <Value v={totals.str} /><Value v={totals.rangedStr} />
              <Value v={totals.magicStrTenths / 10} suffix="%" />
              <td>{speed} ticks ({(speed * 0.6).toFixed(1)}s)</td>
            </tr>
          </tbody>
        </table>
      </div>

      {picking && (
        <SlotPicker
          items={equipmentForSlot(repo, picking.slot)}
          anchor={picking.anchor}
          onSelect={(item) => {
            equip(picking.slot, item);
            setPicking(null);
          }}
          onClear={() => {
            equip(picking.slot, null);
            setPicking(null);
          }}
          onClose={() => setPicking(null)}
        />
      )}
    </div>
  );
};
