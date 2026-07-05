package com.osrs.dps.model;

/** One selectable combat style of a weapon, e.g. Lash (slash, Controlled). */
public record CombatStyle(String name, AttackType type, Stance stance) {

    public String displayName() {
        String typeName = switch (type) {
            case STAB -> "Stab";
            case SLASH -> "Slash";
            case CRUSH -> "Crush";
            case RANGED -> "Ranged";
            case MAGIC -> "Magic";
        };
        return name + "  (" + stance + " · " + typeName + ")";
    }

    @Override
    public String toString() {
        return displayName();
    }
}
