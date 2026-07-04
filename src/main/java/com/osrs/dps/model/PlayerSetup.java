package com.osrs.dps.model;

import java.util.EnumMap;
import java.util.Map;

/** A complete player loadout: stats, gear, style, prayers, potions and situational flags. */
public class PlayerSetup {

    private String name = "New setup";

    private int attackLevel = 99;
    private int strengthLevel = 99;
    private int defenceLevel = 99;
    private int rangedLevel = 99;
    private int magicLevel = 99;
    private int hitpointsLevel = 99;
    /** Current HP, for Dharok's set effect; 0 means "at full health". */
    private int currentHitpoints;
    private int miningLevel = 99;

    private final Map<EquipmentSlot, EquipmentItem> equipment = new EnumMap<>(EquipmentSlot.class);

    private AttackType attackType = AttackType.SLASH;
    private Stance stance = Stance.ACCURATE;
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
        c.attackLevel = attackLevel;
        c.strengthLevel = strengthLevel;
        c.defenceLevel = defenceLevel;
        c.rangedLevel = rangedLevel;
        c.magicLevel = magicLevel;
        c.hitpointsLevel = hitpointsLevel;
        c.currentHitpoints = currentHitpoints;
        c.miningLevel = miningLevel;
        c.equipment.putAll(equipment);
        c.attackType = attackType;
        c.stance = stance;
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
                    && spell != null && "standard".equals(spell.spellbook)) {
                base = 4;
            } else if ("Twinflame staff".equals(weaponName)) {
                base = 6;
            } else {
                base = 5;
            }
        }
        return Math.max(1, base);
    }

    /** True when attacking with an autocast spell rather than a powered staff. */
    public boolean isCastingSpell() {
        return attackType == AttackType.MAGIC && spell != null
                && (stance == Stance.AUTOCAST || stance == Stance.DEFENSIVE_AUTOCAST);
    }

    // --- Boosted (visible) levels ---

    public int visibleAttack() {
        return attackLevel + potion.attackBoost(attackLevel);
    }

    public int visibleStrength() {
        return strengthLevel + potion.strengthBoost(strengthLevel);
    }

    public int visibleRanged() {
        return rangedLevel + potion.rangedBoost(rangedLevel);
    }

    public int visibleMagic() {
        return magicLevel + potion.magicBoost(magicLevel);
    }

    // --- Getters / setters ---

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAttackLevel() {
        return attackLevel;
    }

    public void setAttackLevel(int attackLevel) {
        this.attackLevel = attackLevel;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(int strengthLevel) {
        this.strengthLevel = strengthLevel;
    }

    public int getDefenceLevel() {
        return defenceLevel;
    }

    public void setDefenceLevel(int defenceLevel) {
        this.defenceLevel = defenceLevel;
    }

    public int getRangedLevel() {
        return rangedLevel;
    }

    public void setRangedLevel(int rangedLevel) {
        this.rangedLevel = rangedLevel;
    }

    public int getMagicLevel() {
        return magicLevel;
    }

    public void setMagicLevel(int magicLevel) {
        this.magicLevel = magicLevel;
    }

    public int getHitpointsLevel() {
        return hitpointsLevel;
    }

    public void setHitpointsLevel(int hitpointsLevel) {
        this.hitpointsLevel = hitpointsLevel;
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
        return currentHitpoints > 0 ? Math.min(currentHitpoints, hitpointsLevel) : hitpointsLevel;
    }

    public void setCurrentHitpoints(int currentHitpoints) {
        this.currentHitpoints = currentHitpoints;
    }

    public int getMiningLevel() {
        return miningLevel;
    }

    public void setMiningLevel(int miningLevel) {
        this.miningLevel = miningLevel;
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
