package com.osrs.dps.model;

/**
 * Offensive prayers with factors from the wiki DPS tool. Combined melee entries
 * represent praying the accuracy and strength prayer of the same tier together.
 * Magic prayers additionally grant a magic damage bonus (in tenths of a percent).
 */
public enum Prayer {
    NONE("None", 100, 100, 0),

    // Melee: accuracy %, strength %
    TIER_1_MELEE("Clarity of Thought + Burst of Strength (5%)", 105, 105, 0),
    TIER_2_MELEE("Improved Reflexes + Superhuman Strength (10%)", 110, 110, 0),
    TIER_3_MELEE("Incredible Reflexes + Ultimate Strength (15%)", 115, 115, 0),
    CHIVALRY("Chivalry (15% acc / 18% str)", 115, 118, 0),
    PIETY("Piety (20% acc / 23% str)", 120, 123, 0),

    // Ranged: accuracy %, ranged strength %
    SHARP_EYE("Sharp Eye (5%)", 105, 105, 0),
    HAWK_EYE("Hawk Eye (10%)", 110, 110, 0),
    EAGLE_EYE("Eagle Eye (15%)", 115, 115, 0),
    DEADEYE("Deadeye (18%)", 118, 118, 0),
    RIGOUR("Rigour (20% acc / 23% str)", 120, 123, 0),

    // Magic: accuracy %, (strength unused), magic damage bonus in tenths of a percent
    MYSTIC_WILL("Mystic Will (5%)", 105, 100, 0),
    MYSTIC_LORE("Mystic Lore (10% acc / +1% dmg)", 110, 100, 10),
    MYSTIC_MIGHT("Mystic Might (15% acc / +2% dmg)", 115, 100, 20),
    MYSTIC_VIGOUR("Mystic Vigour (18% acc / +3% dmg)", 118, 100, 30),
    AUGURY("Augury (25% acc / +4% dmg)", 125, 100, 40);

    private final String displayName;
    private final int accuracyPercent;
    private final int strengthPercent;
    private final int magicDamageTenths;

    Prayer(String displayName, int accuracyPercent, int strengthPercent, int magicDamageTenths) {
        this.displayName = displayName;
        this.accuracyPercent = accuracyPercent;
        this.strengthPercent = strengthPercent;
        this.magicDamageTenths = magicDamageTenths;
    }

    /** Applies the accuracy factor to a level, truncating (as the game does). */
    public int applyAccuracy(int level) {
        return level * accuracyPercent / 100;
    }

    /** Applies the strength/damage factor to a level, truncating. */
    public int applyStrength(int level) {
        return level * strengthPercent / 100;
    }

    /** Magic damage bonus in tenths of a percent (Augury = 40 = +4%). */
    public int magicDamageTenths() {
        return magicDamageTenths;
    }

    /** Wiki icon file name (combined melee tiers use the strength prayer's icon). */
    public String imageName() {
        return switch (this) {
            case NONE -> null;
            case TIER_1_MELEE -> "Burst of Strength.png";
            case TIER_2_MELEE -> "Superhuman Strength.png";
            case TIER_3_MELEE -> "Ultimate Strength.png";
            case CHIVALRY -> "Chivalry.png";
            case PIETY -> "Piety.png";
            case SHARP_EYE -> "Sharp Eye.png";
            case HAWK_EYE -> "Hawk Eye.png";
            case EAGLE_EYE -> "Eagle Eye.png";
            case DEADEYE -> "Deadeye.png";
            case RIGOUR -> "Rigour.png";
            case MYSTIC_WILL -> "Mystic Will.png";
            case MYSTIC_LORE -> "Mystic Lore.png";
            case MYSTIC_MIGHT -> "Mystic Might.png";
            case MYSTIC_VIGOUR -> "Mystic Vigour.png";
            case AUGURY -> "Augury.png";
        };
    }

    public static Prayer[] forAttackType(AttackType type) {
        if (type == null) {
            return values();
        }
        return switch (type) {
            case STAB, SLASH, CRUSH -> new Prayer[] {
                NONE, TIER_1_MELEE, TIER_2_MELEE, TIER_3_MELEE, CHIVALRY, PIETY};
            case RANGED -> new Prayer[] {NONE, SHARP_EYE, HAWK_EYE, EAGLE_EYE, DEADEYE, RIGOUR};
            case MAGIC -> new Prayer[] {
                NONE, MYSTIC_WILL, MYSTIC_LORE, MYSTIC_MIGHT, MYSTIC_VIGOUR, AUGURY};
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
