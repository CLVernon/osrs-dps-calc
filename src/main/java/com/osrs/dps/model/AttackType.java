package com.osrs.dps.model;

/** The damage type the player attacks with. */
public enum AttackType {
    STAB("Stab"),
    SLASH("Slash"),
    CRUSH("Crush"),
    RANGED("Ranged"),
    MAGIC("Magic");

    private final String displayName;

    AttackType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isMelee() {
        return this == STAB || this == SLASH || this == CRUSH;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
