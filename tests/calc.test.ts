import { describe, expect, it } from 'vitest';
import { calculate } from '../src/calc/calc';
import { newSetup, type PlayerSetup } from '../src/model/types';
import { potionsForType } from '../src/model/potions';
import { ctx, dummyMonster, findEquipment, findSpell, spells } from './helpers';
import { spellMaxHitAtLevel } from '../src/model/types';

const whipSetup = (): PlayerSetup => {
  const p = newSetup('Whip test');
  p.equipment.weapon = findEquipment('Abyssal whip');
  p.attackType = 'slash';
  p.stance = 'Aggressive';
  p.prayerId = 'PIETY';
  p.potionId = 'SUPER_COMBAT';
  return p;
};

describe('melee', () => {
  it('whip max hit with piety and super combat', () => {
    // visible str = 99+5+floor(99*0.15)=118; floor(118*1.23)=145; +3+8=156
    // max hit = floor((156*(82+64)+320)/640) = 36
    const r = calculate(whipSetup(), dummyMonster({ def: 100, stab: 50, slash: 50, crush: 50 }), ctx());
    expect(r.maxHit).toBe(36);
  });

  it('whip accuracy and dps against known defence', () => {
    // eff attack = floor(118*1.20)+0+8 = 149; roll = 149*146 = 21754
    // def roll = 109*114 = 12426; accuracy = 1-(12428)/(2*21755) = 0.7143644...
    const r = calculate(whipSetup(), dummyMonster({ def: 100, stab: 50, slash: 50, crush: 50 }), ctx());
    expect(r.accuracy).toBeCloseTo(0.7143644, 6);
    // accurate zeros become 1s: E[hit] = 667/37
    const expectedAvg = 0.7143644 * (667 / 37);
    expect(r.avgDamagePerAttack).toBeCloseTo(expectedAvg, 3);
    expect(r.attackSpeedTicks).toBe(4);
    expect(r.dps).toBeCloseTo(expectedAvg / 2.4, 3);
    expect(r.ttkSeconds).toBeGreaterThanOrEqual(100 / r.dps - 1e-9);
  });

  it('slayer helmet boosts melee on task', () => {
    const p = whipSetup();
    p.equipment.head = findEquipment('Slayer helmet (i)');
    p.onSlayerTask = true;
    const r = calculate(p, dummyMonster({ def: 100, slash: 50 }), ctx());
    expect(r.maxHit).toBe(42); // floor(36*7/6)
  });

  it('void melee boosts both rolls', () => {
    const p = whipSetup();
    p.equipment.head = findEquipment('Void melee helm (Normal)');
    p.equipment.body = findEquipment('Void knight top (Normal)');
    p.equipment.legs = findEquipment('Void knight robe (Normal)');
    p.equipment.hands = findEquipment('Void knight gloves (Normal)');
    const m = dummyMonster({ def: 100, slash: 50 });
    const withVoid = calculate(p, m, ctx());
    const without = calculate(whipSetup(), m, ctx());
    expect(withVoid.maxHit).toBe(39); // floor(156*1.1)=171 -> floor((171*146+320)/640)
    expect(withVoid.accuracy).toBeGreaterThan(without.accuracy);
  });

  it('fang rerolls accuracy', () => {
    const m = dummyMonster({ def: 200, stab: 100, slash: 100 });
    const fang = newSetup('Fang');
    fang.equipment.weapon = findEquipment("Osmumten's fang");
    fang.attackType = 'stab';
    fang.stance = 'Aggressive';
    const plain = whipSetup();
    plain.prayerId = 'NONE';
    plain.potionId = 'NONE';
    const fangResult = calculate(fang, m, ctx());
    const plainResult = calculate(plain, m, ctx());
    expect(fangResult.accuracy).toBeGreaterThan(plainResult.accuracy);
    expect(fangResult.accuracy).toBeLessThanOrEqual(1.0);
  });

  it('scythe hits three times on large targets', () => {
    const p = newSetup('Scythe');
    p.equipment.weapon = findEquipment('Scythe of vitur (Charged)');
    p.attackType = 'slash';
    p.stance = 'Aggressive';
    const small = dummyMonster({ def: 100, slash: 50, size: 1 });
    const large = dummyMonster({ def: 100, slash: 50, size: 5 });
    const vsSmall = calculate(p, small, ctx());
    const vsLarge = calculate(p, large, ctx());
    const maxHit = vsSmall.maxHit;
    expect(vsLarge.maxHit).toBe(maxHit + Math.trunc(maxHit / 2) + Math.trunc(maxHit / 4));
    expect(vsLarge.dps).toBeGreaterThan(vsSmall.dps * 1.6);
  });

  it('keris triple hit vs kalphite', () => {
    const p = newSetup('Keris');
    p.equipment.weapon = findEquipment('Keris partisan');
    p.attackType = 'stab';
    p.stance = 'Aggressive';
    const kalphite = dummyMonster({ def: 50, stab: 30, attributes: ['kalphite'] });
    const plain = dummyMonster({ def: 50, stab: 30 });
    const vsKalphite = calculate(p, kalphite, ctx());
    const vsPlain = calculate(p, plain, ctx());
    expect(vsKalphite.maxHit).toBeGreaterThanOrEqual(vsPlain.maxHit * 3);
    expect(vsKalphite.avgDamagePerAttack).toBeGreaterThan(vsPlain.avgDamagePerAttack * 1.3);
  });

  it('vampyre tier 3 immune without blisterwood', () => {
    const vampyre = dummyMonster({ def: 100, slash: 50, attributes: ['vampyre3'] });
    expect(calculate(whipSetup(), vampyre, ctx()).dps).toBe(0);
  });

  it('leafy immune without leaf-bladed', () => {
    const leafy = dummyMonster({ def: 100, slash: 50, attributes: ['leafy'] });
    expect(calculate(whipSetup(), leafy, ctx()).dps).toBe(0);
    const lbb = whipSetup();
    lbb.equipment.weapon = findEquipment('Leaf-bladed battleaxe');
    expect(calculate(lbb, leafy, ctx()).dps).toBeGreaterThan(0);
  });

  it('flat armour reduces average damage', () => {
    const armoured = dummyMonster({ def: 100, slash: 50, flatArmour: 5 });
    const plain = dummyMonster({ def: 100, slash: 50 });
    expect(calculate(whipSetup(), armoured, ctx()).avgDamagePerAttack)
      .toBeLessThan(calculate(whipSetup(), plain, ctx()).avgDamagePerAttack);
  });
});

