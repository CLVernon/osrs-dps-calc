package com.osrs.dps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A single piece of equipment as loaded from the wiki equipment data. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquipmentItem {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bonuses {
        public int str;
        @JsonProperty("ranged_str")
        public int rangedStr;
        /** Magic damage bonus in tenths of a percent (50 = +5.0%). */
        @JsonProperty("magic_str")
        public int magicStr;
        public int prayer;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offensive {
        public int stab;
        public int slash;
        public int crush;
        public int magic;
        public int ranged;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Defensive {
        public int stab;
        public int slash;
        public int crush;
        public int magic;
        public int ranged;
    }

    public String name;
    public int id;
    public String version;
    public String slot;
    public String image;
    /** Attack speed in game ticks; only meaningful for weapons. */
    public int speed;
    public String category;
    public Bonuses bonuses = new Bonuses();
    public Offensive offensive = new Offensive();
    public Defensive defensive = new Defensive();
    @JsonProperty("isTwoHanded")
    public boolean twoHanded;

    public EquipmentSlot equipmentSlot() {
        return EquipmentSlot.fromJsonName(slot);
    }

    /** Display name including version, e.g. "Scythe of vitur (Charged)". */
    public String displayName() {
        if (version == null || version.isBlank()) {
            return name;
        }
        return name + " (" + version + ")";
    }

    @Override
    public String toString() {
        return displayName();
    }
}
