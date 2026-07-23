import {
  type Monster, type PlayerCharacter, type PlayerSetup, type SpellData,
  hasAttribute, isBindSpell, spellElement, spellMaxHitAtLevel,
} from '../model/types';
import { potionById } from '../model/potions';
import { applyPrayerAccuracy, applyPrayerStrength, prayerById } from '../model/prayers';
import {
  AttackDist, HitDist,
  addTransformer, cappedRerollTransformer, divisionTransformer, flatLimitTransformer,
  linearMinTransformer, multiplyTransformer,
} from './dist';
import * as C from './constants';
import { Gear } from './gear';
import { attackBonus, computeTotals, maxHitFromEffective, type EquipmentTotals } from './equipmentTotals';
import { scaleCoxMonster } from './coxScaling';

export interface DpsResult {
  maxHit: number;
  accuracy: number;
  avgDamagePerAttack: number;
  attackSpeedTicks: number;
  dps: number;
  expectedHitsToKill: number;
  ttkSeconds: number;
  notes: string[];
}

export interface CalcContext {
  character: PlayerCharacter;
  allSpells: SpellData[];
}

const t = Math.trunc;

const effectiveCurrentHp = (c: PlayerCharacter): number =>
  c.currentHitpoints > 0 ? Math.min(c.currentHitpoints, c.hitpoints) : c.hitpoints;

/** Weapon attack speed in ticks including stance rules; unarmed is 4 ticks. */
export const attackSpeedTicks = (p: PlayerSetup, spell: SpellData | null): number => {
  const weapon = p.equipment.weapon;
  let base = weapon && weapon.speed > 0 ? weapon.speed : 4;
  const casting = p.attackType === 'magic' && spell != null
    && ['Autocast', 'Defensive Autocast', 'Manual Cast'].includes(p.stance);
  if (p.attackType === 'ranged' && p.stance === 'Rapid') {
    base -= 1;
  } else if (casting) {
    if (weapon?.name === 'Harmonised nightmare staff' && spell?.spellbook === 'standard'
      && p.stance !== 'Manual Cast') {
      base = 4;
    } else if (weapon?.name === 'Twinflame staff') {
      base = 6;
    } else {
      base = 5;
    }
  }
  return Math.max(1, base);
};

class Calc {
  private readonly g: Gear;
  private readonly totals: EquipmentTotals;
  private readonly spell: SpellData | null;
  readonly notes: string[] = [];

  constructor(
    private readonly p: PlayerSetup,
    private readonly m: Monster,
    private readonly ctx: CalcContext,
    private readonly defenceRollOverride: number | null = null,
  ) {
    this.g = new Gear(p);
    this.spell = p.spellName
      ? ctx.allSpells.find((s) => s.name === p.spellName) ?? null
      : null;
    this.totals = computeTotals(p, m, this.g, this.spell?.spellbook ?? null);
  }

  private note(note: string) {
    if (!this.notes.includes(note)) this.notes.push(note);
  }

  private isMelee(): boolean {
    return ['stab', 'slash', 'crush'].includes(this.p.attackType);
  }

  /** How strongly demonbane effects apply to this monster, as a percent. */
  private demonbaneVulnerability(): number {
    const { m } = this;
    if (m.name === 'Duke Sucellus') return 70;
    if (C.YAMA_IDS.has(m.id)) return 120;
    if (C.YAMA_VOID_FLARE_IDS.has(m.id)) return 200;
    if (C.ICE_DEMON_IDS.has(m.id)) return 115;
    return 100;
  }

  /** Weapon demonbane percent scaled by the monster's vulnerability. */
  private demonbanePercent(weaponPercent: number): number {
    return t((weaponPercent * this.demonbaneVulnerability()) / 100);
  }

  private isUndead(): boolean {
    return hasAttribute(this.m, 'undead');
  }

  private onTask(): boolean {
    return this.p.onSlayerTask;
  }

  private prayer() {
    return prayerById(this.p.prayerId);
  }

  private potion() {
    return potionById(this.p.potionId);
  }

  private visibleAttack(): number {
    const level = this.ctx.character.attack;
    return level + this.potion().attackBoost(level);
  }

  private visibleStrength(): number {
    const level = this.ctx.character.strength;
    return level + this.potion().strengthBoost(level);
  }

  private visibleRanged(): number {
    const level = this.ctx.character.ranged;
    return level + this.potion().rangedBoost(level);
  }

  private visibleMagic(): number {
    const level = this.ctx.character.magic;
    return level + this.potion().magicBoost(level);
  }

  /** True when attacking with a cast spell rather than a powered staff. */
  private castingSpell(): boolean {
    return this.p.attackType === 'magic' && this.spell != null
      && ['Autocast', 'Defensive Autocast', 'Manual Cast'].includes(this.p.stance);
  }

  // ------------------------------------------------------------ NPC defence

  npcDefenceRoll(): number {
    if (this.defenceRollOverride != null) return this.defenceRollOverride;
    const type = this.p.attackType;
    const m = this.m;

    const level = type === 'magic' && !C.USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_IDS.has(m.id)
      ? m.skills.magic
      : m.skills.def;
    const effectiveLevel = level + 9;

    let bonus: number;
    if (type === 'ranged') {
      switch (this.rangedDamageType()) {
        case 'light': bonus = m.defensive.light; break;
        case 'heavy': bonus = m.defensive.heavy; break;
        case 'mixed':
          bonus = t((m.defensive.light + m.defensive.standard + m.defensive.heavy) / 3);
          break;
        default: bonus = m.defensive.standard;
      }
    } else {
      bonus = type === 'magic' ? m.defensive.magic : m.defensive[type];
    }

    let defenceRoll = effectiveLevel * (bonus + 64);

    const isCustom = m.id <= 0;
    const invocation = m.toaInvocationLevel ?? 0;
    if ((isCustom || (C.TOA_MONSTER_IDS.has(m.id) && !C.KEPHRI_OVERLORD_IDS.has(m.id)))
      && invocation > 0) {
      defenceRoll = t((defenceRoll * (250 + invocation)) / 250);
      this.note(`ToA invocation ${invocation}: defence scaled`);
    }
    return defenceRoll;
  }

  private rangedDamageType(): 'light' | 'standard' | 'heavy' | 'mixed' {
    switch (this.g.weaponCategory()) {
      case 'Thrown': return 'light';
      case 'Bow': return 'standard';
      case 'Crossbow':
      case 'Chinchompas': return 'heavy';
      case 'Salamander': return 'mixed';
      default: return 'standard';
    }
  }

  // ------------------------------------------------------------------ melee

