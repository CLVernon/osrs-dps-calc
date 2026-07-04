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

    /** Wiki icon file name. */
    public String imageName() {
        return switch (this) {
            case STAB -> "White dagger.png";
            case SLASH -> "White scimitar.png";
            case CRUSH -> "White warhammer.png";
            case RANGED -> "Ranged icon.png";
            case MAGIC -> "Magic icon.png";
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
