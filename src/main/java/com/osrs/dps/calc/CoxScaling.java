package com.osrs.dps.calc;

import com.osrs.dps.model.Monster;

/**
 * Chambers of Xeric monster scaling by party size, party stats and Challenge
 * Mode, ported from the wiki DPS tool. Applies to Xerician monsters only.
 */
public final class CoxScaling {

    private static final int CM_SCALE_PERCENT = 50;

    private CoxScaling() {
    }

    /** Returns a scaled copy for Xerician monsters, or the monster unchanged. */
    public static Monster scale(Monster m) {
        if (!m.hasAttribute("xerician")) {
            return m;
        }
        if (CombatConstants.COX_SINGLES_SCALING_IDS.contains(m.id)) {
            return applySinglesScaling(m.copyWithSkills());
        }
        if (CombatConstants.OLM_IDS.contains(m.id)) {
            return applyOlmScaling(m.copyWithSkills());
        }
        return applyMultiScaling(m.copyWithSkills());
    }

    private static int addPercent(int value, int percent) {
        return value + value * percent / 100;
    }

    private static int iSqrt(int value) {
        return (int) Math.sqrt(value);
    }

    private static int partySumMiningLevel(Monster m) {
        return m.partyAvgMiningLevel * Math.max(1, m.partySize);
    }

    private record SkillMeta(boolean scaleAtk, boolean scaleStr, boolean scaleRanged,
                             boolean scaleMagic, boolean magicIsDefensive, boolean scaleDef,
                             int baseOffensive, int baseDefensive, int baseHp) {
    }

    private static SkillMeta skillMeta(Monster m) {
        boolean magicIsDefensive = CombatConstants.COX_MAGIC_IS_DEFENSIVE_IDS.contains(m.id);

        // skills fixed at 1 stay at 1
        boolean scaleAtk = m.skills.atk != 1;
        boolean scaleStr = m.skills.str != 1;
        boolean scaleRanged = m.skills.ranged != 1;
        boolean scaleMagic = m.skills.magic != 1;
        boolean scaleDef = m.skills.def != 1;

        int baseOffensive = 1;
        if (scaleAtk) {
            baseOffensive = Math.max(baseOffensive, m.skills.atk);
        }
        if (scaleStr) {
            baseOffensive = Math.max(baseOffensive, m.skills.str);
        }
        if (scaleRanged) {
            baseOffensive = Math.max(baseOffensive, m.skills.ranged);
        }
        if (scaleMagic && !magicIsDefensive) {
            baseOffensive = Math.max(baseOffensive, m.skills.magic);
        }

        int baseDefensive = 1;
        if (scaleDef) {
            baseDefensive = Math.max(baseDefensive, m.skills.def);
        }
        if (scaleMagic && magicIsDefensive) {
            baseDefensive = Math.max(baseDefensive, m.skills.magic);
        }

        int baseHp = CombatConstants.GUARDIAN_IDS.contains(m.id)
                ? 151 + partySumMiningLevel(m) / Math.max(1, m.partySize)
                : m.skills.hp;

        return new SkillMeta(scaleAtk, scaleStr, scaleRanged, scaleMagic, magicIsDefensive,
                scaleDef, baseOffensive, baseDefensive, baseHp);
    }

    private static void applyStats(Monster m, SkillMeta meta, int hp, int offensive, int defensive) {
        m.skills.hp = hp;
        if (meta.scaleAtk()) {
            m.skills.atk = offensive;
        }
        if (meta.scaleStr()) {
            m.skills.str = offensive;
        }
        if (meta.scaleRanged()) {
            m.skills.ranged = offensive;
        }
        if (meta.scaleMagic()) {
            m.skills.magic = meta.magicIsDefensive() ? defensive : offensive;
        }
        if (meta.scaleDef()) {
            m.skills.def = defensive;
        }
    }

    /** Solo-style scaling (scavenger beasts, vespine soldiers). */
    private static Monster applySinglesScaling(Monster m) {
        SkillMeta meta = skillMeta(m);

        int hpScaler = Math.max(Math.min(m.partyMaxCombatLevel, 126), 60);
        int statScaler = Math.max(Math.min(m.partyMaxHpLevel, 99), 55);
        if (m.coxChallengeMode) {
            statScaler = addPercent(statScaler, CM_SCALE_PERCENT);
            hpScaler = addPercent(hpScaler, CM_SCALE_PERCENT);
        }

        int hp = Math.max(meta.baseHp() * hpScaler / 126, 5);
        int offensive = Math.max(meta.baseOffensive() * statScaler / 99, 1);
        int defensive = Math.max(meta.baseDefensive() * statScaler / 99, 1);
        applyStats(m, meta, hp, offensive, defensive);
        return m;
    }

    /** Group scaling (most CoX monsters). */
    private static Monster applyMultiScaling(Monster m) {
        SkillMeta meta = skillMeta(m);

        int partySize = Math.min(Math.max(m.partySize, 1), 100);
        int partySizeM1 = partySize - 1;
        int highestCombatLevel = Math.max(Math.min(m.partyMaxCombatLevel, 126), 60);
        int highestHp = Math.max(Math.min(55 + 44 * m.partyMaxHpLevel / 99, 99), 55);

        int offensive = meta.baseOffensive() * highestHp / 99;
        int defensive = meta.baseDefensive() * highestHp / 99;
        int hp = meta.baseHp() * highestCombatLevel / 126;

        int offensiveScalePct = 100 + iSqrt(partySizeM1) * 7 + partySizeM1;
        offensive = offensive * offensiveScalePct / 100;

        int defensiveScalePct = 100 + iSqrt(partySizeM1) + partySizeM1 * 7 / 10;
        defensive = defensive * defensiveScalePct / 100;

        hp += hp * (partySize * 50 / 100);

        if (m.coxChallengeMode) {
            offensive = addPercent(offensive, CM_SCALE_PERCENT);
            boolean glowingCrystal = CombatConstants.GLOWING_CRYSTAL_IDS.contains(m.id);
            if (!glowingCrystal) {
                hp = addPercent(hp, CM_SCALE_PERCENT);
            }
            if (glowingCrystal) {
                // defence not scaled
            } else if (CombatConstants.TEKTON_IDS.contains(m.id)) {
                defensive = addPercent(defensive, partySize < 4 ? 20 : 35);
            } else {
                defensive = addPercent(defensive, CM_SCALE_PERCENT);
            }
        }

        hp = Math.max(Math.min(hp, 30_000), 50);
        offensive = Math.max(Math.min(offensive, 5_000), 50);
        defensive = Math.max(Math.min(defensive, 20_000), 50);
        applyStats(m, meta, hp, offensive, defensive);
        return m;
    }

    /** Olm head and hands: multi scaling plus phase-based HP and halved mage-hand magic. */
    private static Monster applyOlmScaling(Monster m) {
        boolean meleeHand = CombatConstants.OLM_MELEE_HAND_IDS.contains(m.id);
        boolean mageHand = CombatConstants.OLM_MAGE_HAND_IDS.contains(m.id);

        int partySizeScaleFactor = Math.min(m.partySize - 1, 50)
                - 3 * (Math.min(m.partySize, 50) / 8);

        Monster scaled = applyMultiScaling(m);
        if (mageHand) {
            scaled.skills.magic = scaled.skills.magic / 2;
        }
        scaled.skills.hp = (meleeHand || mageHand)
                ? 600 + 300 * partySizeScaleFactor
                : 800 + 400 * partySizeScaleFactor;
        return scaled;
    }
}