  private meleeAttackRoll(): number {
    const { p, g, m } = this;
    let effectiveLevel = applyPrayerAccuracy(this.prayer(), this.visibleAttack());

    let stanceBonus = 8;
    if (p.stance === 'Accurate') stanceBonus += 3;
    else if (p.stance === 'Controlled') stanceBonus += 1;
    effectiveLevel += stanceBonus;

    if (g.meleeVoid()) {
      effectiveLevel = t((effectiveLevel * 11) / 10);
      this.note('Void melee');
    }

    const baseRoll = effectiveLevel * (attackBonus(this.totals, p.attackType) + 64);
    let attackRoll = baseRoll;

    if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      attackRoll = t((attackRoll * (p.forinthrySurge ? 27 : 24)) / 20);
      this.note('Amulet of avarice vs revenant');
    } else if ((g.salveE() || g.salveEi()) && this.isUndead()) {
      attackRoll = t((attackRoll * 6) / 5);
      this.note('Salve amulet (e) vs undead');
    } else if ((g.salve() || g.salveI()) && this.isUndead()) {
      attackRoll = t((attackRoll * 7) / 6);
      this.note('Salve amulet vs undead');
    } else if (g.blackMask() && this.onTask()) {
      attackRoll = t((attackRoll * 7) / 6);
      this.note('Black mask/slayer helmet on task');
    }

