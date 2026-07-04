package com.osrs.dps.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The player character: name and stats, shared by all gear setups.
 * Levels can be imported from the official hiscores or edited manually.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerCharacter {

    public String name = "";
    public int attack = 99;
    public int strength = 99;
    public int defence = 99;
    public int ranged = 99;
    public int magic = 99;
    public int hitpoints = 99;
    public int mining = 99;
    /** Current HP, for Dharok's set effect; 0 means "at full health". */
    public int currentHitpoints;

    /** Current HP clamped to the hitpoints level; full health when unset. */
    public int effectiveCurrentHitpoints() {
        return currentHitpoints > 0 ? Math.min(currentHitpoints, hitpoints) : hitpoints;
    }
}
