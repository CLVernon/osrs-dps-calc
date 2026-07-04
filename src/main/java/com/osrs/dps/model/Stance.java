package com.osrs.dps.model;

/**
 * Attack stances and their invisible level bonuses per the OSRS wiki.
 * Melee: Accurate +3 Attack, Aggressive +3 Strength, Controlled +1 to both.
 * Ranged: Accurate +3 Ranged, Rapid -1 tick attack speed.
 * Magic: Accurate +2 Magic (powered staves / trained style).
 */
public enum Stance {
    ACCURATE("Accurate"),
    AGGRESSIVE("Aggressive"),
    CONTROLLED("Controlled"),
    DEFENSIVE("Defensive"),
    RAPID("Rapid"),
    LONGRANGE("Longrange"),
    AUTOCAST("Autocast"),
    DEFENSIVE_AUTOCAST("Defensive autocast");

    private final String displayName;

    Stance(String displayName) {
        this.displayName = displayName;
    }

    public int meleeAttackBonus() {
        return switch (this) {
            case ACCURATE -> 3;
            case CONTROLLED -> 1;
            default -> 0;
        };
    }

    public int meleeStrengthBonus() {
        return switch (this) {
            case AGGRESSIVE -> 3;
            case CONTROLLED -> 1;
            default -> 0;
        };
    }

    public int rangedAttackBonus() {
        return this == ACCURATE ? 3 : 0;
    }

    public int rangedStrengthBonus() {
        return this == ACCURATE ? 3 : 0;
    }

    public int magicAttackBonus() {
        return this == ACCURATE ? 2 : 0;
    }

    /** Attack speed adjustment in ticks (Rapid shoots one tick faster). */
    public int speedAdjustment() {
        return this == RAPID ? -1 : 0;
    }

    public static Stance[] meleeStances() {
        return new Stance[] {ACCURATE, AGGRESSIVE, CONTROLLED, DEFENSIVE};
    }

    public static Stance[] rangedStances() {
        return new Stance[] {ACCURATE, RAPID, LONGRANGE};
    }

    public static Stance[] magicStances() {
        return new Stance[] {ACCURATE, LONGRANGE, AUTOCAST, DEFENSIVE_AUTOCAST};
    }

    @Override
    public String toString() {
        return displayName;
    }
}
