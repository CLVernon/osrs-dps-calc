package com.osrs.dps.model;

import java.util.ArrayList;
import java.util.List;

import static com.osrs.dps.model.AttackType.CRUSH;
import static com.osrs.dps.model.AttackType.MAGIC;
import static com.osrs.dps.model.AttackType.RANGED;
import static com.osrs.dps.model.AttackType.SLASH;
import static com.osrs.dps.model.AttackType.STAB;
import static com.osrs.dps.model.Stance.ACCURATE;
import static com.osrs.dps.model.Stance.AGGRESSIVE;
import static com.osrs.dps.model.Stance.AUTOCAST;
import static com.osrs.dps.model.Stance.CONTROLLED;
import static com.osrs.dps.model.Stance.DEFENSIVE;
import static com.osrs.dps.model.Stance.DEFENSIVE_AUTOCAST;
import static com.osrs.dps.model.Stance.LONGRANGE;
import static com.osrs.dps.model.Stance.MANUAL_CAST;
import static com.osrs.dps.model.Stance.RAPID;

/**
 * The combat styles available per weapon category, as in the in-game combat
 * options tab. Ported from the wiki DPS tool's category table.
 */
public final class WeaponStyles {

    private WeaponStyles() {
    }

    /** Styles for the given weapon, or unarmed styles when no weapon is equipped. */
    public static List<CombatStyle> forWeapon(EquipmentItem weapon) {
        return forCategory(weapon == null ? "Unarmed" : weapon.category);
    }

