package com.osrs.dps.calc.dist;

/**
 * A single hitsplat outcome with its probability. Inaccurate hits deal 0 damage
 * but are distinguished from accurate zeros for effects that only apply on hit.
 */
public record WeightedHit(double probability, int damage, boolean accurate) {

    public static WeightedHit inaccurate(double probability) {
        return new WeightedHit(probability, 0, false);
    }

    public WeightedHit withProbability(double p) {
        return new WeightedHit(p, damage, accurate);
    }

    public WeightedHit withDamage(int d) {
        return new WeightedHit(probability, d, accurate);
    }
}
