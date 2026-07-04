package com.osrs.dps.model;

/**
 * Offensive prayers. Multipliers apply to the effective level calculation.
 * Combined melee entries represent praying the accuracy and strength prayer
 * of the same tier together, as players do in practice.
 */
public enum Prayer {
    NONE("None", 1.0, 1.0, 1.0, 1.0, 1.0),

    // Melee (accuracy multiplier, strength multiplier)
    TIER_1_MELEE("Clarity of Thought + Burst of Strength (5%)", 1.05, 1.05, 1.0, 1.0, 1.0),
    TIER_2_MELEE("Improved Reflexes + Superhuman Strength (10%)", 1.10, 1.10, 1.0, 1.0, 1.0),
    TIER_3_MELEE("Incredible Reflexes + Ultimate Strength (15%)", 1.15, 1.15, 1.0, 1.0, 1.0),
    CHIVALRY("Chivalry (15% acc / 18% str)", 1.15, 1.18, 1.0, 1.0, 1.0),
    PIETY("Piety (20% acc / 23% str)", 1.20, 1.23, 1.0, 1.0, 1.0),

    // Ranged (accuracy multiplier, strength multiplier)
    SHARP_EYE("Sharp Eye (5%)", 1.0, 1.0, 1.05, 1.05, 1.0),
    HAWK_EYE("Hawk Eye (10%)", 1.0, 1.0, 1.10, 1.10, 1.0),
    EAGLE_EYE("Eagle Eye (15%)", 1.0, 1.0, 1.15, 1.15, 1.0),
    RIGOUR("Rigour (20% acc / 23% str)", 1.0, 1.0, 1.20, 1.23, 1.0),

    // Magic (accuracy multiplier only; no classic prayer boosts magic damage)
    MYSTIC_WILL("Mystic Will (5%)", 1.0, 1.0, 1.0, 1.0, 1.05),
    MYSTIC_LORE("Mystic Lore (10%)", 1.0, 1.0, 1.0, 1.0, 1.10),
    MYSTIC_MIGHT("Mystic Might (15%)", 1.0, 1.0, 1.0, 1.0, 1.15),
    AUGURY("Augury (25%)", 1.0, 1.0, 1.0, 1.0, 1.25);

    private final String displayName;
    private final double meleeAccuracy;
    private final double meleeStrength;
    private final double rangedAccuracy;
    private final double rangedStrength;
    private final double magicAccuracy;

    Prayer(String displayName, double meleeAccuracy, double meleeStrength,
           double rangedAccuracy, double rangedStrength, double magicAccuracy) {
        this.displayName = displayName;
        this.meleeAccuracy = meleeAccuracy;
        this.meleeStrength = meleeStrength;
        this.rangedAccuracy = rangedAccuracy;
        this.rangedStrength = rangedStrength;
        this.magicAccuracy = magicAccuracy;
    }

    public double meleeAccuracy() {
        return meleeAccuracy;
    }

    public double meleeStrength() {
        return meleeStrength;
    }

    public double rangedAccuracy() {
        return rangedAccuracy;
    }

    public double rangedStrength() {
        return rangedStrength;
    }

    public double magicAccuracy() {
        return magicAccuracy;
    }

    public static Prayer[] forAttackType(AttackType type) {
        if (type == null) {
            return values();
        }
        return switch (type) {
            case STAB, SLASH, CRUSH -> new Prayer[] {
                NONE, TIER_1_MELEE, TIER_2_MELEE, TIER_3_MELEE, CHIVALRY, PIETY};
            case RANGED -> new Prayer[] {NONE, SHARP_EYE, HAWK_EYE, EAGLE_EYE, RIGOUR};
            case MAGIC -> new Prayer[] {NONE, MYSTIC_WILL, MYSTIC_LORE, MYSTIC_MIGHT, AUGURY};
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