    public static List<CombatStyle> forCategory(String category) {
        List<CombatStyle> styles = new ArrayList<>(switch (category == null ? "" : category) {
            case "2h Sword" -> List.of(
                    style("Chop", SLASH, ACCURATE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Smash", CRUSH, AGGRESSIVE),
                    style("Block", SLASH, DEFENSIVE));
            case "Banner" -> List.of(
                    style("Lunge", STAB, ACCURATE),
                    style("Swipe", SLASH, AGGRESSIVE),
                    style("Pound", CRUSH, CONTROLLED),
                    style("Block", STAB, DEFENSIVE));
            case "Bladed Staff" -> List.of(
                    style("Jab", STAB, ACCURATE),
                    style("Swipe", SLASH, AGGRESSIVE),
                    style("Fend", CRUSH, DEFENSIVE),
                    style("Spell (Defensive)", MAGIC, DEFENSIVE_AUTOCAST),
                    style("Spell", MAGIC, AUTOCAST));
            case "Bow", "Crossbow", "Thrown" -> List.of(
                    style("Accurate", RANGED, ACCURATE),
                    style("Rapid", RANGED, RAPID),
                    style("Longrange", RANGED, LONGRANGE));
            case "Gun" -> List.of(
                    style("Kick", CRUSH, AGGRESSIVE));
            case "Bulwark" -> List.of(
                    style("Pummel", CRUSH, ACCURATE));
            case "Multi-Melee" -> List.of(
                    style("Poke", STAB, ACCURATE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Pound", CRUSH, AGGRESSIVE),
                    style("Block", SLASH, DEFENSIVE));
            case "Partisan" -> List.of(
                    style("Stab", STAB, ACCURATE),
                    style("Lunge", STAB, AGGRESSIVE),
                    style("Pound", CRUSH, AGGRESSIVE),
                    style("Block", STAB, DEFENSIVE));
            case "Pickaxe" -> List.of(
                    style("Spike", STAB, ACCURATE),
                    style("Impale", STAB, AGGRESSIVE),
                    style("Smash", CRUSH, AGGRESSIVE),
                    style("Block", STAB, DEFENSIVE));
            case "Polearm" -> List.of(
                    style("Jab", STAB, CONTROLLED),
                    style("Swipe", SLASH, AGGRESSIVE),
                    style("Fend", STAB, DEFENSIVE));
            case "Powered Staff", "Powered Wand" -> List.of(
                    style("Accurate", MAGIC, ACCURATE),
                    style("Longrange", MAGIC, LONGRANGE));
            case "Salamander" -> List.of(
                    style("Scorch", SLASH, AGGRESSIVE),
                    style("Flare", RANGED, RAPID),
                    style("Blaze", MAGIC, DEFENSIVE));
            case "Chinchompas" -> List.of(
                    style("Short fuse", RANGED, ACCURATE),
                    style("Medium fuse", RANGED, RAPID),
                    style("Long fuse", RANGED, LONGRANGE));
            case "Claw" -> List.of(
                    style("Chop", SLASH, ACCURATE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Lunge", STAB, CONTROLLED),
                    style("Block", SLASH, DEFENSIVE));
            case "Bludgeon" -> List.of(
                    style("Pound", CRUSH, AGGRESSIVE),
                    style("Pummel", CRUSH, AGGRESSIVE),
                    style("Smash", CRUSH, AGGRESSIVE));
            case "Blunt" -> List.of(
                    style("Pound", CRUSH, ACCURATE),
                    style("Pummel", CRUSH, AGGRESSIVE),
                    style("Block", CRUSH, DEFENSIVE));
            case "Polestaff" -> List.of(
                    style("Bash", CRUSH, ACCURATE),
                    style("Pound", CRUSH, AGGRESSIVE),
                    style("Block", CRUSH, DEFENSIVE));
            case "Spiked" -> List.of(
                    style("Pound", CRUSH, ACCURATE),
                    style("Pummel", CRUSH, AGGRESSIVE),
                    style("Spike", STAB, CONTROLLED),
                    style("Block", CRUSH, DEFENSIVE));
            case "Staff" -> List.of(
                    style("Bash", CRUSH, ACCURATE),
                    style("Pound", CRUSH, AGGRESSIVE),
                    style("Focus", CRUSH, DEFENSIVE),
                    style("Spell (Defensive)", MAGIC, DEFENSIVE_AUTOCAST),
                    style("Spell", MAGIC, AUTOCAST));
            case "Axe" -> List.of(
                    style("Chop", SLASH, ACCURATE),
                    style("Hack", SLASH, AGGRESSIVE),
                    style("Smash", CRUSH, AGGRESSIVE),
                    style("Block", SLASH, DEFENSIVE));
            case "Scythe" -> List.of(
                    style("Reap", SLASH, ACCURATE),
                    style("Chop", SLASH, AGGRESSIVE),
                    style("Jab", CRUSH, AGGRESSIVE),
                    style("Block", SLASH, DEFENSIVE));
            case "Slash Sword" -> List.of(
                    style("Chop", SLASH, ACCURATE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Lunge", STAB, CONTROLLED),
                    style("Block", SLASH, DEFENSIVE));
            case "Spear" -> List.of(
                    style("Lunge", STAB, CONTROLLED),
                    style("Swipe", SLASH, CONTROLLED),
                    style("Pound", CRUSH, CONTROLLED),
                    style("Block", STAB, DEFENSIVE));
            case "Stab Sword" -> List.of(
                    style("Stab", STAB, ACCURATE),
                    style("Lunge", STAB, AGGRESSIVE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Block", STAB, DEFENSIVE));
            case "Whip" -> List.of(
                    style("Flick", SLASH, ACCURATE),
                    style("Lash", SLASH, CONTROLLED),
                    style("Deflect", SLASH, DEFENSIVE));
            case "Flail" -> List.of(
                    style("Chop", SLASH, ACCURATE),
                    style("Slash", SLASH, AGGRESSIVE),
                    style("Block", SLASH, DEFENSIVE));
            default -> List.of( // Unarmed and unknown categories
                    style("Punch", CRUSH, ACCURATE),
                    style("Kick", CRUSH, AGGRESSIVE),
                    style("Block", CRUSH, DEFENSIVE));
        });
        // Any weapon can manually cast a spell
        styles.add(style("Spell (Manual cast)", MAGIC, MANUAL_CAST));
        return styles;
    }

    private static CombatStyle style(String name, AttackType type, Stance stance) {
        return new CombatStyle(name, type, stance);
    }
}
