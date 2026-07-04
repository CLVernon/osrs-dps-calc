package com.osrs.dps.model;

/** Stat-boosting potions/consumables. Boost = flat + percentage of the base level, floored. */
public enum Potion {
    NONE("None"),
    ATTACK_POTION("Attack potion (+3 +10%)"),
    STRENGTH_POTION("Strength potion (+3 +10%)"),
    COMBAT_POTION("Combat potion (+3 +10% Atk/Str)"),
    SUPER_ATTACK("Super attack (+5 +15%)"),
    SUPER_STRENGTH("Super strength (+5 +15%)"),
    SUPER_COMBAT("Super combat (+5 +15% Atk/Str)"),
    RANGING_POTION("Ranging potion (+4 +10%)"),
    BASTION_POTION("Bastion potion (+4 +10% Ranged)"),
    MAGIC_POTION("Magic potion (+4)"),
    IMBUED_HEART("Imbued heart (+1 +10% Magic)"),
    SATURATED_HEART("Saturated heart (+4 +10% Magic)"),
    SMELLING_SALTS("Smelling salts (+11 +16% all)"),
    OVERLOAD_RAID("Overload (raids, +6 +16% all)");

    private final String displayName;

    Potion(String displayName) {
        this.displayName = displayName;
    }

    public int attackBoost(int level) {
        return switch (this) {
            case ATTACK_POTION, COMBAT_POTION -> 3 + level / 10;
            case SUPER_ATTACK, SUPER_COMBAT -> 5 + (int) Math.floor(level * 0.15);
            case SMELLING_SALTS -> 11 + (int) Math.floor(level * 0.16);
            case OVERLOAD_RAID -> 6 + (int) Math.floor(level * 0.16);
            default -> 0;
        };
    }

    public int strengthBoost(int level) {
        return switch (this) {
            case STRENGTH_POTION, COMBAT_POTION -> 3 + level / 10;
            case SUPER_STRENGTH, SUPER_COMBAT -> 5 + (int) Math.floor(level * 0.15);
            case SMELLING_SALTS -> 11 + (int) Math.floor(level * 0.16);
            case OVERLOAD_RAID -> 6 + (int) Math.floor(level * 0.16);
            default -> 0;
        };
    }

    public int rangedBoost(int level) {
        return switch (this) {
            case RANGING_POTION, BASTION_POTION -> 4 + level / 10;
            case SMELLING_SALTS -> 11 + (int) Math.floor(level * 0.16);
            case OVERLOAD_RAID -> 6 + (int) Math.floor(level * 0.16);
            default -> 0;
        };
    }

    public int magicBoost(int level) {
        return switch (this) {
            case MAGIC_POTION -> 4;
            case IMBUED_HEART -> 1 + level / 10;
            case SATURATED_HEART -> 4 + level / 10;
            case SMELLING_SALTS -> 11 + (int) Math.floor(level * 0.16);
            case OVERLOAD_RAID -> 6 + (int) Math.floor(level * 0.16);
            default -> 0;
        };
    }

    public static Potion[] forAttackType(AttackType type) {
        if (type == null) {
            return values();
        }
        return switch (type) {
            case STAB, SLASH, CRUSH -> new Potion[] {
                NONE, ATTACK_POTION, STRENGTH_POTION, COMBAT_POTION, SUPER_ATTACK,
                SUPER_STRENGTH, SUPER_COMBAT, SMELLING_SALTS, OVERLOAD_RAID};
            case RANGED -> new Potion[] {
                NONE, RANGING_POTION, BASTION_POTION, SMELLING_SALTS, OVERLOAD_RAID};
            case MAGIC -> new Potion[] {
                NONE, MAGIC_POTION, IMBUED_HEART, SATURATED_HEART, SMELLING_SALTS, OVERLOAD_RAID};
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
