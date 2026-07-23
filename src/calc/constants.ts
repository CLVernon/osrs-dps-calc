// Monster ID groups with special mechanics, from the wiki DPS tool

export const SECONDS_PER_TICK = 0.6;

export const P2_WARDEN_IDS = new Set([11753, 11754, 11756, 11757]);
export const KEPHRI_OVERLORD_IDS = new Set([11724, 11725, 11726]);

export const TOA_MONSTER_IDS = new Set([
  11778, 11779, 11780, 11781, // akkha + shadow
  11782, 11783, 11784, 11785, 11786, 11787, 11788, 11789, // baboons
  11719, 11721, // kephri
  11724, 11725, 11726, // kephri overlords
  11730, 11732, 11733, // zebak
  11751, 11750, 11752, // obelisk
  11755, 11758, // warden cores
  11761, 11763, 11762, 11764, // p3 wardens
  ...P2_WARDEN_IDS,
]);

export const VERZIK_P1_IDS = new Set([
  10830, 10831, 10832, 8369, 8370, 8371, 10847, 10848, 10849,
]);

export const VERZIK_IDS = new Set([
  ...VERZIK_P1_IDS, 10833, 10834, 10835, 8372, 8373, 8374, 10850, 10851, 10852,
]);

export const TEKTON_IDS = new Set([7540, 7543, 7544, 7545]);
export const SCAVENGER_BEAST_IDS = new Set([7548, 7549]);
export const VESPINE_SOLDIER_IDS = new Set([7538, 7539]);
export const DEATHLY_RANGER_IDS = new Set([7559]);
export const GUARDIAN_IDS = new Set([7569, 7571, 7570, 7572]);
export const OLM_HEAD_IDS = new Set([7551, 7554]);
export const OLM_MELEE_HAND_IDS = new Set([7552, 7555]);
export const OLM_MAGE_HAND_IDS = new Set([7550, 7553]);
export const GLOWING_CRYSTAL_IDS = new Set([7568]);
export const ICE_DEMON_IDS = new Set([7584, 7585]);
export const VESPULA_IDS = new Set([7530, 7531, 7532]);
export const ABYSSAL_PORTAL_IDS = new Set([7533]);
export const FRAGMENT_OF_SEREN_IDS = new Set([8917, 8918, 8919, 8920]);
export const ZULRAH_IDS = new Set([2042, 2043, 2044]);
export const NIGHTMARE_TOTEM_IDS = new Set([
  9434, 9437, 9440, 9443, 9435, 9438, 9441, 9444,
]);

export const OLM_IDS = new Set([
  ...OLM_HEAD_IDS, ...OLM_MELEE_HAND_IDS, ...OLM_MAGE_HAND_IDS,
]);

export const COX_MAGIC_IS_DEFENSIVE_IDS = new Set([
  ...DEATHLY_RANGER_IDS, ...TEKTON_IDS, ...ABYSSAL_PORTAL_IDS, ...VESPULA_IDS,
  ...VESPINE_SOLDIER_IDS, ...OLM_MELEE_HAND_IDS, ...OLM_MAGE_HAND_IDS,
]);

export const COX_SINGLES_SCALING_IDS = new Set([
  ...SCAVENGER_BEAST_IDS, ...VESPINE_SOLDIER_IDS,
]);

export const USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_IDS = new Set([
  ...ICE_DEMON_IDS, ...VERZIK_IDS, ...FRAGMENT_OF_SEREN_IDS, 11709, 11712, 9118,
]);

const DUSK_IDS = [7851, 7854, 7855, 7882, 7883, 7886, 7887, 7888, 7889];

export const IMMUNE_TO_MELEE_IDS = new Set([
  ...ZULRAH_IDS, ...ABYSSAL_PORTAL_IDS, 494, 7706, 7708, 12214, 12215, 12219,
]);

export const IMMUNE_TO_RANGED_IDS = new Set([
  ...TEKTON_IDS, ...DUSK_IDS, ...GLOWING_CRYSTAL_IDS,
]);

export const IMMUNE_TO_NON_SALAMANDER_MELEE_IDS = new Set([
  3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180,
  3181, 3182, 3183, 7037,
]);

export const IMMUNE_TO_MAGIC_IDS = new Set<number>([]);

export const ONE_HIT_MONSTERS = new Set([7223, 8584, 11193]);
export const GUARANTEED_ACCURACY_MONSTERS = new Set([5916]);
export const INFINITE_HEALTH_MONSTERS = new Set([14779]);

export const YAMA_IDS = new Set([14176]);
export const YAMA_VOID_FLARE_IDS = new Set([14179]);

export const ALWAYS_MAX_HIT_MELEE = new Set([11710, 11713, 12814, 11755, 11758, 14179]);
export const ALWAYS_MAX_HIT_RANGED = new Set([11711, 11714, 12815, 11717, 11715, 14179]);
export const ALWAYS_MAX_HIT_MAGIC = new Set([11709, 11712, 12816, 14151, 14150, 14179]);
