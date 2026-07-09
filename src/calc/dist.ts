// Hit distribution primitives, mirroring the wiki DPS tool

export interface WeightedHit {
  probability: number;
  damage: number;
  accurate: boolean;
}

export type HitTransformer = (hit: WeightedHit) => WeightedHit[];

export class HitDist {
  constructor(readonly hits: WeightedHit[]) {}

  static linear(accuracy: number, min: number, max: number): HitDist {
    const hits: WeightedHit[] = [];
    const hitProb = accuracy / (max - min + 1);
    for (let dmg = min; dmg <= max; dmg++) {
      hits.push({ probability: hitProb, damage: dmg, accurate: true });
    }
    if (accuracy < 1.0) {
      hits.push({ probability: 1 - accuracy, damage: 0, accurate: false });
    }
    return new HitDist(hits);
  }

  static single(accuracy: number, damage: number): HitDist {
    const hits: WeightedHit[] = [{ probability: accuracy, damage, accurate: true }];
    if (accuracy < 1.0) {
      hits.push({ probability: 1 - accuracy, damage: 0, accurate: false });
    }
    return new HitDist(hits);
  }

  transform(transformer: HitTransformer, transformInaccurate: boolean): HitDist {
    const out: WeightedHit[] = [];
    for (const hit of this.hits) {
      if (!hit.accurate && !transformInaccurate) {
        out.push(hit);
        continue;
      }
      for (const t of transformer({ ...hit, probability: 1.0 })) {
        out.push({ ...t, probability: t.probability * hit.probability });
      }
    }
    return new HitDist(out).flatten();
  }

  scaleProbability(factor: number): HitDist {
    return new HitDist(this.hits.map((h) => ({ ...h, probability: h.probability * factor })));
  }

  scaleDamage(factor: number, divisor = 1): HitDist {
    return new HitDist(this.hits.map((h) => ({
      ...h, damage: Math.trunc((h.damage * factor) / divisor),
    })));
  }

  flatten(): HitDist {
    const merged = new Map<number, WeightedHit>();
    for (const hit of this.hits) {
      const key = hit.damage * 2 + (hit.accurate ? 1 : 0);
      const existing = merged.get(key);
      if (existing) {
        existing.probability += hit.probability;
      } else {
        merged.set(key, { ...hit });
      }
    }
    return new HitDist([...merged.values()]);
  }

  expectedDamage(): number {
    return this.hits.reduce((sum, h) => sum + h.probability * h.damage, 0);
  }

  maxDamage(): number {
    return this.hits.reduce((max, h) => Math.max(max, h.damage), 0);
  }

  damageHistogram(): Map<number, number> {
    const hist = new Map<number, number>();
    for (const h of this.hits) {
      hist.set(h.damage, (hist.get(h.damage) ?? 0) + h.probability);
    }
    return hist;
  }
}

/** A full attack of one or more independent hitsplats (e.g. scythe hits). */
export class AttackDist {
  constructor(readonly dists: HitDist[]) {}

  static of(dist: HitDist): AttackDist {
    return new AttackDist([dist]);
  }

  transform(transformer: HitTransformer, transformInaccurate: boolean): AttackDist {
    return new AttackDist(this.dists.map((d) => d.transform(transformer, transformInaccurate)));
  }

  scaleDamage(factor: number, divisor = 1): AttackDist {
    return new AttackDist(this.dists.map((d) => d.scaleDamage(factor, divisor)));
  }

  expectedDamage(): number {
    return this.dists.reduce((sum, d) => sum + d.expectedDamage(), 0);
  }

  maxDamage(): number {
    return this.dists.reduce((sum, d) => sum + d.maxDamage(), 0);
  }

  /** Distribution of the TOTAL damage of the attack (all hitsplats convolved). */
  totalDamageHistogram(): Map<number, number> {
    let total = new Map<number, number>([[0, 1.0]]);
    for (const dist of this.dists) {
      const hist = dist.damageHistogram();
      const next = new Map<number, number>();
      for (const [a, pa] of total) {
        for (const [b, pb] of hist) {
          const key = a + b;
          next.set(key, (next.get(key) ?? 0) + pa * pb);
        }
      }
      total = next;
    }
    return total;
  }
}

// --- Standard transformers ---

export const addTransformer = (addend: number, minimum = 0): HitTransformer =>
  (h) => [{ probability: 1.0, damage: Math.max(minimum, h.damage + addend), accurate: h.accurate }];

export const flatLimitTransformer = (maximum: number, minimum = 0): HitTransformer =>
  (h) => [{
    probability: 1.0,
    damage: Math.max(minimum, Math.min(h.damage, maximum)),
    accurate: h.accurate,
  }];

export const linearMinTransformer = (maximum: number, offset = 0): HitTransformer =>
  (h) => {
    const out: WeightedHit[] = [];
    const prob = 1.0 / (maximum + 1);
    for (let i = 0; i <= maximum; i++) {
      out.push({ probability: prob, damage: Math.min(h.damage, i + offset), accurate: h.accurate });
    }
    return out;
  };

export const cappedRerollTransformer = (limit: number, rollMax: number, offset = 0): HitTransformer =>
  (h) => {
    if (h.damage <= limit) return [{ ...h, probability: 1.0 }];
    const out: WeightedHit[] = [];
    const prob = 1.0 / (rollMax + 1);
    for (let i = 0; i <= rollMax; i++) {
      out.push({ probability: prob, damage: i + offset, accurate: h.accurate });
    }
    return out;
  };

export const multiplyTransformer = (numerator: number, divisor = 1, minimum = 0): HitTransformer =>
  (h) => {
    let dmg = Math.trunc((numerator * h.damage) / divisor);
    if (minimum !== 0) {
      dmg = h.damage >= minimum ? Math.max(minimum, dmg) : Math.max(h.damage, dmg);
    }
    return [{ probability: 1.0, damage: dmg, accurate: h.accurate }];
  };

export const divisionTransformer = (divisor: number, minimum = 0): HitTransformer =>
  multiplyTransformer(1, divisor, minimum);
