package com.osrs.dps.model;

/** Castable combat spells. Base max hit before magic damage bonuses. */
public enum Spell {
    WIND_STRIKE("Wind Strike", 2, "air"),
    WATER_STRIKE("Water Strike", 4, "water"),
    EARTH_STRIKE("Earth Strike", 6, "earth"),
    FIRE_STRIKE("Fire Strike", 8, "fire"),
    WIND_BOLT("Wind Bolt", 9, "air"),
    WATER_BOLT("Water Bolt", 10, "water"),
    EARTH_BOLT("Earth Bolt", 11, "earth"),
    FIRE_BOLT("Fire Bolt", 12, "fire"),
    CRUMBLE_UNDEAD("Crumble Undead", 15, null),
    WIND_BLAST("Wind Blast", 13, "air"),
    WATER_BLAST("Water Blast", 14, "water"),
    EARTH_BLAST("Earth Blast", 15, "earth"),
    FIRE_BLAST("Fire Blast", 16, "fire"),
    WIND_WAVE("Wind Wave", 17, "air"),
    WATER_WAVE("Water Wave", 18, "water"),
    EARTH_WAVE("Earth Wave", 19, "earth"),
    FIRE_WAVE("Fire Wave", 20, "fire"),
    WIND_SURGE("Wind Surge", 21, "air"),
    WATER_SURGE("Water Surge", 22, "water"),
    EARTH_SURGE("Earth Surge", 23, "earth"),
    FIRE_SURGE("Fire Surge", 24, "fire"),
    IBAN_BLAST("Iban Blast", 25, null),
    SARADOMIN_STRIKE("Saradomin Strike", 20, null),
    CLAWS_OF_GUTHIX("Claws of Guthix", 20, null),
    FLAMES_OF_ZAMORAK("Flames of Zamorak", 20, null),
    MAGIC_DART("Magic Dart", -1, null);

    private final String displayName;
    private final int baseMaxHit;
    private final String element;

    Spell(String displayName, int baseMaxHit, String element) {
        this.displayName = displayName;
        this.baseMaxHit = baseMaxHit;
        this.element = element;
    }

    /** Base max hit; visible magic level is needed for level-scaled spells like Magic Dart. */
    public int baseMaxHit(int visibleMagicLevel) {
        if (this == MAGIC_DART) {
            return 10 + visibleMagicLevel / 10;
        }
        return baseMaxHit;
    }

    /** Elemental class for monster weakness matching; null for non-elemental spells. */
    public String element() {
        return element;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
