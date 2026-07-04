package com.osrs.dps.calc;

/** Shared low-level combat formulas from the OSRS wiki. */
final class CombatMath {

    private CombatMath() {
    }

    /** Standard hit-chance formula from attack and defence rolls. */
    static double accuracy(long attackRoll, long defenceRoll) {
        if (attackRoll > defenceRoll) {
            return 1.0 - (defenceRoll + 2.0) / (2.0 * (attackRoll + 1.0));
        }
        return attackRoll / (2.0 * (defenceRoll + 1.0));
    }

    /** Base max hit from an effective strength level and equipment strength bonus. */
    static int maxHit(int effectiveLevel, int strengthBonus) {
        return (effectiveLevel * (strengthBonus + 64) + 320) / 640;
    }

    /**
     * Expected damage of a single successful hit rolled uniformly in [minHit, maxHit],
     * with a flat armour reduction applied to each hit (reduced hits floor at 0).
     */
    static double expectedHitDamage(int minHit, int maxHit, int flatArmour) {
        if (maxHit < minHit) {
            return 0;
        }
        int outcomes = maxHit - minHit + 1;
        if (flatArmour <= 0) {
            return (minHit + maxHit) / 2.0;
        }
        long total = 0;
        for (int h = minHit; h <= maxHit; h++) {
            total += Math.max(h - flatArmour, 0);
        }
        return (double) total / outcomes;
    }
}
