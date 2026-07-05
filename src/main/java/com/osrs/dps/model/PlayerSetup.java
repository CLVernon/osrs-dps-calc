package com.osrs.dps.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * A gear loadout: equipment, style, prayers, potions and situational flags.
 * Stats come from the shared {@link PlayerCharacter} - all setups compare
 * gear for the same character.
 */
public class PlayerSetup {

    private String name = "New setup";

    /** The character whose stats this loadout uses; shared between setups. */
    private PlayerCharacter character = new PlayerCharacter();

    private final Map<EquipmentSlot, EquipmentItem> equipment = new EnumMap<>(EquipmentSlot.class);

    private AttackType attackType = AttackType.CRUSH;
    private Stance stance = Stance.ACCURATE;
    /** Name of the selected weapon combat style (e.g. "Flick"). */
    private String styleName = "Punch";
    private Prayer prayer = Prayer.NONE;
    private Potion potion = Potion.NONE;
    private SpellData spell;
    private boolean onSlayerTask;
    private boolean inWilderness;
    private boolean forinthrySurge;
    private boolean markOfDarkness;
    private boolean chargeSpell;
    private boolean kandarinDiary;
    private boolean sunfireRunes;
    private int chinchompaDistance = 5;

    public PlayerSetup() {
    }

    public PlayerSetup(String name) {
        this.name = name;
    }

    public PlayerSetup copy() {
        PlayerSetup c = new PlayerSetup(name + " (copy)");
        c.character = character; // same character, different gear
        c.equipment.putAll(equipment);
        c.attackType = attackType;
        c.stance = stance;
        c.styleName = styleName;
        c.prayer = prayer;
        c.potion = potion;
        c.spell = spell;
        c.onSlayerTask = onSlayerTask;
        c.inWilderness = inWilderness;
        c.forinthrySurge = forinthrySurge;
        c.markOfDarkness = markOfDarkness;
        c.chargeSpell = chargeSpell;
        c.kandarinDiary = kandarinDiary;
        c.sunfireRunes = sunfireRunes;
        c.chinchompaDistance = chinchompaDistance;
        return c;
    }

    public EquipmentItem getEquipped(EquipmentSlot slot) {
        return equipment.get(slot);
    }

    public void setEquipped(EquipmentSlot slot, EquipmentItem item) {
        if (item == null) {
            equipment.remove(slot);
        } else {
            equipment.put(slot, item);
            // A two-handed weapon and a shield cannot be worn together.
            if (slot == EquipmentSlot.WEAPON && item.twoHanded) {
                equipment.remove(EquipmentSlot.SHIELD);
            } else if (slot == EquipmentSlot.SHIELD) {
                EquipmentItem weapon = equipment.get(EquipmentSlot.WEAPON);
                if (weapon != null && weapon.twoHanded) {
                    equipment.remove(EquipmentSlot.WEAPON);
                }
            }
        }
    }

    public Map<EquipmentSlot, EquipmentItem> getEquipment() {
        return equipment;
    }

    public EquipmentItem getWeapon() {
        return equipment.get(EquipmentSlot.WEAPON);
    }

    // --- Aggregated equipment bonuses ---

    public int attackBonus(AttackType type) {
        int total = 0;
        for (EquipmentItem item : equipment.values()) {
            total += switch (type) {
                case STAB -> item.offensive.stab;
                case SLASH -> item.offensive.slash;
                case CRUSH -> item.offensive.crush;
                case RANGED -> item.offensive.ranged;
                case MAGIC -> item.offensive.magic;
            };
        }
        return total;
    }

    public int meleeStrengthBonus() {
        return equipment.values().stream().mapToInt(i -> i.bonuses.str).sum();
    }

    public int rangedStrengthBonus() {
        return equipment.values().stream().mapToInt(i -> i.bonuses.rangedStr).sum();
    }

    /** Total magic damage bonus in tenths of a percent (e.g. 50 = +5.0%). */
    public int magicDamageBonusTenths() {
        return equipment.values().stream().mapToInt(i -> i.bonuses.magicStr).sum();
    }

    /** Weapon attack speed in ticks including stance adjustment; unarmed is 4 ticks. */
    public int attackSpeedTicks() {
        EquipmentItem weapon = getWeapon();
        int base = weapon != null && weapon.speed > 0 ? weapon.speed : 4;
        if (attackType == AttackType.RANGED && stance == Stance.RAPID) {
            base -= 1;
        } else if (isCastingSpell()) {
            String weaponName = weapon == null ? "" : weapon.name;
            if ("Harmonised nightmare staff".equals(weaponName)
                    && spell != null && "standard".equals(spell.spellbook)
                    && stance != Stance.MANUAL_CAST) {
                base = 4;
            } else if ("Twinflame staff".equals(weaponName)) {
                base = 6;
            } else {
                base = 5;
            }
        }
        return Math.max(1, base);
    }

