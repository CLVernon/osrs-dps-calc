package com.osrs.dps.calc;

import java.util.List;

/**
 * Result of a DPS calculation for one player setup against one monster.
 *
 * @param maxHit             the maximum single hit (for multi-hit weapons like the scythe,
 *                           the max of the largest hit; see notes)
 * @param accuracy           chance for an attack roll to succeed (0..1)
 * @param avgDamagePerAttack expected damage per attack including accuracy, multi-hits,
 *                           flat armour and weapon quirks
 * @param attackSpeedTicks   ticks between attacks
 * @param dps                expected damage per second
 * @param notes              human-readable notes about special effects that were applied
 */
public record DpsResult(
        int maxHit,
        double accuracy,
        double avgDamagePerAttack,
        int attackSpeedTicks,
        double dps,
        List<String> notes) {

    public static final double SECONDS_PER_TICK = 0.6;

    public double attackIntervalSeconds() {
        return attackSpeedTicks * SECONDS_PER_TICK;
    }

    /** Expected seconds to deal the given amount of damage (simple hp / dps estimate). */
    public double expectedTimeToKill(int hitpoints) {
        if (dps <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return hitpoints / dps;
    }
}