    if (g.tzhaarWeapon() && g.obsidianArmour()) {
      attackRoll += t(baseRoll / 10);
      this.note('Obsidian set + Tzhaar weapon');
    }
    if (g.revWeaponBuffApplicable()) {
      attackRoll = t((attackRoll * 3) / 2);
      this.note('Wilderness weapon vs wilderness target');
    }
    if (g.wearing('Arclight', 'Emberlight') && hasAttribute(m, 'demon')) {
      attackRoll += t((attackRoll * this.demonbanePercent(70)) / 100);
      this.note('Arclight/Emberlight vs demon');
    }
    if (g.wearing('Silverlight', 'Darklight') && hasAttribute(m, 'demon')) {
      attackRoll += t((attackRoll * this.demonbanePercent(60)) / 100);
    }
    if (g.wearing('Bone claws', 'Burning claws') && hasAttribute(m, 'demon')) {
      attackRoll += t((attackRoll * this.demonbanePercent(5)) / 100);
    }
    if (hasAttribute(m, 'dragon')) {
      if (g.wearing('Dragon hunter lance')) {
        attackRoll = t((attackRoll * 6) / 5);
        this.note('Dragon hunter lance vs dragon');
      } else if (g.wearing('Dragon hunter wand')) {
        attackRoll = t((attackRoll * 7) / 4);
        this.note('Dragon hunter wand vs dragon');
      }
    }
    if (g.wearing('Keris partisan of breaching') && hasAttribute(m, 'kalphite')) {
      attackRoll = t((attackRoll * 133) / 100);
      this.note('Keris partisan of breaching vs kalphite');
    }
    if (g.wearing('Blisterwood flail', 'Blisterwood sickle') && hasAttribute(m, 'vampyre')) {
      attackRoll = t((attackRoll * 21) / 20);
    }
    if (g.silverWeapon() && g.wearing("Efaritay's aid") && hasAttribute(m, 'vampyre')) {
      attackRoll = t((attackRoll * 23) / 20);
      this.note("Efaritay's aid vs vampyre");
    }
    if (g.wearing('Granite hammer') && hasAttribute(m, 'golem')) {
      attackRoll = t((attackRoll * 13) / 10);
      this.note('Granite hammer vs golem');
    }
    return this.applyInquisitor(attackRoll);
  }

  private applyInquisitor(roll: number): number {
    if (this.p.attackType !== 'crush') return roll;
    const weight = this.g.inquisitorWeight();
    if (weight === 0) return roll;
    this.note("Inquisitor's armour (crush)");
    return t((roll * (200 + weight)) / 200);
  }

  private meleeMinMax(): [number, number] {
    const { p, g, m } = this;
    const baseLevel = this.visibleStrength();
    let effectiveLevel = applyPrayerStrength(this.prayer(), baseLevel);

    // Soulreaper axe passive: +6% strength per stack (does not stack
    // multiplicatively with prayers)
    if (g.weaponName() === 'Soulreaper axe') {
      const stacks = Math.max(0, Math.min(5, p.soulreaperStacks ?? 0));
      if (stacks > 0) {
        effectiveLevel += t((baseLevel * stacks * 6) / 100);
        this.note(`Soulreaper axe: ${stacks} stack(s)`);
      }
    }

    let stanceBonus = 8;
    if (p.stance === 'Aggressive') stanceBonus += 3;
    else if (p.stance === 'Controlled') stanceBonus += 1;
    effectiveLevel += stanceBonus;

    if (g.meleeVoid()) {
      effectiveLevel = t((effectiveLevel * 11) / 10);
    }

    const baseMax = maxHitFromEffective(effectiveLevel, this.totals.str);
    let minHit = 0;
    let maxHit = baseMax;

    if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      maxHit = t((maxHit * (p.forinthrySurge ? 27 : 24)) / 20);
    } else if ((g.salveE() || g.salveEi()) && this.isUndead()) {
      maxHit = t((maxHit * 6) / 5);
    } else if ((g.salve() || g.salveI()) && this.isUndead()) {
      maxHit = t((maxHit * 7) / 6);
    } else if (g.blackMask() && this.onTask()) {
      maxHit = t((maxHit * 7) / 6);
    }

    if (g.wearing('Arclight', 'Emberlight') && hasAttribute(m, 'demon')) {
      maxHit += t((maxHit * this.demonbanePercent(70)) / 100);
    }
    if (g.wearing('Bone claws', 'Burning claws') && hasAttribute(m, 'demon')) {
      maxHit += t((maxHit * this.demonbanePercent(5)) / 100);
    }
    if (g.tzhaarWeapon() && g.obsidianArmour()) {
      maxHit += t(baseMax / 10);
    }
    if (g.wearing('Dragon hunter lance') && hasAttribute(m, 'dragon')) {
      maxHit = t((maxHit * 6) / 5);
    }
    if (g.wearing('Dragon hunter wand') && hasAttribute(m, 'dragon')) {
      maxHit = t((maxHit * 7) / 5);
    }
    if (g.keris() && hasAttribute(m, 'kalphite')) {
      maxHit = g.wearing('Keris partisan of amascut')
        ? t((maxHit * 115) / 100)
        : t((maxHit * 133) / 100);
      this.note('Keris vs kalphite');
    }
    if (g.wearing('Barronite mace') && hasAttribute(m, 'golem')) {
      maxHit = t((maxHit * 23) / 20);
      this.note('Barronite mace vs golem');
    }
    if (g.wearing('Granite hammer') && hasAttribute(m, 'golem')) {
      maxHit = t((maxHit * 13) / 10);
    }
    if (g.revWeaponBuffApplicable()) {
      maxHit = t((maxHit * 3) / 2);
    }
    if (g.wearing('Silverlight', 'Darklight', 'Silverlight (dyed)') && hasAttribute(m, 'demon')) {
      maxHit += t((maxHit * this.demonbanePercent(60)) / 100);
      this.note('Silverlight/Darklight vs demon');
    }
    if (g.wearing('Leaf-bladed battleaxe') && hasAttribute(m, 'leafy')) {
      maxHit = t((maxHit * 47) / 40);
      this.note('Leaf-bladed battleaxe vs leafy');
    }
    if (g.wearing('Colossal blade')) {
      maxHit += Math.min(m.size * 2, 10);
      this.note(`Colossal blade vs size ${m.size}`);
    }
    if (g.ratBoneWeapon() && hasAttribute(m, 'rat')) {
      maxHit += 10;
      this.note('Rat bone weapon vs rat');
    }
    if (this.p.attackType === 'crush') {
      const weight = g.inquisitorWeight();
      if (weight > 0) {
        maxHit = t((maxHit * (200 + weight)) / 200);
      }
    }

    if (g.fang()) {
      const shrink = t((maxHit * 3) / 20);
      minHit = shrink;
      maxHit -= shrink;
      this.note("Osmumten's fang");
    }
    return [minHit, maxHit];
  }

  // ----------------------------------------------------------------- ranged

  private rangedAttackRoll(): number {
    const { p, g, m } = this;
    let effectiveLevel = applyPrayerAccuracy(this.prayer(), this.visibleRanged());
    if (p.stance === 'Accurate') effectiveLevel += 3;
    effectiveLevel += 8;
    if (g.rangedVoid()) {
      effectiveLevel = t((effectiveLevel * 11) / 10);
      this.note('Void ranged');
    }

    let attackRoll = effectiveLevel * (this.totals.ranged + 64);

    if (g.crystalBow()) {
      const weight = g.crystalArmourWeight();
      if (weight > 0) {
        attackRoll = t((attackRoll * (20 + weight)) / 20);
        this.note('Crystal armour + crystal bow');
      }
    }

    if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      attackRoll = t((attackRoll * (p.forinthrySurge ? 27 : 24)) / 20);
      this.note('Amulet of avarice vs revenant');
    } else if (g.salveEi() && this.isUndead()) {
      attackRoll = t((attackRoll * 6) / 5);
      this.note('Salve amulet(ei) vs undead');
    } else if (g.salveI() && this.isUndead()) {
      attackRoll = t((attackRoll * 7) / 6);
      this.note('Salve amulet(i) vs undead');
    } else if (g.imbuedBlackMask() && this.onTask()) {
      attackRoll = t((attackRoll * 23) / 20);
      this.note('Slayer helmet (i) on task');
    }

    if (g.twistedBow()) {
      const cap = hasAttribute(m, 'xerician') ? 350 : 250;
      const tbowMagic = Math.min(cap, Math.max(m.skills.magic, m.offensive.magic));
      attackRoll = tbowScaling(attackRoll, tbowMagic, true);
      if (C.P2_WARDEN_IDS.has(m.id)) {
        attackRoll = tbowScaling(attackRoll, tbowMagic, true);
      }
      this.note(`Twisted bow vs magic ${tbowMagic}`);
    }
    if (g.revWeaponBuffApplicable()) {
      attackRoll = t((attackRoll * 3) / 2);
      this.note('Wilderness weapon vs wilderness target');
    }
    if (g.wearing('Dragon hunter crossbow') && hasAttribute(m, 'dragon')) {
      attackRoll = t((attackRoll * 13) / 10);
      this.note('Dragon hunter crossbow vs dragon');
    }
    if (g.chinchompa()) {
      attackRoll = t((attackRoll * this.chinchompaFuseNumerator()) / 4);
      this.note(`Chinchompa fuse at distance ${p.chinchompaDistance}`);
    }
    if (g.wearing('Scorching bow') && hasAttribute(m, 'demon')) {
      attackRoll += t((attackRoll * this.demonbanePercent(30)) / 100);
      this.note('Scorching bow vs demon');
    }
    return attackRoll;
  }

  private chinchompaFuseNumerator(): number {
    const distance = Math.min(7, Math.max(1, this.p.chinchompaDistance));
    const stance = this.p.stance;
    if (stance === 'Accurate') {
      if (distance >= 7) return 2;
      return distance >= 4 ? 3 : 4;
    }
    if (stance === 'Rapid') {
      return distance < 4 || distance >= 7 ? 3 : 4;
    }
    if (distance < 4) return 2;
    return distance < 7 ? 3 : 4;
  }

  private rangedMinMax(): [number, number] {
    const { p, g, m } = this;
    const scalesWithStr = g.wearing('Eclipse atlatl', "Hunter's spear");
    let effectiveLevel = scalesWithStr ? this.visibleStrength() : this.visibleRanged();

    if (g.wearing('Holy water')) {
      if (!hasAttribute(m, 'demon')) {
        this.note('Holy water only damages demons');
        return [0, 0];
      }
      effectiveLevel += 10;
      const str = p.equipment.weapon?.bonuses.ranged_str ?? 0;
      let maxHit = maxHitFromEffective(effectiveLevel, str);
      maxHit += t((maxHit * 60) / 100);
      this.note('Holy water vs demon');
      return [0, maxHit];
    }

    if (g.wearing('Ogre bow', 'Comp ogre bow')) {
      effectiveLevel += 10;
      const bonusStr = p.equipment.ammo?.bonuses.ranged_str ?? 0;
      const maxHit = t((effectiveLevel * (bonusStr + 64) + 320) / 640);
      this.note('Ogre bow: ammo strength only');
      return [0, maxHit];
    }

    effectiveLevel = applyPrayerStrength(this.prayer(), effectiveLevel);
    if (p.stance === 'Accurate') effectiveLevel += 3;
    effectiveLevel += 8;

    if (g.eliteRangedVoid()) {
      effectiveLevel = t((effectiveLevel * 9) / 8);
      this.note('Elite void ranged');
    } else if (g.rangedVoid()) {
      effectiveLevel = t((effectiveLevel * 11) / 10);
    }

    const bonusStr = scalesWithStr ? this.totals.str : this.totals.rangedStr;
    let minHit = 0;
    let maxHit = maxHitFromEffective(effectiveLevel, bonusStr);

    if (g.crystalBow()) {
      const weight = g.crystalArmourWeight();
      if (weight > 0) {
        maxHit = t((maxHit * (40 + weight)) / 40);
      }
    }

    let needRevBuff = g.revWeaponBuffApplicable();
    let needDragonbane = g.wearing('Dragon hunter crossbow') && hasAttribute(m, 'dragon');
    let needDemonbane = g.wearing('Scorching bow') && hasAttribute(m, 'demon');

    if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      maxHit = t((maxHit * (p.forinthrySurge ? 27 : 24)) / 20);
    } else if ((g.salveEi() || (scalesWithStr && g.salveE())) && this.isUndead()) {
      maxHit = t((maxHit * 6) / 5);
    } else if ((g.salveI() || (scalesWithStr && g.salve())) && this.isUndead()) {
      maxHit = t((maxHit * 7) / 6);
    } else if (scalesWithStr && g.blackMask() && this.onTask()) {
      maxHit = t((maxHit * 7) / 6);
    } else if (g.imbuedBlackMask() && this.onTask()) {
      let numerator = 23;
      if (needRevBuff) { needRevBuff = false; numerator += 10; }
      if (needDragonbane) { needDragonbane = false; numerator += 5; }
      if (needDemonbane) { needDemonbane = false; numerator += 6; }
      maxHit = t((maxHit * numerator) / 20);
    }

    if (g.twistedBow()) {
      const cap = hasAttribute(m, 'xerician') ? 350 : 250;
      const tbowMagic = Math.min(cap, Math.max(m.skills.magic, m.offensive.magic));
      maxHit = tbowScaling(maxHit, tbowMagic, false);
    }
    if (needRevBuff) maxHit = t((maxHit * 3) / 2);
    if (needDragonbane) maxHit = t((maxHit * 5) / 4);
    if (needDemonbane) maxHit += t((maxHit * this.demonbanePercent(30)) / 100);

    if (g.ratBoneWeapon() && hasAttribute(m, 'rat')) {
      maxHit += 10;
    }
    if (g.wearing('Tonalztics of ralos')) {
      maxHit = t((maxHit * 3) / 4);
      this.note('Tonalztics of ralos');
    }

    if (C.P2_WARDEN_IDS.has(m.id)) {
      [minHit, maxHit] = this.applyP2WardensDamage(maxHit);
    }
    return [minHit, maxHit];
  }

  // ------------------------------------------------------------------ magic

  private magicAttackRoll(): number {
    const { p, g, m } = this;
    let effectiveLevel = applyPrayerAccuracy(this.prayer(), this.visibleMagic());
    if (p.stance === 'Accurate') effectiveLevel += 2;
    effectiveLevel += 9;
    if (g.magicVoid()) {
      effectiveLevel = t((effectiveLevel * 29) / 20);
      this.note('Void magic');
    }

    const baseRoll = effectiveLevel * (this.totals.magic + 64);
    let attackRoll = baseRoll;

    let additiveBonus = 0;
    let blackMaskBonus = false;
    if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      additiveBonus += p.forinthrySurge ? 35 : 20;
      this.note('Amulet of avarice vs revenant');
    } else if (g.salveEi() && this.isUndead()) {
      additiveBonus += 20;
      this.note('Salve amulet(ei) vs undead');
    } else if (g.salveI() && this.isUndead()) {
      additiveBonus += 15;
      this.note('Salve amulet(i) vs undead');
    } else if (g.imbuedBlackMask() && this.onTask()) {
      blackMaskBonus = true;
      this.note('Slayer helmet (i) on task');
    }
    if (g.wearing("Efaritay's aid") && hasAttribute(m, 'vampyre') && g.silverWeapon()) {
      additiveBonus += 15;
    }
    const usingSpell = this.castingSpell() ? this.spell : null;
    if (g.smokeStaff() && usingSpell?.spellbook === 'standard') {
      additiveBonus += 10;
      this.note('Smoke battlestaff with standard spell');
    }
    if (additiveBonus !== 0) {
      attackRoll = t((attackRoll * (100 + additiveBonus)) / 100);
    }

    if (hasAttribute(m, 'dragon')) {
      if (g.wearing('Dragon hunter crossbow')) {
        attackRoll = t((attackRoll * 13) / 10);
      } else if (g.wearing('Dragon hunter lance')) {
        attackRoll = t((attackRoll * 6) / 5);
      } else if (g.wearing('Dragon hunter wand')) {
        attackRoll = t((attackRoll * 7) / 4);
        this.note('Dragon hunter wand vs dragon');
      }
    }
    if (blackMaskBonus) {
      attackRoll = t((attackRoll * 23) / 20);
    }
    if (usingSpell?.name.includes('Demonbane') && hasAttribute(m, 'demon')) {
      let percent = p.markOfDarkness ? 40 : 20;
      if (g.wearing('Purging staff')) percent *= 2;
      attackRoll += t((attackRoll * this.demonbanePercent(percent)) / 100);
      this.note(`Demonbane spell vs demon${p.markOfDarkness ? ' (Mark of Darkness)' : ''}`);
    }
    if (g.revWeaponBuffApplicable()) {
      attackRoll = t((attackRoll * 3) / 2);
      this.note('Wilderness weapon vs wilderness target');
    }
    if (g.wearing('Tome of water') && usingSpell
      && (spellElement(usingSpell) === 'water' || isBindSpell(usingSpell))) {
      attackRoll = t((attackRoll * 6) / 5);
      this.note('Tome of water');
    }

    const element = spellElement(usingSpell);
    if (element && m.weakness && element.toLowerCase() === m.weakness.element?.toLowerCase()) {
      attackRoll += t((baseRoll * m.weakness.severity) / 100);
      this.note(`Elemental weakness: ${element} +${m.weakness.severity}%`);
    }
    return attackRoll;
  }

  private magicMinMax(): [number, number] {
    const { p, g, m } = this;
    const magicLevel = this.visibleMagic();
    const spell = this.castingSpell() || (!g.poweredStaff() && !g.salamander())
      ? this.spell : null;
    let maxHit = 0;
    let minHit = 0;

    if (spell) {
      maxHit = spellMaxHitAtLevel(spell, magicLevel, this.ctx.allSpells);
      if (spell.name === 'Magic Dart') {
        maxHit = g.wearing("Slayer's staff (e)") && this.onTask()
          ? t(13 + magicLevel / 6)
          : t(10 + magicLevel / 10);
      }
    } else {
      const staffMax = this.poweredStaffMaxHit(magicLevel);
      if (staffMax == null) {
        this.note('No spell selected and weapon has no built-in spell');
        return [0, 0];
      }
      maxHit = staffMax;
    }

    if (maxHit === 0) return [0, 0];

    if (g.wearing('Chaos gauntlets') && spell?.name.toLowerCase().includes('bolt')) {
      maxHit += 3;
      this.note('Chaos gauntlets');
    }
    if (g.chargeSpellApplicable(spell?.name ?? null)) {
      maxHit += 10;
      this.note('Charge with god cape');
    }

    const baseMax = maxHit;
    let magicDmgTenths = this.totals.magicStrTenths;

    if (g.smokeStaff() && spell?.spellbook === 'standard') {
      magicDmgTenths += 100;
    }

    let blackMaskBonus = false;
    if (g.salveEi() && this.isUndead()) {
      magicDmgTenths += 200;
    } else if (g.salveI() && this.isUndead()) {
      magicDmgTenths += 150;
    } else if (g.wearing('Amulet of avarice') && m.name?.startsWith('Revenant')) {
      magicDmgTenths += p.forinthrySurge ? 350 : 200;
    } else if (g.imbuedBlackMask() && this.onTask()) {
      blackMaskBonus = true;
    }

    magicDmgTenths += this.prayer().magicDamageTenths;

    maxHit += t((maxHit * magicDmgTenths) / 1000);

    if (blackMaskBonus) {
      maxHit = t((maxHit * 23) / 20);
    }
    if (hasAttribute(m, 'dragon')) {
      if (g.wearing('Dragon hunter lance')) {
        maxHit = t((maxHit * 6) / 5);
      } else if (g.wearing('Dragon hunter wand')) {
        maxHit = t((maxHit * 7) / 5);
      } else if (g.wearing('Dragon hunter crossbow')) {
        maxHit = t((maxHit * 5) / 4);
      }
    }
    if (g.revWeaponBuffApplicable()) {
      maxHit = t((maxHit * 3) / 2);
    }

    const element = spellElement(spell);
    if (element && m.weakness && element.toLowerCase() === m.weakness.element?.toLowerCase()) {
      maxHit += t((baseMax * m.weakness.severity) / 100);
    }

    if (p.sunfireRunes && spell && spellElement(spell) === 'fire') {
      minHit = t(maxHit / 10);
      this.note('Sunfire runes');
    }

    const shield = p.equipment.shield;
    const chargedShield = shield?.version === 'Charged';
    if (chargedShield
      && ((g.wearing('Tome of fire') && element === 'fire')
        || (g.wearing('Tome of water') && element === 'water')
        || (g.wearing('Tome of earth') && element === 'earth'))) {
      maxHit = t((maxHit * 11) / 10);
      this.note('Elemental tome');
    }

    if (C.P2_WARDEN_IDS.has(m.id)) {
      [minHit, maxHit] = this.applyP2WardensDamage(maxHit);
    }
    return [minHit, maxHit];
  }

  /** Built-in max hit of powered staves and salamanders; null if unknown. */
  private poweredStaffMaxHit(magicLevel: number): number | null {
    const w = this.g.weaponName();
    switch (w) {
      case 'Starter staff': return 8;
      case 'Trident of the seas':
      case 'Trident of the seas (e)': return Math.max(1, t(magicLevel / 3) - 5);
      case "Thammaron's sceptre": return Math.max(1, t(magicLevel / 3) - 8);
      case 'Accursed sceptre': return Math.max(1, t(magicLevel / 3) - 6);
      case 'Trident of the swamp':
      case 'Trident of the swamp (e)': return Math.max(1, t(magicLevel / 3) - 2);
      case 'Sanguinesti staff':
      case 'Holy sanguinesti staff': return Math.max(1, t(magicLevel / 3));
      case 'Dawnbringer': return Math.max(1, t(magicLevel / 6) - 1);
      case "Tumeken's shadow": return Math.max(1, t(magicLevel / 3)) + 1;
      case 'Eye of ayak': return Math.max(1, t(magicLevel / 3) - 6);
      case 'Warped sceptre': return Math.max(1, t((8 * magicLevel + 96) / 37));
      case 'Bone staff': return Math.max(1, t(magicLevel / 3) - 5) + 10;
      case 'Crystal staff (basic)':
      case 'Corrupted staff (basic)': return 23;
      case 'Crystal staff (attuned)':
      case 'Corrupted staff (attuned)': return 31;
      case 'Crystal staff (perfected)':
      case 'Corrupted staff (perfected)': return 39;
      case 'Swamp lizard': return t((magicLevel * (56 + 64) + 320) / 640);
      case 'Orange salamander': return t((magicLevel * (59 + 64) + 320) / 640);
      case 'Red salamander': return t((magicLevel * (77 + 64) + 320) / 640);
      case 'Black salamander': return t((magicLevel * (92 + 64) + 320) / 640);
      case 'Tecu salamander': return t((magicLevel * (104 + 64) + 320) / 640);
      default: return null;
    }
  }

  private applyP2WardensDamage(max: number): [number, number] {
    const reducedDefence = t(this.npcDefenceRoll() / 3);
    const delta = Math.max(this.maxAttackRoll() - reducedDefence, 0);
    const modifier = Math.max(15, Math.min(40, 15 + t((delta * 25) / 42000)));
    this.note('P2 Wardens damage modifier');
    return [t((max * modifier) / 100), t((max * (modifier + 20)) / 100)];
  }

  // --------------------------------------------------------------- accuracy

  maxAttackRoll(): number {
    switch (this.p.attackType) {
      case 'ranged': return this.rangedAttackRoll();
      case 'magic': return this.magicAttackRoll();
      default: return this.meleeAttackRoll();
    }
  }

  private minMax(): [number, number] {
    let mm: [number, number];
    switch (this.p.attackType) {
      case 'ranged': mm = this.rangedMinMax(); break;
      case 'magic': mm = this.magicMinMax(); break;
      default: mm = this.meleeMinMax();
    }
    if (mm[0] > mm[1]) mm[1] = mm[0];
    mm[0] = Math.max(mm[0], 0);
    mm[1] = Math.max(mm[1], 0);
    return mm;
  }

  hitChance(): number {
    const { m, g, p } = this;
    if (C.GUARANTEED_ACCURACY_MONSTERS.has(m.id)) return 1.0;
    if (m.id === 7223) return 1.0; // Scurrius giant rat
    if (C.P2_WARDEN_IDS.has(m.id)) return 1.0;
    if (this.alwaysMaxHits()) return 1.0;

    const atk = this.maxAttackRoll();
    const def = this.npcDefenceRoll();
    let chance = normalAccuracy(atk, def);

    if (g.fang() && p.attackType === 'stab') {
      chance = C.TOA_MONSTER_IDS.has(m.id)
        ? 1 - (1 - chance) ** 2
        : fangAccuracy(atk, def);
    }
    return chance;
  }

  displayHitChance(): number {
    let chance = this.hitChance();
    if (chance === 0.0 || chance === 1.0) return chance;
    if (this.p.attackType === 'magic' && this.g.wearing('Brimstone ring')
      && this.defenceRollOverride == null) {
      const effectChance = normalAccuracy(this.maxAttackRoll(), t((this.npcDefenceRoll() * 9) / 10));
      chance = 0.75 * chance + 0.25 * effectChance;
    }
    return chance;
  }

  private alwaysMaxHits(): boolean {
    const { m } = this;
    return (this.isMelee() && C.ALWAYS_MAX_HIT_MELEE.has(m.id))
      || (this.p.attackType === 'ranged' && C.ALWAYS_MAX_HIT_RANGED.has(m.id))
      || (this.p.attackType === 'magic' && C.ALWAYS_MAX_HIT_MAGIC.has(m.id));
  }

  // ------------------------------------------------------------ distribution

  attackerDist(): AttackDist {
    const { p, g, m } = this;
    const acc = this.hitChance();
    const [min, max] = this.minMax();

    if (max === 0) {
      return AttackDist.of(new HitDist([{ probability: 1.0, damage: 0, accurate: false }]));
    }

    if (C.ONE_HIT_MONSTERS.has(m.id)) {
      return AttackDist.of(HitDist.single(1.0, m.skills.hp));
    }

    const standard = HitDist.linear(acc, min, max);
    let dist = AttackDist.of(standard);

    if (p.attackType === 'ranged' && g.wearing('Tonalztics of ralos')
      && p.equipment.weapon?.version === 'Charged') {
      dist = new AttackDist([standard, standard]);
    }

    if (this.isMelee() && g.wearing('Gadderhammer') && hasAttribute(m, 'shade')) {
      dist = AttackDist.of(new HitDist([
        ...standard.scaleProbability(0.95).scaleDamage(5, 4).hits,
        ...standard.scaleProbability(0.05).scaleDamage(2).hits,
      ]));
      this.note('Gadderhammer vs shade');
    }

    if (p.attackType === 'ranged' && g.wearing('Dark bow')) {
      dist = new AttackDist([standard, standard]);
      this.note('Dark bow: two arrows');
    }

    if (this.isMelee() && g.veracSet()) {
      dist = AttackDist.of(new HitDist([
        ...standard.scaleProbability(0.75).hits,
        ...HitDist.linear(1.0, 1, max + 1).scaleProbability(0.25).hits,
      ]));
      this.note("Verac's set: 25% guaranteed hits");
    }

    if (p.attackType === 'ranged' && g.karilSet()) {
      dist = dist.transform((h) => [
        { ...h, probability: 0.75 },
        { probability: 0.25, damage: h.damage + t(h.damage / 2), accurate: true },
      ], false);
      this.note("Karil's set: 25% bonus hit");
    }

    if (this.isMelee() && g.scythe()) {
      const hits: HitDist[] = [];
      for (let i = 0; i < Math.min(Math.max(m.size, 1), 3); i++) {
        const splatMax = max >> i;
        hits.push(HitDist.linear(acc, min, Math.max(min, splatMax)));
      }
      dist = new AttackDist(hits);
      if (hits.length > 1) {
        this.note(`Scythe: ${hits.length} hits vs size ${m.size}`);
      }
    }

    if (this.isMelee() && g.wearing('Dual macuahuitl')) {
      const firstMax = t(max / 2);
      const secondMax = max - firstMax;
      const second = HitDist.linear(acc, min, Math.max(min, secondMax));
      const first = HitDist.linear(acc, min, Math.max(min, firstMax));
      dist = AttackDist.of(first.transform((h) =>
        second.hits.map((s) => ({
          probability: s.probability, damage: h.damage + s.damage, accurate: true,
        })), false));
      this.note('Dual macuahuitl');
    }

    if (this.isMelee() && g.wearing("Torag's hammers", 'Sulphur blades',
      'Glacial temotli', 'Earthbound tecpatl')) {
      const firstMax = t(max / 2);
      const secondMax = max - firstMax;
      dist = new AttackDist([
        HitDist.linear(acc, min, Math.max(min, firstMax)),
        HitDist.linear(acc, min, Math.max(min, secondMax)),
      ]);
      this.note('Two-hit weapon');
    }

    if (this.isMelee() && g.keris() && hasAttribute(m, 'kalphite')) {
      dist = AttackDist.of(new HitDist([
        ...standard.scaleProbability(50.0 / 51.0).hits,
        ...standard.scaleProbability(1.0 / 51.0).scaleDamage(3).hits,
      ]));
      this.note('Keris: 1/51 triple damage');
    }

    if (this.isMelee() && C.GUARDIAN_IDS.has(m.id) && g.pickaxe()) {
      const factor = 50 + this.ctx.character.mining + this.pickaxeBonus();
      dist = dist.transform(multiplyTransformer(factor, 150, 0), true);
      this.note('CoX Guardians: pickaxe/mining scaling');
    }

    // Sanguinesti staff: 20% of accurate hits deal +8 damage (Summer sweep up)
    if (g.wearing('Sanguinesti staff', 'Holy sanguinesti staff') && !this.castingSpell()) {
      dist = dist.transform((h) => [
        { ...h, probability: 0.8 },
        { probability: 0.2, damage: h.damage + 8, accurate: h.accurate },
      ], false);
      this.note('Sanguinesti staff: 20% chance of +8 damage');
    }

    const usingSpell = this.castingSpell() ? this.spell : null;
    if (p.markOfDarkness && usingSpell?.name.includes('Demonbane') && hasAttribute(m, 'demon')) {
      const percent = g.wearing('Purging staff') ? 50 : 25;
      const vulnerability = this.demonbaneVulnerability();
      dist = dist.transform((h) => [
        {
          ...h,
          probability: 1.0,
          damage: h.damage + t((t((h.damage * percent) / 100) * vulnerability) / 100),
        },
      ], true);
      this.note('Mark of Darkness demonbane bonus');
    }

    if (p.attackType === 'magic' && g.ahrimSet()) {
      dist = dist.transform((h) => [
        { ...h, probability: 0.75 },
        { probability: 0.25, damage: t((h.damage * 13) / 10), accurate: h.accurate },
      ], true);
      this.note("Ahrim's set: 25% +30% damage");
    }

    if (this.isMelee() && g.dharokSet()) {
      const maxHp = this.ctx.character.hitpoints;
      const currentHp = effectiveCurrentHp(this.ctx.character);
      dist = dist.scaleDamage(10000 + (maxHp - currentHp) * maxHp, 10000);
      this.note(`Dharok's set at ${currentHp}/${maxHp} HP`);
    }

    if (this.isMelee() && g.berserkerNecklace() && g.tzhaarWeapon()) {
      dist = dist.scaleDamage(6, 5);
      this.note('Berserker necklace + Tzhaar weapon');
    }

    if (hasAttribute(m, 'vampyre')) {
      dist = this.applyVampyreScaling(dist);
    }

    dist = this.applyBoltEffects(dist, max, false);

    // Seeking arrows: accurate hits deal at least 3 damage (bows only)
    if (p.attackType === 'ranged' && g.ammoName().includes('Seeking')
      && g.weaponCategory() === 'Bow') {
      dist = dist.transform((h) => [
        { probability: 1.0, damage: Math.max(h.damage, 3), accurate: h.accurate },
      ], false);
      this.note('Seeking arrows: minimum hit of 3');
    }

    // Accurate zero-damage hits deal 1 damage
    const accurateZeroApplies = this.spell == null || this.spell.max_hit > 0
      || g.poweredStaff() || g.salamander();
    if (accurateZeroApplies) {
      dist = dist.transform((h) => [
        { probability: 1.0, damage: Math.max(h.damage, 1), accurate: true },
      ], false);
    }

    if (p.attackType === 'magic' && usingSpell?.spellbook === 'standard'
      && g.wearing('Twinflame staff')
      && ['Bolt', 'Blast', 'Wave'].some((c) => usingSpell.name.includes(c))) {
      dist = dist.transform((h) => [
        { ...h, probability: 1.0, damage: h.damage + t((h.damage * 4) / 10) },
      ], true);
      this.note('Twinflame staff double hit');
    }

    if (m.name === 'Corporeal Beast' && !g.corpbaneWeapon()) {
      dist = dist.transform(divisionTransformer(2, 0), true);
      this.note('Corporeal Beast: half damage without corpbane weapon');
    }

    dist = this.applyBoltEffects(dist, max, true);

    if (p.attackType === 'magic' && g.wearing('Brimstone ring')
      && this.defenceRollOverride == null) {
      const effectDef = t((this.npcDefenceRoll() * 9) / 10);
      const effectDist = new Calc(p, m, this.ctx, effectDef).attackerDist();
      const mixed: HitDist[] = [];
      for (let i = 0; i < dist.dists.length; i++) {
        mixed.push(new HitDist([
          ...dist.dists[i].scaleProbability(0.75).hits,
          ...effectDist.dists[i].scaleProbability(0.25).hits,
        ]).flatten());
      }
      dist = new AttackDist(mixed);
      this.note('Brimstone ring');
    }

    if (this.alwaysMaxHits()) {
      return AttackDist.of(HitDist.single(1.0, dist.maxDamage()));
    }
    return dist;
  }

  private pickaxeBonus(): number {
    switch (this.g.weaponName()) {
      case 'Bronze pickaxe':
      case 'Iron pickaxe': return 1;
      case 'Steel pickaxe': return 6;
      case 'Black pickaxe': return 11;
      case 'Mithril pickaxe': return 21;
      case 'Adamant pickaxe': return 31;
      case 'Rune pickaxe':
      case 'Gilded pickaxe': return 41;
      default: return 61;
    }
  }

  private isVampyreTier(tier: number): boolean {
    return (this.m.attributes ?? []).includes(`vampyre${tier}`);
  }

  private applyVampyreScaling(dist: AttackDist): AttackDist {
    const { g } = this;
    const efaritay = g.wearing("Efaritay's aid");
    const applyEfaritay = (d: AttackDist) => (efaritay ? d.scaleDamage(11, 10) : d);

    if (g.wearing('Blisterwood flail')) {
      dist = applyEfaritay(dist).scaleDamage(5, 4);
      this.note('Blisterwood flail vs vampyre');
    } else if (g.wearing('Blisterwood sickle')) {
      dist = applyEfaritay(dist).scaleDamage(23, 20);
      this.note('Blisterwood sickle vs vampyre');
    } else if (g.wearing('Ivandis flail')) {
      dist = applyEfaritay(dist).scaleDamage(6, 5);
      this.note('Ivandis flail vs vampyre');
    } else if (g.wearing('Rod of ivandis') && !this.isVampyreTier(3)) {
      dist = applyEfaritay(dist).scaleDamage(11, 10);
      this.note('Rod of ivandis vs vampyre');
    } else if (g.silverWeapon() && this.isVampyreTier(1)) {
      dist = applyEfaritay(dist).scaleDamage(11, 10);
      this.note('Silver weapon vs vampyre');
    }
    return dist;
  }

  private applyBoltEffects(dist: AttackDist, max: number, rubyPass: boolean): AttackDist {
    const { p, g, m } = this;
    if (p.attackType !== 'ranged' || g.weaponCategory() !== 'Crossbow') return dist;
    const zcb = g.wearing('Zaryte crossbow');
    const procScale = p.kandarinDiary ? 1.1 : 1.0;
    const rangedLvl = this.visibleRanged();

    if (rubyPass) {
      if (g.wearing('Ruby bolts (e)', 'Ruby dragon bolts (e)')) {
        const chance = 0.06 * procScale;
        const cap = C.INFINITE_HEALTH_MONSTERS.has(m.id) ? (zcb ? 66 : 60) : (zcb ? 110 : 100);
        const effectDmg = Math.min(cap, t((m.skills.hp * (zcb ? 22 : 20)) / 100));
        dist = dist.transform((h) => [
          { probability: chance, damage: effectDmg, accurate: true },
          { ...h, probability: 1 - chance },
        ], true);
        this.note('Ruby bolts (e)');
      }
      return dist;
    }

    const bonusProc = (d: AttackDist, chance: number, bonus: number, accurateOnly: boolean) =>
      d.transform((h) => [
        { probability: chance, damage: h.damage + bonus, accurate: h.accurate },
        { ...h, probability: 1 - chance },
      ], !accurateOnly);

    if (g.wearing('Opal bolts (e)', 'Opal dragon bolts (e)')) {
      dist = bonusProc(dist, 0.05 * procScale, t(rangedLvl / (zcb ? 9 : 10)), false);
      this.note('Opal bolts (e)');
    } else if (g.wearing('Pearl bolts (e)', 'Pearl dragon bolts (e)')) {
      const divisor = hasAttribute(m, 'fiery') ? 15 : 20;
      dist = bonusProc(dist, 0.06 * procScale, t(rangedLvl / (zcb ? divisor - 2 : divisor)), false);
      this.note('Pearl bolts (e)');
    } else if (g.wearing('Diamond bolts (e)', 'Diamond dragon bolts (e)')) {
      const chance = 0.1 * procScale;
      const effect = HitDist.linear(1.0, 0, t((max * (zcb ? 126 : 115)) / 100));
      dist = dist.transform((h) => [
        ...effect.scaleProbability(chance).hits,
        { ...h, probability: 1 - chance },
      ], true);
      this.note('Diamond bolts (e)');
    } else if (g.wearing('Dragonstone bolts (e)', 'Dragonstone dragon bolts (e)')
      && !hasAttribute(m, 'fiery') && !hasAttribute(m, 'dragon')) {
      dist = bonusProc(dist, 0.06 * procScale, t((rangedLvl * 2) / (zcb ? 9 : 10)), true);
      this.note('Dragonstone bolts (e)');
    } else if (g.wearing('Onyx bolts (e)', 'Onyx dragon bolts (e)') && !this.isUndead()) {
      const chance = 0.11 * procScale;
      const effect = HitDist.linear(1.0, 0, t((max * (zcb ? 132 : 120)) / 100));
      dist = dist.transform((h) => [
        ...effect.scaleProbability(chance).hits,
        { ...h, probability: 1 - chance },
      ], false);
      this.note('Onyx bolts (e)');
    }
    return dist;
  }

  // -------------------------------------------------------- NPC transforms

  applyNpcTransforms(dist: AttackDist): AttackDist {
    const { p, g, m } = this;
    const style = p.attackType;

    if (this.isImmune()) {
      this.note('Target is immune to this attack style/weapon');
      return AttackDist.of(new HitDist([{ probability: 1.0, damage: 0, accurate: false }]));
    }

    const usingSpell = this.castingSpell() ? this.spell : null;
    if (m.name === 'Zulrah') {
      dist = dist.transform(cappedRerollTransformer(50, 5, 45), true);
      this.note('Zulrah: hits above 50 rerolled to 45-50');
    }
    if (m.name === 'Fragment of Seren') {
      dist = dist.transform(linearMinTransformer(2, 22), true);
    }
    if ((m.name === 'Kraken' || m.name === 'Cave kraken') && style === 'ranged') {
      dist = dist.transform(divisionTransformer(7, 1), true);
      this.note('Kraken: ranged damage divided by 7');
    }
    if (C.VERZIK_P1_IDS.has(m.id) && !g.wearing('Dawnbringer')) {
      dist = dist.transform(linearMinTransformer(this.isMelee() ? 10 : 3, 0), true);
      this.note('Verzik P1 damage cap without Dawnbringer');
    }
    if (C.TEKTON_IDS.has(m.id) && style === 'magic') {
      dist = dist.transform(divisionTransformer(5, 1), true);
      this.note('Tekton: magic damage divided by 5');
    }
    if (C.GLOWING_CRYSTAL_IDS.has(m.id) && style === 'magic') {
      dist = dist.transform(divisionTransformer(3, 0), true);
    }
    if ((C.OLM_MELEE_HAND_IDS.has(m.id) || C.OLM_HEAD_IDS.has(m.id)) && style === 'magic') {
      dist = dist.transform(divisionTransformer(3, 0), true);
      this.note('Olm: magic damage divided by 3');
    }
    if ((C.OLM_MAGE_HAND_IDS.has(m.id) || C.OLM_MELEE_HAND_IDS.has(m.id)) && style === 'ranged') {
      dist = dist.transform(divisionTransformer(3, 0), true);
      this.note('Olm: ranged damage divided by 3');
    }
    if (C.ICE_DEMON_IDS.has(m.id) && spellElement(usingSpell) !== 'fire'
      && !usingSpell?.name.includes('Demonbane')) {
      dist = dist.transform(divisionTransformer(3, 0), true);
      this.note('Ice demon: non-fire damage divided by 3');
    }
    if (m.name === 'Slagilith' && !g.pickaxe()) {
      dist = dist.transform(divisionTransformer(3, 0), true);
    }
    if (C.NIGHTMARE_TOTEM_IDS.has(m.id) && style === 'magic') {
      dist = dist.transform(multiplyTransformer(2, 1, 0), true);
      this.note('Nightmare totem: double magic damage');
    }
    if (m.name && ['Slash Bash', 'Zogre', 'Skogre'].includes(m.name)) {
      if (usingSpell?.name === 'Crumble Undead') {
        dist = dist.transform(divisionTransformer(2, 0), true);
      } else if (style !== 'ranged'
        || !g.ammoName().includes(' brutal')
        || g.weaponName() !== 'Comp ogre bow') {
        dist = dist.transform(divisionTransformer(4, 0), true);
      }
      this.note('Zogre damage reduction');
    }
    if (this.isVampyreTier(2)) {
      if (!g.vampyrebane(true) && g.wearing("Efaritay's aid")) {
        dist = dist.transform(divisionTransformer(2, 0), true);
        this.note('Vampyre T2: half damage with Efaritay\'s aid');
      } else if (g.silverWeapon()) {
        dist = dist.transform(flatLimitTransformer(10, 0), true);
        this.note('Vampyre T2: damage capped at 10 with silver weapon');
      }
    }

    const flatArmour = m.defensive.flat_armour;
    if (flatArmour > 0 && style !== 'magic') {
      dist = dist.transform(addTransformer(-flatArmour, 0), false);
      this.note(`Flat armour ${flatArmour}`);
    }
    return dist;
  }

  isImmune(): boolean {
    const { p, g, m } = this;
    const style = p.attackType;

    if (C.IMMUNE_TO_MAGIC_IDS.has(m.id) && style === 'magic') return true;
    if (C.IMMUNE_TO_RANGED_IDS.has(m.id) && style === 'ranged') return true;
    if (C.IMMUNE_TO_MELEE_IDS.has(m.id) && this.isMelee()) {
      if (!(C.ZULRAH_IDS.has(m.id) && g.polearm())) return true;
    }
    if (hasAttribute(m, 'flying') && this.isMelee()) {
      if (C.VESPULA_IDS.has(m.id)) return true;
      if (!g.polearm() && !g.salamander()) return true;
    }
    if (C.IMMUNE_TO_NON_SALAMANDER_MELEE_IDS.has(m.id) && this.isMelee() && !g.salamander()) {
      return true;
    }
    if (this.isVampyreTier(3) && !g.vampyrebane(false)) return true;
    if (this.isVampyreTier(2) && !g.vampyrebane(true) && !g.wearing("Efaritay's aid")
      && !g.silverWeapon()) {
      return true;
    }
    if (C.GUARDIAN_IDS.has(m.id) && (!this.isMelee() || !g.pickaxe())) return true;
    if (hasAttribute(m, 'leafy') && !g.leafBladedWeapon(this.spell?.name ?? null)) return true;
    return !hasAttribute(m, 'rat') && g.ratBoneWeapon();
  }

  // -------------------------------------------------------------------- run

  run(): DpsResult {
    const { m } = this;
    if (hasAttribute(m, 'xerician') && ((m.partySize ?? 1) > 1 || m.coxChallengeMode)) {
      this.note(`CoX scaling: ${m.partySize} players${m.coxChallengeMode ? ', Challenge Mode' : ''}`);
    }
    const dist = this.applyNpcTransforms(this.attackerDist());
    const accuracy = this.displayHitChance();
    const expected = dist.expectedDamage();
    const maxHit = dist.maxDamage();
    const speed = attackSpeedTicks(this.p, this.spell);
    const dps = expected / (speed * C.SECONDS_PER_TICK);
    const htk = expectedHitsToKill(dist, Math.max(1, m.skills.hp));
    const ttk = htk > 0 ? htk * speed * C.SECONDS_PER_TICK : Infinity;
    return {
      maxHit,
      accuracy,
      avgDamagePerAttack: expected,
      attackSpeedTicks: speed,
      dps,
      expectedHitsToKill: htk,
      ttkSeconds: ttk,
      notes: this.notes,
    };
  }
}