    /** True when attacking with a cast spell (autocast or manual) rather than a powered staff. */
    public boolean isCastingSpell() {
        return attackType == AttackType.MAGIC && spell != null
                && (stance == Stance.AUTOCAST || stance == Stance.DEFENSIVE_AUTOCAST
                        || stance == Stance.MANUAL_CAST);
    }

    /** The currently selected weapon combat style. */
    public CombatStyle getCombatStyle() {
        for (CombatStyle style : WeaponStyles.forWeapon(getWeapon())) {
            if (style.name().equals(styleName) && style.type() == attackType
                    && style.stance() == stance) {
                return style;
            }
        }
        return new CombatStyle(styleName, attackType, stance);
    }

    /** Selects a weapon combat style, which determines attack type and stance. */
    public void setCombatStyle(CombatStyle style) {
        this.styleName = style.name();
        this.attackType = style.type();
        this.stance = style.stance();
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    // --- Boosted (visible) levels ---

    public int visibleAttack() {
        return character.attack + potion.attackBoost(character.attack);
    }

    public int visibleStrength() {
        return character.strength + potion.strengthBoost(character.strength);
    }

    public int visibleRanged() {
        return character.ranged + potion.rangedBoost(character.ranged);
    }

    public int visibleMagic() {
        return character.magic + potion.magicBoost(character.magic);
    }

    // --- Getters / setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlayerCharacter getCharacter() {
        return character;
    }

    public void setCharacter(PlayerCharacter character) {
        this.character = character != null ? character : new PlayerCharacter();
    }

    public int getAttackLevel() {
        return character.attack;
    }

    public void setAttackLevel(int attackLevel) {
        character.attack = attackLevel;
    }

    public int getStrengthLevel() {
        return character.strength;
    }

    public void setStrengthLevel(int strengthLevel) {
        character.strength = strengthLevel;
    }

    public int getDefenceLevel() {
        return character.defence;
    }

    public void setDefenceLevel(int defenceLevel) {
        character.defence = defenceLevel;
    }

    public int getRangedLevel() {
        return character.ranged;
    }

    public void setRangedLevel(int rangedLevel) {
        character.ranged = rangedLevel;
    }

    public int getMagicLevel() {
        return character.magic;
    }

    public void setMagicLevel(int magicLevel) {
        character.magic = magicLevel;
    }

    public int getHitpointsLevel() {
        return character.hitpoints;
    }

    public void setHitpointsLevel(int hitpointsLevel) {
        character.hitpoints = hitpointsLevel;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public void setAttackType(AttackType attackType) {
        this.attackType = attackType;
    }

    public Stance getStance() {
        return stance;
    }

    public void setStance(Stance stance) {
        this.stance = stance;
    }

    public Prayer getPrayer() {
        return prayer;
    }

    public void setPrayer(Prayer prayer) {
        this.prayer = prayer;
    }

    public Potion getPotion() {
        return potion;
    }

    public void setPotion(Potion potion) {
        this.potion = potion;
    }

    public SpellData getSpell() {
        return spell;
    }

    public void setSpell(SpellData spell) {
        this.spell = spell;
    }

    /** Current HP for Dharok's; defaults to full health when unset. */
    public int getCurrentHitpoints() {
        return character.effectiveCurrentHitpoints();
    }

    public void setCurrentHitpoints(int currentHitpoints) {
        character.currentHitpoints = currentHitpoints;
    }

    public int getMiningLevel() {
        return character.mining;
    }

    public void setMiningLevel(int miningLevel) {
        character.mining = miningLevel;
    }

    public boolean isInWilderness() {
        return inWilderness;
    }

    public void setInWilderness(boolean inWilderness) {
        this.inWilderness = inWilderness;
    }

    public boolean isForinthrySurge() {
        return forinthrySurge;
    }

    public void setForinthrySurge(boolean forinthrySurge) {
        this.forinthrySurge = forinthrySurge;
    }

    public boolean isMarkOfDarkness() {
        return markOfDarkness;
    }

    public void setMarkOfDarkness(boolean markOfDarkness) {
        this.markOfDarkness = markOfDarkness;
    }

    public boolean isChargeSpell() {
        return chargeSpell;
    }

    public void setChargeSpell(boolean chargeSpell) {
        this.chargeSpell = chargeSpell;
    }

    public boolean isKandarinDiary() {
        return kandarinDiary;
    }

    public void setKandarinDiary(boolean kandarinDiary) {
        this.kandarinDiary = kandarinDiary;
    }

    public boolean isSunfireRunes() {
        return sunfireRunes;
    }

    public void setSunfireRunes(boolean sunfireRunes) {
        this.sunfireRunes = sunfireRunes;
    }

    public int getChinchompaDistance() {
        return chinchompaDistance;
    }

    public void setChinchompaDistance(int chinchompaDistance) {
        this.chinchompaDistance = chinchompaDistance;
    }

    public boolean isOnSlayerTask() {
        return onSlayerTask;
    }

    public void setOnSlayerTask(boolean onSlayerTask) {
        this.onSlayerTask = onSlayerTask;
    }

    @Override
    public String toString() {
        return name;
    }
}
