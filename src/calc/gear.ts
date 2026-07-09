import type { EquipmentItem, EquipmentSlotName, PlayerSetup } from '../model/types';

/** Name-based detection of gear with special effects (canonical wiki names). */
export class Gear {
  constructor(private readonly p: PlayerSetup) {}

  private nameAt(slot: EquipmentSlotName): string {
    return this.p.equipment[slot]?.name ?? '';
  }

  wearing(...names: string[]): boolean {
    for (const item of Object.values(this.p.equipment)) {
      if (item && names.includes(item.name)) return true;
    }
    return false;
  }

  wearingAll(...names: string[]): boolean {
    return names.every((name) =>
      Object.values(this.p.equipment).some((item) => item?.name === name));
  }

  weaponName(): string {
    return this.nameAt('weapon');
  }

  weaponCategory(): string {
    return this.p.equipment.weapon?.category ?? '';
  }

  ammoName(): string {
    return this.nameAt('ammo');
  }

  private isMelee(): boolean {
    return ['stab', 'slash', 'crush'].includes(this.p.attackType);
  }

  // --- Void ---

  voidRobes(): boolean {
    return this.wearing('Void knight top', 'Void knight top (or)', 'Elite void top', 'Elite void top (or)')
      && this.wearing('Void knight robe', 'Void knight robe (or)', 'Elite void robe', 'Elite void robe (or)')
      && this.wearing('Void knight gloves', 'Void knight gloves (or)');
  }

  eliteVoidRobes(): boolean {
    return this.wearing('Elite void top', 'Elite void top (or)')
      && this.wearing('Elite void robe', 'Elite void robe (or)')
      && this.wearing('Void knight gloves', 'Void knight gloves (or)');
  }

  meleeVoid(): boolean {
    return this.voidRobes() && this.wearing('Void melee helm', 'Void melee helm (or)');
  }

  rangedVoid(): boolean {
    return this.voidRobes() && this.wearing('Void ranger helm', 'Void ranger helm (or)');
  }

  eliteRangedVoid(): boolean {
    return this.eliteVoidRobes() && this.wearing('Void ranger helm', 'Void ranger helm (or)');
  }

  magicVoid(): boolean {
    return this.voidRobes() && this.wearing('Void mage helm', 'Void mage helm (or)');
  }

  eliteMagicVoid(): boolean {
    return this.eliteVoidRobes() && this.wearing('Void mage helm', 'Void mage helm (or)');
  }

  // --- Slayer / salve ---

  imbuedBlackMask(): boolean {
    const head = this.nameAt('head');
    return head.startsWith('Black mask (i)') || head.startsWith('Slayer helmet (i)');
  }

  blackMask(): boolean {
    const head = this.nameAt('head');
    return this.imbuedBlackMask() || head.startsWith('Black mask') || head.startsWith('Slayer helmet');
  }

  salve(): boolean {
    return this.nameAt('neck') === 'Salve amulet';
  }

  salveE(): boolean {
    return this.nameAt('neck') === 'Salve amulet (e)';
  }

  salveI(): boolean {
    return this.nameAt('neck') === 'Salve amulet(i)';
  }

  salveEi(): boolean {
    return this.nameAt('neck') === 'Salve amulet(ei)';
  }

  // --- Weapons ---

  fang(): boolean {
    return this.wearing("Osmumten's fang", "Osmumten's fang (or)");
  }

  scythe(): boolean {
    return this.weaponName().includes('of vitur');
  }

  twistedBow(): boolean {
    return this.wearing('Twisted bow');
  }

  tumekensShadow(): boolean {
    return this.weaponName() === "Tumeken's shadow";
  }

  crystalBow(): boolean {
    return this.wearing('Crystal bow') || this.weaponName().includes('Bow of faerdhinen');
  }

  keris(): boolean {
    return this.weaponName().includes('Keris');
  }

  tzhaarWeapon(): boolean {
    return this.wearing('Tzhaar-ket-em', 'Tzhaar-ket-om', 'Tzhaar-ket-om (t)',
      'Toktz-xil-ak', 'Toktz-xil-ek', 'Toktz-mej-tal');
  }

  obsidianArmour(): boolean {
    return this.wearingAll('Obsidian helmet', 'Obsidian platelegs', 'Obsidian platebody');
  }

  berserkerNecklace(): boolean {
    return this.wearing('Berserker necklace', 'Berserker necklace (or)');
  }

  ratBoneWeapon(): boolean {
    return this.wearing('Bone mace', 'Bone shortbow', 'Bone staff');
  }

  smokeStaff(): boolean {
    return this.wearing('Smoke battlestaff', 'Mystic smoke staff', 'Twinflame staff');
  }