describe('ranged', () => {
  it('twisted bow scales with monster magic', () => {
    const p = newSetup('Tbow');
    p.equipment.weapon = findEquipment('Twisted bow');
    p.equipment.ammo = findEquipment('Dragon arrow (Unpoisoned)');
    p.attackType = 'ranged';
    p.stance = 'Rapid';
    p.prayerId = 'RIGOUR';
    p.potionId = 'RANGING_POTION';
    const vsLow = calculate(p, dummyMonster({ magicLevel: 1, standard: 50 }), ctx());
    const vsHigh = calculate(p, dummyMonster({ magicLevel: 250, standard: 50 }), ctx());
    expect(vsHigh.maxHit).toBeGreaterThan(vsLow.maxHit * 2);
    expect(vsHigh.attackSpeedTicks).toBe(5);
  });

  it('ruby bolts boost average damage vs high hp', () => {
    const p = newSetup('RCB');
    p.equipment.weapon = findEquipment('Rune crossbow');
    p.attackType = 'ranged';
    p.stance = 'Rapid';
    const withRuby = { ...p, equipment: { ...p.equipment, ammo: findEquipment('Ruby bolts (e)') } };
    const bigHp = dummyMonster({ hp: 500, standard: 50 });
    const without = calculate(p, bigHp, ctx());
    const withR = calculate(withRuby, bigHp, ctx());
    expect(withR.avgDamagePerAttack).toBeGreaterThan(without.avgDamagePerAttack);
    expect(withR.maxHit).toBeGreaterThanOrEqual(100);
  });
});

describe('magic', () => {
  it('trident of the swamp max hit', () => {
    // floor(99/3)-2 = 31 base; occult +5% -> 32
    const p = newSetup('Trident');
    p.equipment.weapon = findEquipment('Trident of the swamp (Charged)');
    p.equipment.neck = findEquipment('Occult necklace');
    p.attackType = 'magic';
    p.stance = 'Accurate';
    const r = calculate(p, dummyMonster({ magicLevel: 100, magicDef: 30 }), ctx());
    expect(r.maxHit).toBe(32);
    expect(r.attackSpeedTicks).toBe(4);
  });

  it("tumeken's shadow triples magic bonuses", () => {
    // base = floor(99/3)+1 = 34; occult 5% tripled to 15% -> floor(34*1.15) = 39
    const p = newSetup('Shadow');
    p.equipment.weapon = findEquipment("Tumeken's shadow (Charged)");
    p.equipment.neck = findEquipment('Occult necklace');
    p.attackType = 'magic';
    p.stance = 'Accurate';
    const r = calculate(p, dummyMonster({ magicLevel: 100, magicDef: 30 }), ctx());
    expect(r.maxHit).toBe(39);
  });

  it('manual cast does not triple shadow bonuses', () => {
    const p = newSetup('Manual cast');
    p.equipment.weapon = findEquipment("Tumeken's shadow (Charged)");
    p.equipment.neck = findEquipment('Occult necklace');
    p.attackType = 'magic';
    p.spellName = 'Fire Surge';
    p.stance = 'Manual Cast';
    const m = dummyMonster({ magicLevel: 100, magicDef: 30 });
    const manual = calculate(p, m, ctx());
    expect(manual.maxHit).toBe(25); // floor(24*1.05)
    expect(manual.attackSpeedTicks).toBe(5);
    const auto = calculate({ ...p, stance: 'Autocast' }, m, ctx());
    expect(auto.maxHit).toBe(27); // floor(24*1.15)
  });

  it('salamander blaze max hit', () => {
    const p = newSetup('Salamander');
    p.equipment.weapon = findEquipment('Black salamander');
    p.attackType = 'magic';
    p.stance = 'Defensive';
    const r = calculate(p, dummyMonster({ magicLevel: 100 }), ctx());
    expect(r.maxHit).toBe(24); // floor((99*156+320)/640)
  });

  it('magic without spell or powered staff is zero', () => {
    const p = newSetup('No spell');
    p.equipment.weapon = findEquipment('Abyssal whip');
    p.attackType = 'magic';
    p.stance = 'Autocast';
    expect(calculate(p, dummyMonster({ magicLevel: 100 }), ctx()).dps).toBe(0);
  });

  it('spell max hits scale with level', () => {
    expect(spellMaxHitAtLevel(findSpell('Wind Strike'), 99, spells)).toBe(8);
  });
});

describe('potions', () => {
  it('potion lists match attack type', () => {
    const ranged = potionsForType('ranged').map((p) => p.id);
    expect(ranged).toContain('RANGING_POTION');
    expect(ranged).toContain('BASTION_POTION');
    const magic = potionsForType('magic').map((p) => p.id);
    expect(magic).toContain('SATURATED_HEART');
    expect(magic).toContain('IMBUED_HEART');
    const melee = potionsForType('slash').map((p) => p.id);
    expect(melee).toContain('SUPER_COMBAT');
  });
});
