package com.osrs.dps.calc;

import java.util.List;

/**
 * Result of a DPS calculation for one player setup against one monster.
 *
 * @param maxHit             maximum total damage of one attack (all hitsplats, post-reductions)
 * @param accuracy           chance for an attack to land (incl. re-roll effects)
 * @param avgDamagePerAttack expected damage per attack
 * @param attackSpeedTicks   ticks between attacks
 * @param dps                expected damage per second
 * @param expectedHitsToKill expected number of attacks to kill (overkill-aware), or 0 if n/a
 * @param ttkSeconds         expected time to kill in seconds, or infinity if no damage
 * @param notes              human-readable notes about special effects that were applied
 */
public record DpsResult(
        int maxHit,
        double accuracy,
        double avgDamagePerAttack,
        int attackSpeedTicks,
        double dps,
        double expectedHitsToKill,
        double ttkSeconds,
        List<String> notes) {

    public static final double SECONDS_PER_TICK = CombatConstants.SECONDS_PER_TICK;

    public double attackIntervalSeconds() {
        return attackSpeedTicks * SECONDS_PER_TICK;
    }
}