export const tbowScaling = (current: number, magic: number, accuracyMode: boolean): number => {
  const factor = accuracyMode ? 10 : 14;
  const base = accuracyMode ? 140 : 250;
  const t2 = t((3 * magic - factor) / 100);
  const t3 = t((t((3 * magic) / 10) - 10 * factor) ** 2 / 100);
  const bonus = base + t2 - t3;
  return t((current * bonus) / 100);
};

export const normalAccuracy = (atk: number, def: number): number => {
  if (atk >= 0 && def >= 0) {
    return atk > def
      ? 1 - (def + 2.0) / (2.0 * (atk + 1))
      : atk / (2.0 * (def + 1));
  }
  if (atk < 0) atk = Math.min(0, atk + 2);
  if (def < 0) def = Math.min(0, def + 2);
  if (atk >= 0) return 1 - 1.0 / (-def + 1) / (atk + 1);
  if (def >= 0) return 0;
  const a = -def;
  const d = -atk;
  return a > d
    ? 1 - (d + 2.0) / (2.0 * (a + 1))
    : a / (2.0 * (d + 1));
};

/** The fang's double-roll accuracy (exact formula). */
export const fangAccuracy = (atk: number, def: number): number => {
  if (atk >= 0 && def >= 0) {
    if (atk > def) {
      return 1 - ((def + 2.0) * (2 * def + 3.0)) / (atk + 1) / (atk + 1) / 6;
    }
    return (atk * (4.0 * atk + 5)) / 6 / (atk + 1) / (def + 1);
  }
  if (atk < 0) atk = Math.min(0, atk + 2);
  if (def < 0) def = Math.min(0, def + 2);
  if (atk >= 0) return 1 - 1.0 / (-def + 1) / (atk + 1);
  if (def >= 0) return 0;
  const a = -def;
  const d = -atk;
  if (a < d) {
    return (a * (d * 6.0 - 2 * a + 5)) / 6 / (d + 1) / (d + 1);
  }
  return 1 - ((d + 2.0) * (2 * d + 3.0)) / (a + 1) / (a + 1) / 6;
};

/** Expected attacks to kill via DP over the total damage histogram. */
const expectedHitsToKill = (dist: AttackDist, startHp: number): number => {
  const hist = dist.totalDamageHistogram();
  const zeroProb = hist.get(0) ?? 0;
  if (1 - zeroProb < 1e-9) return 0;
  const maxDmg = dist.maxDamage();
  const htk = new Float64Array(startHp + 1);
  for (let hp = 1; hp <= startHp; hp++) {
    let val = 1.0;
    const limit = Math.min(hp, maxDmg);
    for (let dmg = 1; dmg <= limit; dmg++) {
      const prob = hist.get(dmg);
      if (prob) val += prob * htk[hp - dmg];
    }
    htk[hp] = val / (1 - zeroProb);
  }
  return htk[startHp];
};

/** Main entry point: full DPS result for one setup vs one monster. */
export const calculate = (p: PlayerSetup, monster: Monster, ctx: CalcContext): DpsResult =>
  new Calc(p, scaleCoxMonster(monster), ctx).run();
