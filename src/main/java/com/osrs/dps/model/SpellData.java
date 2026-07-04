package com.osrs.dps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Locale;

/** A combat spell as loaded from the wiki spell data. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpellData {

    public String name;
    public String image;
    @JsonProperty("max_hit")
    public int maxHit;
    public String spellbook;
    /** air / water / earth / fire, or null for non-elemental spells. */
    public String element;

    /**
     * Effective element for weakness/tome purposes: ancient spells map
     * Smoke=air, Ice=water, Shadow=earth, Blood=fire.
     */
    public String effectiveElement() {
        if (name != null) {
            if (name.contains("Smoke")) {
                return "air";
            }
            if (name.contains("Ice")) {
                return "water";
            }
            if (name.contains("Shadow")) {
                return "earth";
            }
            if (name.contains("Blood")) {
                return "fire";
            }
        }
        return element;
    }

    public boolean isDemonbane() {
        return name != null && name.contains("Demonbane");
    }

    public boolean isFireSpell() {
        return "fire".equals(effectiveElement());
    }

    public boolean isBindSpell() {
        return name != null && List.of("Bind", "Snare", "Entangle").contains(name);
    }

    /**
     * Max hit at the given visible magic level. Standard elemental spells scale:
     * the highest tier of the same spell class castable at the level applies.
     */
    public int maxHitAtLevel(int magicLevel, List<SpellData> allSpells) {
        if (element == null || name == null || !name.contains(" ")) {
            return maxHit;
        }
        String spellClass = name.split(" ")[1];
        int[] thresholds = switch (spellClass) {
            case "Strike" -> new int[] {13, 9, 5};
            case "Bolt" -> new int[] {35, 29, 23};
            case "Blast" -> new int[] {59, 53, 47};
            case "Wave" -> new int[] {75, 70, 65};
            case "Surge" -> new int[] {95, 90, 85};
            default -> null;
        };
        if (thresholds == null) {
            return maxHit;
        }
        String[] elements = {"Fire", "Earth", "Water"};
        for (int i = 0; i < 3; i++) {
            if (magicLevel >= thresholds[i]) {
                return findMaxHit(elements[i] + " " + spellClass, allSpells);
            }
        }
        return findMaxHit("Wind " + spellClass, allSpells);
    }

    private int findMaxHit(String spellName, List<SpellData> allSpells) {
        return allSpells.stream()
                .filter(s -> spellName.equals(s.name))
                .mapToInt(s -> s.maxHit)
                .findFirst()
                .orElse(maxHit);
    }

    public String displayName() {
        return name + " (" + capitalize(spellbook) + ")";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return displayName();
    }
}
