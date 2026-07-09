import { describe, expect, it } from 'vitest';
import { scaleCoxMonster } from '../src/calc/coxScaling';
import type { Monster } from '../src/model/types';
import { dummyMonster } from './helpers';

const xerician = (id: number): Monster => {
  const m = dummyMonster({ attributes: ['xerician'] });
  m.id = id;
  m.skills = { atk: 250, str: 250, ranged: 250, magic: 250, def: 150, hp: 300 };
  return m;
};

describe('CoX scaling', () => {
  it('non-xerician monsters are untouched', () => {
    const m = dummyMonster({});
    m.partySize = 5;
    expect(scaleCoxMonster(m)).toBe(m);
  });

  it('solo at max stats keeps base values', () => {
    const m = xerician(999001);
    m.partySize = 1;
    const scaled = scaleCoxMonster(m);
    expect(scaled.skills.atk).toBe(250);
    expect(scaled.skills.def).toBe(150);
    expect(scaled.skills.hp).toBe(300);
  });

  it('five player scaling matches formulas', () => {
    const m = xerician(999001);
    m.partySize = 5;
    const scaled = scaleCoxMonster(m);
    expect(scaled.skills.atk).toBe(295); // 250*118/100
    expect(scaled.skills.magic).toBe(295);
    expect(scaled.skills.def).toBe(156); // 150*104/100
    expect(scaled.skills.hp).toBe(900); // 300 + 300*2
  });

  it('challenge mode adds fifty percent', () => {
    const m = xerician(999001);
    m.partySize = 5;
    m.coxChallengeMode = true;
    const scaled = scaleCoxMonster(m);
    expect(scaled.skills.atk).toBe(442);
    expect(scaled.skills.def).toBe(234);
    expect(scaled.skills.hp).toBe(1350);
  });

  it('tekton CM defence is special cased', () => {
    const tekton = xerician(7540);
    tekton.skills.magic = 205;
    tekton.skills.def = 205;
    tekton.partySize = 5;
    tekton.coxChallengeMode = true;
    const scaled = scaleCoxMonster(tekton);
    expect(scaled.skills.def).toBe(287); // 205*104/100=213, +35% = 287
    expect(scaled.skills.magic).toBe(287); // magic is defensive for Tekton
  });

  it('olm head hp uses phase formula', () => {
    const olmHead = xerician(7551);
    olmHead.partySize = 5;
    expect(scaleCoxMonster(olmHead).skills.hp).toBe(2400); // 800 + 400*4
  });

  it('olm mage hand halves magic and uses hand hp', () => {
    const mageHand = xerician(7550);
    mageHand.partySize = 3;
    const scaled = scaleCoxMonster(mageHand);
    expect(scaled.skills.hp).toBe(1200); // 600 + 300*2
    expect(scaled.skills.magic).toBe(127); // 250*102/100=255, halved
  });

  it('guardians hp scales with mining', () => {
    const guardian = xerician(7569);
    guardian.partySize = 2;
    guardian.partyAvgMiningLevel = 99;
    expect(scaleCoxMonster(guardian).skills.hp).toBe(500); // (151+99)*1 + *trunc(2*50/100)
  });
});
