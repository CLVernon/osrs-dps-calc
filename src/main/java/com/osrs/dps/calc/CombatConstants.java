package com.osrs.dps.calc;

import java.util.Set;

/** Monster ID groups with special mechanics, ported from the wiki DPS tool. */
public final class CombatConstants {

    private CombatConstants() {
    }

    public static final double SECONDS_PER_TICK = 0.6;
    public static final int DEFAULT_ATTACK_SPEED = 4;

    public static final Set<Integer> P2_WARDEN_IDS = Set.of(11753, 11754, 11756, 11757);
    private static final Set<Integer> TOA_PATH_IDS = Set.of(
            11778, 11779, 11780, 11781, // akkha + shadow
            11782, 11783, 11784, 11785, 11786, 11787, 11788, 11789, // baboons
            11719, 11721, // kephri
            11724, 11725, 11726, // kephri overlords
            11730, 11732, 11733, // zebak
            11751, 11750, 11752, // obelisk
            11755, 11758, // warden cores
            11761, 11763, 11762, 11764); // p3 wardens

    public static final Set<Integer> KEPHRI_OVERLORD_IDS = Set.of(11724, 11725, 11726);

    /** All Tombs of Amascut monsters (invocation scaling, shadow 4x, fang reroll). */
    public static final Set<Integer> TOA_MONSTER_IDS;

    static {
        java.util.HashSet<Integer> toa = new java.util.HashSet<>(TOA_PATH_IDS);
        toa.addAll(P2_WARDEN_IDS);
        TOA_MONSTER_IDS = Set.copyOf(toa);
    }

    public static final Set<Integer> VERZIK_P1_IDS = Set.of(
            10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849);

    public static final Set<Integer> VERZIK_IDS;

    static {
        java.util.HashSet<Integer> v = new java.util.HashSet<>(VERZIK_P1_IDS);
        v.addAll(Set.of(10833, 10834, 10835, 8372, 8373, 8374, 10850, 10851, 10852));
        VERZIK_IDS = Set.copyOf(v);
    }

    public static final Set<Integer> TEKTON_IDS = Set.of(7540, 7543, 7544, 7545);
    public static final Set<Integer> SCAVENGER_BEAST_IDS = Set.of(7548, 7549);
    public static final Set<Integer> VESPINE_SOLDIER_IDS = Set.of(7538, 7539);
    public static final Set<Integer> DEATHLY_RANGER_IDS = Set.of(7559);
    public static final Set<Integer> GUARDIAN_IDS = Set.of(7569, 7571, 7570, 7572);
    public static final Set<Integer> OLM_HEAD_IDS = Set.of(7551, 7554);
    public static final Set<Integer> OLM_MELEE_HAND_IDS = Set.of(7552, 7555);
    public static final Set<Integer> OLM_MAGE_HAND_IDS = Set.of(7550, 7553);
    public static final Set<Integer> GLOWING_CRYSTAL_IDS = Set.of(7568);
    public static final Set<Integer> ICE_DEMON_IDS = Set.of(7584, 7585);
    public static final Set<Integer> VESPULA_IDS = Set.of(7530, 7531, 7532);
    public static final Set<Integer> ABYSSAL_PORTAL_IDS = Set.of(7533);
    public static final Set<Integer> FRAGMENT_OF_SEREN_IDS = Set.of(8917, 8918, 8919, 8920);
    public static final Set<Integer> ZULRAH_IDS = Set.of(2042, 2043, 2044);
    public static final Set<Integer> NIGHTMARE_TOTEM_IDS = Set.of(
            9434, 9437, 9440, 9443, 9435, 9438, 9441, 9444);

    /** NPCs whose magic defence uses the Defence level rather than the Magic level. */
    public static final Set<Integer> USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_IDS;

    static {
        java.util.HashSet<Integer> s = new java.util.HashSet<>();
        s.addAll(ICE_DEMON_IDS);
        s.addAll(VERZIK_IDS);
        s.addAll(FRAGMENT_OF_SEREN_IDS);
        s.addAll(Set.of(11709, 11712, 9118));
        USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_IDS = Set.copyOf(s);
    }

    private static final Set<Integer> DUSK_IDS = Set.of(
            7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889);

    public static final Set<Integer> IMMUNE_TO_MELEE_IDS;

    static {
        java.util.HashSet<Integer> s = new java.util.HashSet<>(ZULRAH_IDS);
        s.addAll(ABYSSAL_PORTAL_IDS);
        s.addAll(Set.of(494, 7706, 7708, 12214, 12215, 12219));
        IMMUNE_TO_MELEE_IDS = Set.copyOf(s);
    }

    public static final Set<Integer> IMMUNE_TO_RANGED_IDS;

    static {
        java.util.HashSet<Integer> s = new java.util.HashSet<>(TEKTON_IDS);
        s.addAll(DUSK_IDS);
        s.addAll(GLOWING_CRYSTAL_IDS);
        IMMUNE_TO_RANGED_IDS = Set.copyOf(s);
    }

    /** Aviansies etc. — immune to melee except salamanders. */
    public static final Set<Integer> IMMUNE_TO_NON_SALAMANDER_MELEE_IDS = Set.of(
            3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180,
            3181, 3182, 3183, 7037);

    public static final Set<Integer> IMMUNE_TO_MAGIC_IDS = Set.of(); // none currently bundled

    public static final Set<Integer> ONE_HIT_MONSTERS = Set.of(7223, 8584, 11193);
    public static final Set<Integer> GUARANTEED_ACCURACY_MONSTERS = Set.of(5916);
    public static final Set<Integer> INFINITE_HEALTH_MONSTERS = Set.of(14779);

    /** CoX monsters whose Magic level counts as defensive rather than offensive for scaling. */
    public static final Set<Integer> COX_MAGIC_IS_DEFENSIVE_IDS;

    /** CoX monsters that use solo-style scaling even in groups. */
    public static final Set<Integer> COX_SINGLES_SCALING_IDS;

    public static final Set<Integer> OLM_IDS;

    static {
        java.util.HashSet<Integer> s = new java.util.HashSet<>();
        s.addAll(DEATHLY_RANGER_IDS);
        s.addAll(TEKTON_IDS);
        s.addAll(ABYSSAL_PORTAL_IDS);
        s.addAll(VESPULA_IDS);
        s.addAll(VESPINE_SOLDIER_IDS);
        s.addAll(OLM_MELEE_HAND_IDS);
        s.addAll(OLM_MAGE_HAND_IDS);
        COX_MAGIC_IS_DEFENSIVE_IDS = Set.copyOf(s);

        java.util.HashSet<Integer> singles = new java.util.HashSet<>(SCAVENGER_BEAST_IDS);
        singles.addAll(VESPINE_SOLDIER_IDS);
        COX_SINGLES_SCALING_IDS = Set.copyOf(singles);

        java.util.HashSet<Integer> olm = new java.util.HashSet<>(OLM_HEAD_IDS);
        olm.addAll(OLM_MELEE_HAND_IDS);
        olm.addAll(OLM_MAGE_HAND_IDS);
        OLM_IDS = Set.copyOf(olm);
    }

    public static final Set<Integer> ALWAYS_MAX_HIT_MELEE = Set.of(11710, 11713, 12814, 11755, 11758);
    public static final Set<Integer> ALWAYS_MAX_HIT_RANGED = Set.of(11711, 11714, 12815, 11717, 11715);
    public static final Set<Integer> ALWAYS_MAX_HIT_MAGIC = Set.of(11709, 11712, 12816, 14151, 14150);
}