  silverWeapon(): boolean {
    if (this.ammoName().startsWith('Silver bolts') && this.p.attackType === 'ranged') {
      return true;
    }
    return this.isMelee() && this.wearing('Blessed axe', 'Ivandis flail', 'Blisterwood flail',
      'Silver sickle', 'Silver sickle (b)', 'Emerald sickle', 'Emerald sickle (b)',
      'Enchanted emerald sickle (b)', 'Ruby sickle (b)', 'Enchanted ruby sickle (b)',
      'Blisterwood sickle', 'Silverlight', 'Darklight', 'Arclight', 'Rod of ivandis', 'Wolfbane');
  }

  vampyrebane(tier2: boolean): boolean {
    if (!tier2 && !this.isMelee()) return false;
    return (tier2 && this.wearing('Rod of ivandis'))
      || this.wearing('Ivandis flail', 'Blisterwood sickle', 'Blisterwood flail',
        'Hallowed flail', 'Sunspear');
  }

  leafBladedWeapon(spellName: string | null): boolean {
    if (this.isMelee() && this.wearing('Leaf-bladed battleaxe', 'Leaf-bladed spear', 'Leaf-bladed sword')) {
      return true;
    }
    if (spellName === 'Magic Dart') return true;
    return this.p.attackType === 'ranged'
      && this.wearing('Broad arrows', 'Broad bolts', 'Amethyst broad bolts');
  }

  corpbaneWeapon(): boolean {
    const weapon = this.weaponName();
    const stab = this.p.attackType === 'stab';
    if (this.fang()) return stab;
    if (weapon.endsWith('halberd')) return stab;
    if (weapon.includes('spear') && weapon !== 'Blue moon spear') return stab;
    return false;
  }

  revWeaponBuffApplicable(): boolean {
    if (!this.p.inWilderness) return false;
    switch (this.p.attackType) {
      case 'magic':
        return this.wearing('Accursed sceptre', 'Accursed sceptre (a)',
          "Thammaron's sceptre", "Thammaron's sceptre (a)");
      case 'ranged':
        return this.wearing("Craw's bow", 'Webweaver bow');
      default:
        return this.wearing('Ursine chainmace', "Viggora's chainmace");
    }
  }

  chargeSpellApplicable(spellName: string | null): boolean {
    if (!this.p.chargeSpell || !spellName) return false;
    switch (spellName) {
      case 'Saradomin Strike':
        return this.wearing('Saradomin cape', 'Imbued saradomin cape',
          'Saradomin max cape', 'Imbued saradomin max cape');
      case 'Claws of Guthix':
        return this.wearing('Guthix cape', 'Imbued guthix cape',
          'Guthix max cape', 'Imbued guthix max cape');
      case 'Flames of Zamorak':
        return this.wearing('Zamorak cape', 'Imbued zamorak cape',
          'Zamorak max cape', 'Imbued zamorak max cape');
      default:
        return false;
    }
  }

  // --- Sets ---

  dharokSet(): boolean {
    return this.wearingAll("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
  }

  veracSet(): boolean {
    return this.wearingAll("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
  }

  karilSet(): boolean {
    return this.wearingAll("Karil's coif", "Karil's leathertop", "Karil's leatherskirt",
      "Karil's crossbow", 'Amulet of the damned');
  }

  ahrimSet(): boolean {
    return this.wearingAll("Ahrim's staff", "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt",
      'Amulet of the damned');
  }

  inquisitorPieces(): number {
    let pieces = 0;
    if (this.nameAt('head').startsWith("Inquisitor's great helm")) pieces++;
    if (this.nameAt('body').startsWith("Inquisitor's hauberk")) pieces++;
    if (this.nameAt('legs').startsWith("Inquisitor's plateskirt")) pieces++;
    return pieces;
  }

  inquisitorsMace(): boolean {
    return this.weaponName().startsWith("Inquisitor's mace");
  }

  /** Crystal armour weighted as the game does: helm 1, legs 2, body 3. */
  crystalArmourWeight(): number {
    let weight = 0;
    if (this.nameAt('head').startsWith('Crystal helm')) weight += 1;
    if (this.nameAt('legs').startsWith('Crystal legs')) weight += 2;
    if (this.nameAt('body').startsWith('Crystal body')) weight += 3;
    return weight;
  }

  virtusPieces(): number {
    let pieces = 0;
    for (const slot of ['head', 'body', 'legs'] as EquipmentSlotName[]) {
      if (this.nameAt(slot).includes('Virtus')) pieces++;
    }
    return pieces;
  }

  poweredStaff(): boolean {
    return ['Powered Staff', 'Powered Wand'].includes(this.weaponCategory());
  }

  salamander(): boolean {
    return this.weaponCategory() === 'Salamander';
  }

  chinchompa(): boolean {
    return this.weaponCategory() === 'Chinchompas';
  }

  pickaxe(): boolean {
    return this.weaponCategory() === 'Pickaxe';
  }

  polearm(): boolean {
    return this.weaponCategory() === 'Polearm';
  }
}

export type { EquipmentItem };
