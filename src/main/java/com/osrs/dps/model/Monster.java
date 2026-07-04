package com.osrs.dps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A monster as loaded from the wiki monster data (or user-defined). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Monster {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Skills {
        public int atk;
        public int def;
        public int hp;
        public int magic;
        public int ranged;
        public int str;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Offensive {
        public int atk;
        public int magic;
        @JsonProperty("magic_str")
        public int magicStr;
        public int ranged;
        @JsonProperty("ranged_str")
        public int rangedStr;
        public int str;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Defensive {
        /** Flat damage reduction applied to every hit (e.g. Tormented demons). */
        @JsonProperty("flat_armour")
        public int flatArmour;
        public int stab;
        public int slash;
        public int crush;
        public int magic;
        /** Ranged defence bonus vs heavy ranged weapons (crossbows, ballistae). */
        public int heavy;
        /** Ranged defence bonus vs standard ranged weapons (bows). */
        public int standard;
        /** Ranged defence bonus vs light ranged weapons (thrown, darts). */
        public int light;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weakness {
        public String element;
        public int severity;
    }

    public int id;
    public String name;
    public String version;
    public String image;
    public int level;
    public int speed;
    public List<String> style = new ArrayList<>();
    public int size = 1;
    public Skills skills = new Skills();
    public Offensive offensive = new Offensive();
    public Defensive defensive = new Defensive();
    public List<String> attributes = new ArrayList<>();
    /** Elemental weakness, or null / element "none" when the monster has none. */
    public Weakness weakness;

    public boolean hasAttribute(String attribute) {
        if (attributes == null) {
            return false;
        }
        String wanted = attribute.toLowerCase(Locale.ROOT);
        return attributes.stream()
                .filter(a -> a != null)
                .anyMatch(a -> a.toLowerCase(Locale.ROOT).startsWith(wanted));
    }

    public boolean isUndead() {
        return hasAttribute("undead");
    }

    public boolean isDemon() {
        return hasAttribute("demon");
    }

    public boolean isDragon() {
        return hasAttribute("dragon");
    }

    public boolean isKalphite() {
        return hasAttribute("kalphite");
    }

    public boolean isLeafy() {
        return hasAttribute("leafy");
    }

    /** Display name including version, e.g. "Zulrah (Magma)". */
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
