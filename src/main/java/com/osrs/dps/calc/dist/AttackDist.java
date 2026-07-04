package com.osrs.dps.calc.dist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A full attack made of one or more independent hitsplat distributions
 * (e.g. the scythe's three hits).
 */
public final class AttackDist {

    private final List<HitDist> dists;

    public AttackDist(List<HitDist> dists) {
        this.dists = dists;
    }

    public static AttackDist of(HitDist dist) {
        return new AttackDist(List.of(dist));
    }

    public List<HitDist> dists() {
        return dists;
    }

    public AttackDist transform(Function<WeightedHit, HitDist> transformer, boolean transformInaccurate) {
        return new AttackDist(dists.stream().map(d -> d.transform(transformer, transformInaccurate)).toList());
    }

    public AttackDist scaleDamage(int factor, int divisor) {
        return new AttackDist(dists.stream().map(d -> d.scaleDamage(factor, divisor)).toList());
    }

    public double expectedDamage() {
        return dists.stream().mapToDouble(HitDist::expectedDamage).sum();
    }

    public int maxDamage() {
        return dists.stream().mapToInt(HitDist::maxDamage).sum();
    }

    /** Distribution of the TOTAL damage of the attack (all hitsplats convolved). */
    public Map<Integer, Double> totalDamageHistogram() {
        Map<Integer, Double> total = new HashMap<>();
        total.put(0, 1.0);
        for (HitDist dist : dists) {
            Map<Integer, Double> hist = dist.damageHistogram();
            Map<Integer, Double> next = new HashMap<>();
            for (Map.Entry<Integer, Double> a : total.entrySet()) {
                for (Map.Entry<Integer, Double> b : hist.entrySet()) {
                    next.merge(a.getKey() + b.getKey(), a.getValue() * b.getValue(), Double::sum);
                }
            }
            total = next;
        }
        return total;
    }
}
