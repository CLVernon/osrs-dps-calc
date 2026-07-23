import { describe, expect, it } from 'vitest';
import { calculate } from '../src/calc/calc';
import { newSetup } from '../src/model/types';
import { ctx, dummyMonster, findEquipment } from './helpers';

// Summer sweep up (July 2026) rebalance regression tests

describe('inquisitor rework', () => {
  it('hauberk alone gives +1% (weight 2)', () => {
    const m = dummyMonster({ def: 100, crush: 50 });
    const p = newSetup('Inq');
    p.equipment.weapon = findEquipment("Inquisitor's mace");
    p.equipment.body = findEquipment("Inquisitor's hauberk");
    p.attackType = 'crush';
    p.stance = 'Aggressive';
    const r = calculate(p, m, ctx());
    // eff str 99+3+8=110; str bonus = mace 96 + hauberk
    const str = 96 + findEquipment("Inquisitor's hauberk").bonuses.str;
    const baseMax = Math.trunc((110 * (str + 64) + 320) / 640);
    expect(r.maxHit).toBe(Math.trunc((baseMax * 202) / 200));
  });

  it('full set gives exactly +2.5% (weight 5), same as before, and mace adds nothing special', () => {
    const m = dummyMonster({ def: 100, crush: 50 });
    const p = newSetup('Inq');
    p.equipment.weapon = findEquipment("Inquisitor's mace");
    p.equipment.head = findEquipment("Inquisitor's great helm");
    p.equipment.body = findEquipment("Inquisitor's hauberk");
    p.equipment.legs = findEquipment("Inquisitor's plateskirt");
    p.attackType = 'crush';
    p.stance = 'Aggressive';
    const r = calculate(p, m, ctx());
    // effective str: 99+3+8=110; str bonus = mace 96 + helm/hauberk/skirt pieces
    const str = 96
      + findEquipment("Inquisitor's great helm").bonuses.str
      + findEquipment("Inquisitor's hauberk").bonuses.str
      + findEquipment("Inquisitor's plateskirt").bonuses.str;
    const baseMax = Math.trunc((110 * (str + 64) + 320) / 640);
    expect(r.maxHit).toBe(Math.trunc((baseMax * 205) / 200));
  });
});

describe('soulreaper axe passive', () => {
  it('adds 6% strength per stack, non-multiplicative with prayers', () => {
    const m = dummyMonster({ def: 100, slash: 50 });
    const p = newSetup('SRA');
    p.equipment.weapon = findEquipment('Soulreaper axe');
    p.attackType = 'slash';
    p.stance = 'Aggressive';
    p.prayerId = 'PIETY';

    const zero = calculate(p, m, ctx());
    const five = calculate({ ...p, soulreaperStacks: 5 }, m, ctx());
    // eff str at 0 stacks: floor(99*1.23)=121 +3+8 = 132
    // at 5 stacks: +floor(99*0.30)=29 -> 161
    // SRA str bonus 125: max0 = floor((132*189+320)/640)=39, max5 = floor((161*189+320)/640)=48
    expect(zero.maxHit).toBe(39);
    expect(five.maxHit).toBe(48);
  });

  it('stacks do nothing without the axe', () => {
    const m = dummyMonster({ def: 100, slash: 50 });
    const p = newSetup('whip');
    p.equipment.weapon = findEquipment('Abyssal whip');
    p.attackType = 'slash';
    p.stance = 'Aggressive';
    const zero = calculate(p, m, ctx());
    const five = calculate({ ...p, soulreaperStacks: 5 }, m, ctx());
    expect(five.maxHit).toBe(zero.maxHit);
  });
});

describe('sanguinesti buff', () => {
  it('base max hit is floor(magic/3) and 20% of hits gain +8', () => {
    const m = dummyMonster({ magicLevel: 100, magicDef: 30 });
    const p = newSetup('Sang');
    p.equipment.weapon = findEquipment('Sanguinesti staff (Charged)');
    p.attackType = 'magic';
    p.stance = 'Accurate';
    const r = calculate(p, m, ctx());
    // base = floor(99/3) = 33 (was 32); dist max includes the +8 proc outcome
    expect(r.maxHit).toBe(33 + 8);
    // 80% of accurate hits: E[max(h,1)] = 562/34; 20%: E[h+8] = 561/34 + 8
    const expected = r.accuracy * ((0.8 * 562 + 0.2 * 561) / 34 + 0.2 * 8);
    expect(r.avgDamagePerAttack).toBeCloseTo(expected, 6);
  });
});

describe('demonbane vulnerability', () => {
  it('darklight now boosts accuracy vs demons', () => {
    const demon = dummyMonster({ def: 100, slash: 50, attributes: ['demon'] });
    const p = newSetup('Darklight');
    p.equipment.weapon = findEquipment('Darklight');
    p.attackType = 'slash';
    p.stance = 'Aggressive';
    const vsDemon = calculate(p, demon, ctx());
    const vsPlain = calculate(p, dummyMonster({ def: 100, slash: 50 }), ctx());
    expect(vsDemon.accuracy).toBeGreaterThan(vsPlain.accuracy);
    expect(vsDemon.maxHit).toBeGreaterThan(vsPlain.maxHit);
  });

  it('ice demon amplifies demonbane by 115%', () => {
    // arclight 70% * 115% = 80% on the ice demon (id 7584)
    const iceDemon = dummyMonster({ def: 100, slash: 50, attributes: ['demon', 'xerician'] });
    iceDemon.id = 7584;
    const normalDemon = dummyMonster({ def: 100, slash: 50, attributes: ['demon'] });
    const p = newSetup('Arclight');
    p.equipment.weapon = findEquipment('Arclight (Charged)');
    p.attackType = 'slash';
    p.stance = 'Aggressive';
    const vsIce = calculate(p, iceDemon, ctx());
    const vsNormal = calculate(p, normalDemon, ctx());
    // ice demon divides non-fire damage by 3, so compare max hits before that…
    // instead compare accuracy boost: same def stats, bigger demonbane on ice demon
    expect(vsIce.accuracy).toBeGreaterThan(vsNormal.accuracy);
  });
});

describe('seeking arrows', () => {
  it('accurate hits deal at least 3 with a bow', () => {
    const m = dummyMonster({ def: 1, standard: 0 });
    const p = newSetup('Seeker');
    const bow = findEquipment('Twisted bow');
    p.equipment.weapon = bow;
    p.equipment.ammo = findEquipment('Seeking dragon arrow');
    p.attackType = 'ranged';
    p.stance = 'Rapid';
    const r = calculate(p, m, ctx());
    expect(r.notes.some((n) => n.includes('Seeking'))).toBe(true);
    expect(r.avgDamagePerAttack).toBeGreaterThanOrEqual(r.accuracy * 3);
  });
});
