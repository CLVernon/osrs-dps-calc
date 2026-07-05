package com.osrs.dps.calc;

import com.osrs.dps.calc.dist.AttackDist;
import com.osrs.dps.calc.dist.HitDist;
import com.osrs.dps.calc.dist.Transforms;
import com.osrs.dps.calc.dist.WeightedHit;
import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.SpellData;
import com.osrs.dps.model.Stance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Player-vs-NPC combat calculator, ported from the OSRS wiki DPS tool
 * (github.com/weirdgloop/osrs-dps-calc). Computes hit distributions so that
 * multi-hit weapons, proc effects and damage caps compose correctly.
 */
public final class PlayerVsNpcCalc {

    private final PlayerSetup p;
    private final Monster m;
    private final Gear g;
    private final EquipmentTotals totals;
    private final List<String> notes = new ArrayList<>();
    /** Override for the NPC defence roll (brimstone ring sub-calculation). */
    private final Long defenceRollOverride;

    private PlayerVsNpcCalc(PlayerSetup p, Monster m, Long defenceRollOverride) {
        this.p = p;
        this.m = m;
        this.g = new Gear(p);
        this.totals = new EquipmentTotals(p, m, g);
        this.defenceRollOverride = defenceRollOverride;
    }

    public static DpsResult calculate(PlayerSetup player, Monster monster) {
        return new PlayerVsNpcCalc(player, CoxScaling.scale(monster), null).run();
    }

    private DpsResult run() {
        if (m.hasAttribute("xerician") && (m.partySize > 1 || m.coxChallengeMode)) {
            note("CoX scaling: " + m.partySize + " players"
                    + (m.coxChallengeMode ? ", Challenge Mode" : ""));
        }
        AttackDist dist = applyNpcTransforms(attackerDist());
        double accuracy = displayHitChance();
        double expected = dist.expectedDamage();
        int maxHit = dist.maxDamage();
        int speed = p.attackSpeedTicks();
        double dps = expected / (speed * CombatConstants.SECONDS_PER_TICK);
        double htk = expectedHitsToKill(dist);
        double ttk = htk > 0 ? htk * speed * CombatConstants.SECONDS_PER_TICK : Double.POSITIVE_INFINITY;
        return new DpsResult(maxHit, accuracy, expected, speed, dps, htk, ttk, notes);
    }

    private void note(String note) {
        if (!notes.contains(note)) {
            notes.add(note);
        }
    }

    private boolean isMelee() {
        return p.getAttackType().isMelee();
    }

    private boolean isUndead() {
        return m.isUndead();
    }

    private boolean onTask() {
        return p.isOnSlayerTask();
    }

    // ------------------------------------------------------------ NPC defence

    long npcDefenceRoll() {
        if (defenceRollOverride != null) {
            return defenceRollOverride;
        }
        AttackType type = p.getAttackType();

        int level = type == AttackType.MAGIC
                && !CombatConstants.USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_IDS.contains(m.id)
                ? m.skills.magic
                : m.skills.def;
        int effectiveLevel = level + 9;

        int bonus;
        if (type == AttackType.RANGED) {
            bonus = switch (rangedDamageType()) {
                case "light" -> m.defensive.light;
                case "heavy" -> m.defensive.heavy;
                case "mixed" -> (m.defensive.light + m.defensive.standard + m.defensive.heavy) / 3;
                default -> m.defensive.standard;
            };
        } else {
            bonus = switch (type) {
                case STAB -> m.defensive.stab;
                case SLASH -> m.defensive.slash;
                case CRUSH -> m.defensive.crush;
                default -> m.defensive.magic;
            };
        }

        long defenceRoll = (long) effectiveLevel * (bonus + 64);

        boolean isCustom = m.id <= 0;
        if ((isCustom || (CombatConstants.TOA_MONSTER_IDS.contains(m.id)
                && !CombatConstants.KEPHRI_OVERLORD_IDS.contains(m.id)))
                && m.toaInvocationLevel > 0) {
            defenceRoll = defenceRoll * (250 + m.toaInvocationLevel) / 250;
            note("ToA invocation " + m.toaInvocationLevel + ": defence scaled");
        }
        return defenceRoll;
    }

    private String rangedDamageType() {
        return switch (g.weaponCategory()) {
            case "Thrown" -> "light";
            case "Bow" -> "standard";
            case "Crossbow", "Chinchompas" -> "heavy";
            case "Salamander" -> "mixed";
            default -> "standard";
        };
    }

    // ------------------------------------------------------------------ melee

    private long meleeAttackRoll() {
        int effectiveLevel = p.getPrayer().applyAccuracy(p.visibleAttack());

        int stanceBonus = 8;
        if (p.getStance() == Stance.ACCURATE) {
            stanceBonus += 3;
        } else if (p.getStance() == Stance.CONTROLLED) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (g.meleeVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
            note("Void melee");
        }

        long baseRoll = (long) effectiveLevel * (totals.attackBonus(p.getAttackType()) + 64);
        long attackRoll = baseRoll;

        // Non-stacking target-specific bonuses
        if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            attackRoll = attackRoll * (p.isForinthrySurge() ? 27 : 24) / 20;
            note("Amulet of avarice vs revenant");
        } else if ((g.salveE() || g.salveEi()) && isUndead()) {
            attackRoll = attackRoll * 6 / 5;
            note("Salve amulet (e) vs undead");
        } else if ((g.salve() || g.salveI()) && isUndead()) {
            attackRoll = attackRoll * 7 / 6;
            note("Salve amulet vs undead");
        } else if (g.blackMask() && onTask()) {
            attackRoll = attackRoll * 7 / 6;
            note("Black mask/slayer helmet on task");
        }

        if (g.tzhaarWeapon() && g.obsidianArmour()) {
            attackRoll += baseRoll / 10;
            note("Obsidian set + Tzhaar weapon");
        }
        if (g.revWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
            note("Wilderness weapon vs wilderness target");
        }
        if (g.wearing("Arclight", "Emberlight") && m.isDemon()) {
            attackRoll += attackRoll * 70 / 100;
            note("Arclight/Emberlight vs demon");
        }
        if (g.wearing("Bone claws", "Burning claws") && m.isDemon()) {
            attackRoll += attackRoll * 5 / 100;
        }
        if (m.isDragon()) {
            if (g.wearing("Dragon hunter lance")) {
                attackRoll = attackRoll * 6 / 5;
                note("Dragon hunter lance vs dragon");
            } else if (g.wearing("Dragon hunter wand")) {
                attackRoll = attackRoll * 7 / 4;
                note("Dragon hunter wand vs dragon");
            }
        }
        if (g.wearing("Keris partisan of breaching") && m.isKalphite()) {
            attackRoll = attackRoll * 133 / 100;
            note("Keris partisan of breaching vs kalphite");
        }
        if (g.wearing("Blisterwood flail", "Blisterwood sickle") && isVampyre()) {
            attackRoll = attackRoll * 21 / 20;
        }
        if (g.silverWeapon() && g.wearing("Efaritay's aid") && isVampyre()) {
            attackRoll = attackRoll * 23 / 20;
            note("Efaritay's aid vs vampyre");
        }
        if (g.wearing("Granite hammer") && m.hasAttribute("golem")) {
            attackRoll = attackRoll * 13 / 10;
            note("Granite hammer vs golem");
        }
        attackRoll = applyInquisitor(attackRoll);
        return attackRoll;
    }

    private long applyInquisitor(long roll) {
        if (p.getAttackType() != AttackType.CRUSH) {
            return roll;
        }
        int pieces = g.inquisitorPieces();
        if (pieces == 0) {
            return roll;
        }
        int weight = pieces;
        if (g.inquisitorsMace()) {
            weight = pieces * 5; // 2.5% per piece with the mace, no set bonus
        } else if (pieces == 3) {
            weight = 5; // full set adds 1.0%
        }
        note("Inquisitor's armour (crush)");
        return roll * (200 + weight) / 200;
    }

    /** Returns {minHit, maxHit} for melee. */
    private int[] meleeMinMax() {
        int baseLevel = p.visibleStrength();
        int effectiveLevel = p.getPrayer().applyStrength(baseLevel);
        // Burst of Strength quirk at very low levels is ignored (combined prayer tiers).

        int stanceBonus = 8;
        if (p.getStance() == Stance.AGGRESSIVE) {
            stanceBonus += 3;
        } else if (p.getStance() == Stance.CONTROLLED) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (g.meleeVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int baseMax = EquipmentTotals.maxHitFromEffective(effectiveLevel, totals.str);
        int minHit = 0;
        int maxHit = baseMax;

        if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            maxHit = maxHit * (p.isForinthrySurge() ? 27 : 24) / 20;
        } else if ((g.salveE() || g.salveEi()) && isUndead()) {
            maxHit = maxHit * 6 / 5;
        } else if ((g.salve() || g.salveI()) && isUndead()) {
            maxHit = maxHit * 7 / 6;
        } else if (g.blackMask() && onTask()) {
            maxHit = maxHit * 7 / 6;
        }

        if (g.wearing("Arclight", "Emberlight") && m.isDemon()) {
            maxHit += maxHit * 70 / 100;
        }
        if (g.wearing("Bone claws", "Burning claws") && m.isDemon()) {
            maxHit += maxHit * 5 / 100;
        }
        if (g.tzhaarWeapon() && g.obsidianArmour()) {
            maxHit += baseMax / 10;
        }
        if (g.wearing("Dragon hunter lance") && m.isDragon()) {
            maxHit = maxHit * 6 / 5;
        }
        if (g.wearing("Dragon hunter wand") && m.isDragon()) {
            maxHit = maxHit * 7 / 5;
        }
        if (g.keris() && m.isKalphite()) {
            maxHit = g.wearing("Keris partisan of amascut")
                    ? maxHit * 115 / 100
                    : maxHit * 133 / 100;
            note("Keris vs kalphite");
        }
        if (g.wearing("Barronite mace") && m.hasAttribute("golem")) {
            maxHit = maxHit * 23 / 20;
            note("Barronite mace vs golem");
        }
        if (g.wearing("Granite hammer") && m.hasAttribute("golem")) {
            maxHit = maxHit * 13 / 10;
        }
        if (g.revWeaponBuffApplicable()) {
            maxHit = maxHit * 3 / 2;
        }
        if (g.wearing("Silverlight", "Darklight", "Silverlight (dyed)") && m.isDemon()) {
            maxHit += maxHit * 60 / 100;
            note("Silverlight/Darklight vs demon");
        }
        if (g.wearing("Leaf-bladed battleaxe") && m.isLeafy()) {
            maxHit = maxHit * 47 / 40;
            note("Leaf-bladed battleaxe vs leafy");
        }
        if (g.wearing("Colossal blade")) {
            maxHit += Math.min(m.size * 2, 10);
            note("Colossal blade vs size " + m.size);
        }
        if (g.ratBoneWeapon() && m.hasAttribute("rat")) {
            maxHit += 10;
            note("Rat bone weapon vs rat");
        }
        maxHit = (int) applyInquisitorDamage(maxHit);

        if (g.fang()) {
            int shrink = maxHit * 3 / 20;
            minHit = shrink;
            maxHit -= shrink;
            note("Osmumten's fang");
        }

        return new int[] {minHit, maxHit};
    }

    private long applyInquisitorDamage(long maxHit) {
        if (p.getAttackType() != AttackType.CRUSH) {
            return maxHit;
        }
        int pieces = g.inquisitorPieces();
        if (pieces == 0) {
            return maxHit;
        }
        int weight = pieces;
        if (g.inquisitorsMace()) {
            weight = pieces * 5;
        } else if (pieces == 3) {
            weight = 5;
        }
        return maxHit * (200 + weight) / 200;
    }

    // ----------------------------------------------------------------- ranged

    private long rangedAttackRoll() {
        int effectiveLevel = p.getPrayer().applyAccuracy(p.visibleRanged());
        if (p.getStance() == Stance.ACCURATE) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;
        if (g.rangedVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
            note("Void ranged");
        }

        long attackRoll = (long) effectiveLevel * (totals.ranged + 64);

        if (g.crystalBow()) {
            int weight = g.crystalArmourWeight();
            if (weight > 0) {
                attackRoll = attackRoll * (20 + weight) / 20;
                note("Crystal armour + crystal bow");
            }
        }

        if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            attackRoll = attackRoll * (p.isForinthrySurge() ? 27 : 24) / 20;
            note("Amulet of avarice vs revenant");
        } else if (g.salveEi() && isUndead()) {
            attackRoll = attackRoll * 6 / 5;
            note("Salve amulet(ei) vs undead");
        } else if (g.salveI() && isUndead()) {
            attackRoll = attackRoll * 7 / 6;
            note("Salve amulet(i) vs undead");
        } else if (g.imbuedBlackMask() && onTask()) {
            attackRoll = attackRoll * 23 / 20;
            note("Slayer helmet (i) on task");
        }

        if (g.twistedBow()) {
            int cap = m.hasAttribute("xerician") ? 350 : 250;
            int tbowMagic = Math.min(cap, Math.max(m.skills.magic, m.offensive.magic));
            attackRoll = tbowScaling(attackRoll, tbowMagic, true);
            if (CombatConstants.P2_WARDEN_IDS.contains(m.id)) {
                attackRoll = tbowScaling(attackRoll, tbowMagic, true);
            }
            note("Twisted bow vs magic " + tbowMagic);
        }
        if (g.revWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
            note("Wilderness weapon vs wilderness target");
        }
        if (g.wearing("Dragon hunter crossbow") && m.isDragon()) {
            attackRoll = attackRoll * 13 / 10;
            note("Dragon hunter crossbow vs dragon");
        }
        if (g.chinchompa()) {
            attackRoll = attackRoll * chinchompaFuseNumerator() / 4;
            note("Chinchompa fuse at distance " + p.getChinchompaDistance());
        }
        if (g.wearing("Scorching bow") && m.isDemon()) {
            attackRoll += attackRoll * 30 / 100;
            note("Scorching bow vs demon");
        }
        return attackRoll;
    }

    private int chinchompaFuseNumerator() {
        int distance = Math.min(7, Math.max(1, p.getChinchompaDistance()));
        Stance stance = p.getStance();
        // Short fuse = accurate, medium = rapid, long = longrange
        if (stance == Stance.ACCURATE) {
            if (distance >= 7) {
                return 2;
            }
            return distance >= 4 ? 3 : 4;
        }
        if (stance == Stance.RAPID) {
            return (distance < 4 || distance >= 7) ? 3 : 4;
        }
        if (distance < 4) {
            return 2;
        }
        return distance < 7 ? 3 : 4;
    }

    /** Returns {minHit, maxHit} for ranged. */
    private int[] rangedMinMax() {
        boolean scalesWithStr = g.wearing("Eclipse atlatl", "Hunter's spear");
        int effectiveLevel = scalesWithStr ? p.visibleStrength() : p.visibleRanged();

        if (g.wearing("Holy water")) {
            if (!m.isDemon()) {
                note("Holy water only damages demons");
                return new int[] {0, 0};
            }
            effectiveLevel += 10;
            int str = 64 + (p.getWeapon() != null ? p.getWeapon().bonuses.rangedStr : 0);
            int maxHit = EquipmentTotals.maxHitFromEffective(effectiveLevel, str - 64);
            maxHit += maxHit * 60 / 100;
            note("Holy water vs demon");
            return new int[] {0, maxHit};
        }

        if (g.wearing("Ogre bow", "Comp ogre bow")) {
            effectiveLevel += 10;
            int bonusStr = p.getEquipped(com.osrs.dps.model.EquipmentSlot.AMMO) != null
                    ? p.getEquipped(com.osrs.dps.model.EquipmentSlot.AMMO).bonuses.rangedStr
                    : 0;
            int maxHit = (effectiveLevel * (bonusStr + 64) + 320) / 640;
            note("Ogre bow: ammo strength only");
            return new int[] {0, maxHit};
        }

        effectiveLevel = p.getPrayer().applyStrength(effectiveLevel);
        if (p.getStance() == Stance.ACCURATE) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (g.eliteRangedVoid()) {
            effectiveLevel = effectiveLevel * 9 / 8;
            note("Elite void ranged");
        } else if (g.rangedVoid()) {
            effectiveLevel = effectiveLevel * 11 / 10;
        }

        int bonusStr = scalesWithStr ? totals.str : totals.rangedStr;
        int baseMax = EquipmentTotals.maxHitFromEffective(effectiveLevel, bonusStr);
        int minHit = 0;
        int maxHit = baseMax;

        if (g.crystalBow()) {
            int weight = g.crystalArmourWeight();
            if (weight > 0) {
                maxHit = maxHit * (40 + weight) / 40;
            }
        }

        boolean needRevBuff = g.revWeaponBuffApplicable();
        boolean needDragonbane = g.wearing("Dragon hunter crossbow") && m.isDragon();
        boolean needDemonbane = g.wearing("Scorching bow") && m.isDemon();

        if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            maxHit = maxHit * (p.isForinthrySurge() ? 27 : 24) / 20;
        } else if ((g.salveEi() || (scalesWithStr && g.salveE())) && isUndead()) {
            maxHit = maxHit * 6 / 5;
        } else if ((g.salveI() || (scalesWithStr && g.salve())) && isUndead()) {
            maxHit = maxHit * 7 / 6;
        } else if (scalesWithStr && g.blackMask() && onTask()) {
            maxHit = maxHit * 7 / 6;
        } else if (g.imbuedBlackMask() && onTask()) {
            int numerator = 23;
            if (needRevBuff) {
                needRevBuff = false;
                numerator += 10;
            }
            if (needDragonbane) {
                needDragonbane = false;
                numerator += 5;
            }
            if (needDemonbane) {
                needDemonbane = false;
                numerator += 6;
            }
            maxHit = maxHit * numerator / 20;
        }

        if (g.twistedBow()) {
            int cap = m.hasAttribute("xerician") ? 350 : 250;
            int tbowMagic = Math.min(cap, Math.max(m.skills.magic, m.offensive.magic));
            maxHit = (int) tbowScaling(maxHit, tbowMagic, false);
        }
        if (needRevBuff) {
            maxHit = maxHit * 3 / 2;
        }
        if (needDragonbane) {
            maxHit = maxHit * 5 / 4;
        }
        if (needDemonbane) {
            maxHit += maxHit * 30 / 100;
        }
        if (g.ratBoneWeapon() && m.hasAttribute("rat")) {
            maxHit += 10;
        }
        if (g.wearing("Tonalztics of ralos")) {
            maxHit = maxHit * 3 / 4;
            note("Tonalztics of ralos");
        }

        if (CombatConstants.P2_WARDEN_IDS.contains(m.id)) {
            int[] adjusted = applyP2WardensDamage(maxHit);
            minHit = adjusted[0];
            maxHit = adjusted[1];
        }
        return new int[] {minHit, maxHit};
    }

    static long tbowScaling(long current, int magic, boolean accuracyMode) {
        int factor = accuracyMode ? 10 : 14;
        int base = accuracyMode ? 140 : 250;
        long t2 = (3L * magic - factor) / 100;
        long t3 = (long) Math.pow(3L * magic / 10 - 10L * factor, 2) / 100;
        long bonus = base + t2 - t3;
        return current * bonus / 100;
    }

    // ------------------------------------------------------------------ magic

    private long magicAttackRoll() {
        int effectiveLevel = p.getPrayer().applyAccuracy(p.visibleMagic());
        if (p.getStance() == Stance.ACCURATE) {
            effectiveLevel += 2;
        }
        effectiveLevel += 9;
        if (g.magicVoid()) {
            effectiveLevel = effectiveLevel * 29 / 20;
            note("Void magic");
        }

        long baseRoll = (long) effectiveLevel * (totals.magic + 64);
        long attackRoll = baseRoll;

        int additiveBonus = 0;
        boolean blackMaskBonus = false;
        if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            additiveBonus += p.isForinthrySurge() ? 35 : 20;
            note("Amulet of avarice vs revenant");
        } else if (g.salveEi() && isUndead()) {
            additiveBonus += 20;
            note("Salve amulet(ei) vs undead");
        } else if (g.salveI() && isUndead()) {
            additiveBonus += 15;
            note("Salve amulet(i) vs undead");
        } else if (g.imbuedBlackMask() && onTask()) {
            blackMaskBonus = true;
            note("Slayer helmet (i) on task");
        }
        if (g.wearing("Efaritay's aid") && isVampyre() && g.silverWeapon()) {
            additiveBonus += 15;
        }
        if (g.smokeStaff() && p.getSpell() != null && "standard".equals(p.getSpell().spellbook)) {
            additiveBonus += 10;
            note("Smoke battlestaff with standard spell");
        }
        if (additiveBonus != 0) {
            attackRoll = attackRoll * (100 + additiveBonus) / 100;
        }

        if (m.isDragon()) {
            if (g.wearing("Dragon hunter crossbow")) {
                attackRoll = attackRoll * 13 / 10;
            } else if (g.wearing("Dragon hunter lance")) {
                attackRoll = attackRoll * 6 / 5;
            } else if (g.wearing("Dragon hunter wand")) {
                attackRoll = attackRoll * 7 / 4;
                note("Dragon hunter wand vs dragon");
            }
        }
        if (blackMaskBonus) {
            attackRoll = attackRoll * 23 / 20;
        }
        if (p.getSpell() != null && p.getSpell().isDemonbane() && m.isDemon()) {
            int percent = p.isMarkOfDarkness() ? 40 : 20;
            if (g.wearing("Purging staff")) {
                percent *= 2;
            }
            attackRoll += attackRoll * percent / 100;
            note("Demonbane spell vs demon" + (p.isMarkOfDarkness() ? " (Mark of Darkness)" : ""));
        }
        if (g.revWeaponBuffApplicable()) {
            attackRoll = attackRoll * 3 / 2;
            note("Wilderness weapon vs wilderness target");
        }
        if (g.wearing("Tome of water") && p.getSpell() != null
                && ("water".equals(spellElement()) || p.getSpell().isBindSpell())) {
            attackRoll = attackRoll * 6 / 5;
            note("Tome of water");
        }

        String element = spellElement();
        if (element != null && m.weakness != null && element.equalsIgnoreCase(m.weakness.element)) {
            attackRoll += baseRoll * m.weakness.severity / 100;
            note("Elemental weakness: " + element + " +" + m.weakness.severity + "%");
        }
        return attackRoll;
    }

    private String spellElement() {
        return p.getSpell() == null ? null : p.getSpell().effectiveElement();
    }

    /** Returns {minHit, maxHit} for magic. */
    private int[] magicMinMax() {
        int magicLevel = p.visibleMagic();
        SpellData spell = p.isCastingSpell() || (!g.poweredStaff() && !g.salamander())
                ? p.getSpell() : null;
        int maxHit = 0;
        int minHit = 0;

        if (spell != null) {
            maxHit = spell.maxHitAtLevel(magicLevel, DataRepository.get().allSpells());
            if ("Magic Dart".equals(spell.name)) {
                if (g.wearing("Slayer's staff (e)") && onTask()) {
                    maxHit = 13 + magicLevel / 6;
                } else {
                    maxHit = 10 + magicLevel / 10;
                }
            }
        } else {
            maxHit = poweredStaffMaxHit(magicLevel);
            if (maxHit < 0) {
                note("No spell selected and weapon has no built-in spell");
                return new int[] {0, 0};
            }
        }

        if (maxHit == 0) {
            return new int[] {0, 0};
        }

        if (g.wearing("Chaos gauntlets") && spell != null
                && spell.name.toLowerCase().contains("bolt")) {
            maxHit += 3;
            note("Chaos gauntlets");
        }
        if (g.chargeSpellApplicable()) {
            maxHit += 10;
            note("Charge with god cape");
        }

        int baseMax = maxHit;
        int magicDmgTenths = totals.magicStrTenths;

        if (g.smokeStaff() && spell != null && "standard".equals(spell.spellbook)) {
            magicDmgTenths += 100;
        }

        boolean blackMaskBonus = false;
        if (g.salveEi() && isUndead()) {
            magicDmgTenths += 200;
        } else if (g.salveI() && isUndead()) {
            magicDmgTenths += 150;
        } else if (g.wearing("Amulet of avarice") && m.name != null && m.name.startsWith("Revenant")) {
            magicDmgTenths += p.isForinthrySurge() ? 350 : 200;
        } else if (g.imbuedBlackMask() && onTask()) {
            blackMaskBonus = true;
        }

        magicDmgTenths += p.getPrayer().magicDamageTenths();

        maxHit += maxHit * magicDmgTenths / 1000;

        if (blackMaskBonus) {
            maxHit = maxHit * 23 / 20;
        }
        if (m.isDragon()) {
            if (g.wearing("Dragon hunter lance")) {
                maxHit = maxHit * 6 / 5;
            } else if (g.wearing("Dragon hunter wand")) {
                maxHit = maxHit * 7 / 5;
            } else if (g.wearing("Dragon hunter crossbow")) {
                maxHit = maxHit * 5 / 4;
            }
        }
        if (g.revWeaponBuffApplicable()) {
            maxHit = maxHit * 3 / 2;
        }

        String element = spellElement();
        if (element != null && m.weakness != null && element.equalsIgnoreCase(m.weakness.element)) {
            maxHit += baseMax * m.weakness.severity / 100;
        }

        if (p.isSunfireRunes() && p.getSpell() != null && p.getSpell().isFireSpell()) {
            minHit = maxHit / 10;
            note("Sunfire runes");
        }

        boolean chargedShield = p.getEquipped(com.osrs.dps.model.EquipmentSlot.SHIELD) != null
                && "Charged".equals(p.getEquipped(com.osrs.dps.model.EquipmentSlot.SHIELD).version);
        if (chargedShield
                && ((g.wearing("Tome of fire") && "fire".equals(element))
                || (g.wearing("Tome of water") && "water".equals(element))
                || (g.wearing("Tome of earth") && "earth".equals(element)))) {
            maxHit = maxHit * 11 / 10;
            note("Elemental tome");
        }

        if (CombatConstants.P2_WARDEN_IDS.contains(m.id)) {
            int[] adjusted = applyP2WardensDamage(maxHit);
            minHit = adjusted[0];
            maxHit = adjusted[1];
        }
        return new int[] {minHit, maxHit};
    }

    /** Built-in max hit of powered staves and salamanders; -1 if unknown. */
    private int poweredStaffMaxHit(int magicLevel) {
        String w = g.weaponName();
        return switch (w) {
            case "Starter staff" -> 8;
            case "Trident of the seas", "Trident of the seas (e)" ->
                    Math.max(1, magicLevel / 3 - 5);
            case "Thammaron's sceptre" -> Math.max(1, magicLevel / 3 - 8);
            case "Accursed sceptre" -> Math.max(1, magicLevel / 3 - 6);
            case "Trident of the swamp", "Trident of the swamp (e)" ->
                    Math.max(1, magicLevel / 3 - 2);
            case "Sanguinesti staff", "Holy sanguinesti staff" -> Math.max(1, magicLevel / 3 - 1);
            case "Dawnbringer" -> Math.max(1, magicLevel / 6 - 1);
            case "Tumeken's shadow" -> Math.max(1, magicLevel / 3) + 1;
            case "Eye of ayak" -> Math.max(1, magicLevel / 3 - 6);
            case "Warped sceptre" -> Math.max(1, (8 * magicLevel + 96) / 37);
            case "Bone staff" -> Math.max(1, magicLevel / 3 - 5) + 10;
            case "Crystal staff (basic)", "Corrupted staff (basic)" -> 23;
            case "Crystal staff (attuned)", "Corrupted staff (attuned)" -> 31;
            case "Crystal staff (perfected)", "Corrupted staff (perfected)" -> 39;
            case "Swamp lizard" -> (magicLevel * (56 + 64) + 320) / 640;
            case "Orange salamander" -> (magicLevel * (59 + 64) + 320) / 640;
            case "Red salamander" -> (magicLevel * (77 + 64) + 320) / 640;
            case "Black salamander" -> (magicLevel * (92 + 64) + 320) / 640;
            case "Tecu salamander" -> (magicLevel * (104 + 64) + 320) / 640;
            default -> -1;
        };
    }

    private int[] applyP2WardensDamage(int max) {
        long reducedDefence = npcDefenceRoll() / 3;
        long delta = Math.max(maxAttackRoll() - reducedDefence, 0);
        // linear interpolation from 15% at 0 to 40% at 42000
        long modifier = Math.max(15, Math.min(40, 15 + delta * (40 - 15) / 42_000));
        note("P2 Wardens damage modifier");
        return new int[] {(int) (max * modifier / 100), (int) (max * (modifier + 20) / 100)};
    }

    // --------------------------------------------------------------- accuracy

    long maxAttackRoll() {
        return switch (p.getAttackType()) {
            case STAB, SLASH, CRUSH -> meleeAttackRoll();
            case RANGED -> rangedAttackRoll();
            case MAGIC -> magicAttackRoll();
        };
    }

    private int[] minMax() {
        int[] mm = switch (p.getAttackType()) {
            case STAB, SLASH, CRUSH -> meleeMinMax();
            case RANGED -> rangedMinMax();
            case MAGIC -> magicMinMax();
        };
        if (mm[0] > mm[1]) {
            mm[1] = mm[0];
        }
        mm[0] = Math.max(mm[0], 0);
        mm[1] = Math.max(mm[1], 0);
        return mm;
    }

    static double normalAccuracy(long atk, long def) {
        if (atk >= 0 && def >= 0) {
            return atk > def
                    ? 1 - (def + 2.0) / (2.0 * (atk + 1))
                    : atk / (2.0 * (def + 1));
        }
        if (atk < 0) {
            atk = Math.min(0, atk + 2);
        }
        if (def < 0) {
            def = Math.min(0, def + 2);
        }
        if (atk >= 0) {
            return 1 - 1.0 / (-def + 1) / (atk + 1);
        }
        if (def >= 0) {
            return 0;
        }
        long a = -def;
        long d = -atk;
        return a > d
                ? 1 - (d + 2.0) / (2.0 * (a + 1))
                : a / (2.0 * (d + 1));
    }

    /** The fang's double-roll accuracy (exact formula from the wiki tool). */
    static double fangAccuracy(long atk, long def) {
        if (atk >= 0 && def >= 0) {
            if (atk > def) {
                return 1 - (def + 2.0) * (2 * def + 3.0) / (atk + 1) / (atk + 1) / 6;
            }
            return atk * (4.0 * atk + 5) / 6 / (atk + 1) / (def + 1);
        }
        if (atk < 0) {
            atk = Math.min(0, atk + 2);
        }
        if (def < 0) {
            def = Math.min(0, def + 2);
        }
        if (atk >= 0) {
            return 1 - 1.0 / (-def + 1) / (atk + 1);
        }
        if (def >= 0) {
            return 0;
        }
        // both negative: reverse roll
        long a = -def;
        long d = -atk;
        if (a < d) {
            return a * (d * 6.0 - 2 * a + 5) / 6 / (d + 1) / (d + 1);
        }
        return 1 - (d + 2.0) * (2 * d + 3.0) / (a + 1) / (a + 1) / 6;
    }

    double hitChance() {
        if (CombatConstants.GUARANTEED_ACCURACY_MONSTERS.contains(m.id)) {
            return 1.0;
        }
        if (m.id == 7223 /* Scurrius giant rat */) {
            return 1.0;
        }
        if (CombatConstants.P2_WARDEN_IDS.contains(m.id)) {
            return 1.0;
        }
        if (alwaysMaxHits()) {
            return 1.0;
        }

        long atk = maxAttackRoll();
        long def = npcDefenceRoll();
        double chance = normalAccuracy(atk, def);

        if (g.fang() && p.getAttackType() == AttackType.STAB) {
            if (CombatConstants.TOA_MONSTER_IDS.contains(m.id)) {
                chance = 1 - (1 - chance) * (1 - chance);
            } else {
                chance = fangAccuracy(atk, def);
            }
        }
        return chance;
    }

    /** Hit chance including the brimstone ring's 25% defence-reduction proc. */
    double displayHitChance() {
        double chance = hitChance();
        if (chance == 0.0 || chance == 1.0) {
            return chance;
        }
        if (p.getAttackType() == AttackType.MAGIC && g.wearing("Brimstone ring")
                && defenceRollOverride == null) {
            double effectChance = normalAccuracy(maxAttackRoll(), npcDefenceRoll() * 9 / 10);
            chance = 0.75 * chance + 0.25 * effectChance;
        }
        return chance;
    }

    private boolean alwaysMaxHits() {
        return (isMelee() && CombatConstants.ALWAYS_MAX_HIT_MELEE.contains(m.id))
                || (p.getAttackType() == AttackType.RANGED
                        && CombatConstants.ALWAYS_MAX_HIT_RANGED.contains(m.id))
                || (p.getAttackType() == AttackType.MAGIC
                        && CombatConstants.ALWAYS_MAX_HIT_MAGIC.contains(m.id));
    }

    // ------------------------------------------------------------ distribution

    AttackDist attackerDist() {
        double acc = hitChance();
        int[] mm = minMax();
        int min = mm[0];
        int max = mm[1];

        if (max == 0) {
            return AttackDist.of(new HitDist(List.of(WeightedHit.inaccurate(1.0))));
        }

        if (CombatConstants.ONE_HIT_MONSTERS.contains(m.id)) {
            return AttackDist.of(HitDist.single(1.0, m.skills.hp));
        }

        HitDist standard = HitDist.linear(acc, min, max);
        AttackDist dist = AttackDist.of(standard);

        if (p.getAttackType() == AttackType.RANGED && g.wearing("Tonalztics of ralos")
                && p.getWeapon() != null && "Charged".equals(p.getWeapon().version)) {
            dist = new AttackDist(List.of(standard, standard));
        }

        if (isMelee() && g.wearing("Gadderhammer") && m.hasAttribute("shade")) {
            dist = AttackDist.of(new HitDist(concat(
                    standard.scaleProbability(0.95).scaleDamage(5, 4).hits(),
                    standard.scaleProbability(0.05).scaleDamage(2, 1).hits())));
            note("Gadderhammer vs shade");
        }

        if (p.getAttackType() == AttackType.RANGED && g.wearing("Dark bow")) {
            dist = new AttackDist(List.of(standard, standard));
            note("Dark bow: two arrows");
        }

        if (isMelee() && g.veracSet()) {
            dist = AttackDist.of(new HitDist(concat(
                    standard.scaleProbability(0.75).hits(),
                    HitDist.linear(1.0, 1, max + 1).scaleProbability(0.25).hits())));
            note("Verac's set: 25% guaranteed hits");
        }

        if (p.getAttackType() == AttackType.RANGED && g.karilSet()) {
            // 25% chance of an extra hitsplat at half damage
            dist = dist.transform(h -> new HitDist(List.of(
                    h.withProbability(0.75),
                    new WeightedHit(0.25, h.damage() + h.damage() / 2, true))), false);
            note("Karil's set: 25% bonus hit");
        }

        if (isMelee() && g.scythe()) {
            List<HitDist> hits = new ArrayList<>();
            for (int i = 0; i < Math.min(Math.max(m.size, 1), 3); i++) {
                int splatMax = max >> i;
                hits.add(HitDist.linear(acc, min, Math.max(min, splatMax)));
            }
            dist = new AttackDist(hits);
            if (hits.size() > 1) {
                note("Scythe: " + hits.size() + " hits vs size " + m.size);
            }
        }

        if (isMelee() && g.wearing("Dual macuahuitl")) {
            int firstMax = max / 2;
            int secondMax = max - firstMax;
            HitDist second = HitDist.linear(acc, min, Math.max(min, secondMax));
            HitDist first = HitDist.linear(acc, min, Math.max(min, firstMax));
            // second hit only rolls if the first lands
            dist = AttackDist.of(first.transform(h -> {
                List<WeightedHit> combined = new ArrayList<>();
                for (WeightedHit s : second.hits()) {
                    combined.add(new WeightedHit(s.probability(), h.damage() + s.damage(), true));
                }
                return new HitDist(combined);
            }, false));
            note("Dual macuahuitl");
        }

        if (isMelee() && g.wearing("Torag's hammers", "Sulphur blades",
                "Glacial temotli", "Earthbound tecpatl")) {
            int firstMax = max / 2;
            int secondMax = max - firstMax;
            dist = new AttackDist(List.of(
                    HitDist.linear(acc, min, Math.max(min, firstMax)),
                    HitDist.linear(acc, min, Math.max(min, secondMax))));
            note("Two-hit weapon");
        }

        if (isMelee() && g.keris() && m.isKalphite()) {
            dist = AttackDist.of(new HitDist(concat(
                    standard.scaleProbability(50.0 / 51.0).hits(),
                    standard.scaleProbability(1.0 / 51.0).scaleDamage(3, 1).hits())));
            note("Keris: 1/51 triple damage");
        }

        if (isMelee() && CombatConstants.GUARDIAN_IDS.contains(m.id) && g.pickaxe()) {
            int pickBonus = pickaxeBonus();
            int factor = 50 + p.getMiningLevel() + pickBonus;
            dist = dist.transform(Transforms.multiply(factor, 150, 0), true);
            note("CoX Guardians: pickaxe/mining scaling");
        }

        if (p.isMarkOfDarkness() && p.getSpell() != null && p.getSpell().isDemonbane() && m.isDemon()) {
            int percent = g.wearing("Purging staff") ? 50 : 25;
            dist = dist.transform(h -> new HitDist(List.of(
                    h.withDamage(h.damage() + h.damage() * percent / 100))), true);
            note("Mark of Darkness demonbane bonus");
        }

        if (p.getAttackType() == AttackType.MAGIC && g.ahrimSet()) {
            dist = dist.transform(h -> new HitDist(List.of(
                    h.withProbability(0.75),
                    new WeightedHit(0.25, h.damage() * 13 / 10, h.accurate()))), true);
            note("Ahrim's set: 25% +30% damage");
        }

        if (isMelee() && g.dharokSet()) {
            int maxHp = p.getHitpointsLevel();
            int currentHp = p.getCurrentHitpoints();
            dist = dist.scaleDamage(10000 + (maxHp - currentHp) * maxHp, 10000);
            note("Dharok's set at " + currentHp + "/" + maxHp + " HP");
        }

        if (isMelee() && g.berserkerNecklace() && g.tzhaarWeapon()) {
            dist = dist.scaleDamage(6, 5);
            note("Berserker necklace + Tzhaar weapon");
        }

        if (isVampyre()) {
            dist = applyVampyreScaling(dist);
        }

        dist = applyBoltEffects(dist, acc, max, false);

        // Accurate zero-damage hits deal 1 damage
        boolean accurateZeroApplies = p.getSpell() == null || p.getSpell().maxHit > 0
                || g.poweredStaff() || g.salamander();
        if (accurateZeroApplies) {
            dist = dist.transform(h -> new HitDist(List.of(
                    new WeightedHit(1.0, Math.max(h.damage(), 1), true))), false);
        }

        if (p.getAttackType() == AttackType.MAGIC && p.getSpell() != null
                && "standard".equals(p.getSpell().spellbook) && g.wearing("Twinflame staff")
                && containsAny(p.getSpell().name, "Bolt", "Blast", "Wave")) {
            dist = dist.transform(h -> new HitDist(List.of(
                    h.withDamage(h.damage() + h.damage() * 4 / 10))), true);
            note("Twinflame staff double hit");
        }

        if ("Corporeal Beast".equals(m.name) && !g.corpbaneWeapon()) {
            dist = dist.transform(Transforms.division(2, 0), true);
            note("Corporeal Beast: half damage without corpbane weapon");
        }

        dist = applyBoltEffects(dist, acc, max, true);

        if (p.getAttackType() == AttackType.MAGIC && g.wearing("Brimstone ring")
                && defenceRollOverride == null) {
            long effectDef = npcDefenceRoll() * 9 / 10;
            AttackDist effectDist = new PlayerVsNpcCalc(p, m, effectDef).attackerDist();
            List<HitDist> mixed = new ArrayList<>();
            for (int i = 0; i < dist.dists().size(); i++) {
                mixed.add(new HitDist(concat(
                        dist.dists().get(i).scaleProbability(0.75).hits(),
                        effectDist.dists().get(i).scaleProbability(0.25).hits())).flatten());
            }
            dist = new AttackDist(mixed);
            note("Brimstone ring");
        }

        if (alwaysMaxHits()) {
            return AttackDist.of(HitDist.single(1.0, dist.maxDamage()));
        }

        return dist;
    }

    private int pickaxeBonus() {
        String w = g.weaponName();
        return switch (w) {
            case "Bronze pickaxe", "Iron pickaxe" -> 1;
            case "Steel pickaxe" -> 6;
            case "Black pickaxe" -> 11;
            case "Mithril pickaxe" -> 21;
            case "Adamant pickaxe" -> 31;
            case "Rune pickaxe", "Gilded pickaxe" -> 41;
            default -> 61;
        };
    }

    private boolean isVampyre() {
        return m.hasAttribute("vampyre");
    }

    private boolean isVampyreTier(int tier) {
        return m.hasAttribute("vampyre" + tier);
    }

    private AttackDist applyVampyreScaling(AttackDist dist) {
        boolean efaritay = g.wearing("Efaritay's aid");
        if (g.wearing("Blisterwood flail")) {
            if (efaritay) {
                dist = dist.scaleDamage(11, 10);
            }
            dist = dist.scaleDamage(5, 4);
            note("Blisterwood flail vs vampyre");
        } else if (g.wearing("Blisterwood sickle")) {
            if (efaritay) {
                dist = dist.scaleDamage(11, 10);
            }
            dist = dist.scaleDamage(23, 20);
            note("Blisterwood sickle vs vampyre");
        } else if (g.wearing("Ivandis flail")) {
            if (efaritay) {
                dist = dist.scaleDamage(11, 10);
            }
            dist = dist.scaleDamage(6, 5);
            note("Ivandis flail vs vampyre");
        } else if (g.wearing("Rod of ivandis") && !isVampyreTier(3)) {
            if (efaritay) {
                dist = dist.scaleDamage(11, 10);
            }
            dist = dist.scaleDamage(11, 10);
            note("Rod of ivandis vs vampyre");
        } else if (g.silverWeapon() && isVampyreTier(1)) {
            if (efaritay) {
                dist = dist.scaleDamage(11, 10);
            }
            dist = dist.scaleDamage(11, 10);
            note("Silver weapon vs vampyre");
        }
        return dist;
    }

    private AttackDist applyBoltEffects(AttackDist dist, double acc, int max, boolean rubyPass) {
        if (p.getAttackType() != AttackType.RANGED || !"Crossbow".equals(g.weaponCategory())) {
            return dist;
        }
        boolean zcb = g.wearing("Zaryte crossbow");
        double procScale = p.isKandarinDiary() ? 1.1 : 1.0;
        int rangedLvl = p.visibleRanged();

        if (rubyPass) {
            if (g.wearing("Ruby bolts (e)", "Ruby dragon bolts (e)")) {
                double chance = 0.06 * procScale;
                int cap = CombatConstants.INFINITE_HEALTH_MONSTERS.contains(m.id)
                        ? (zcb ? 66 : 60) : (zcb ? 110 : 100);
                int effectDmg = Math.min(cap, m.skills.hp * (zcb ? 22 : 20) / 100);
                dist = dist.transform(h -> new HitDist(List.of(
                        new WeightedHit(chance, effectDmg, true),
                        h.withProbability(1 - chance))), true);
                note("Ruby bolts (e)");
            }
            return dist;
        }

        if (g.wearing("Opal bolts (e)", "Opal dragon bolts (e)")) {
            double chance = 0.05 * procScale;
            int bonus = rangedLvl / (zcb ? 9 : 10);
            dist = bonusDamageProc(dist, chance, bonus, false);
            note("Opal bolts (e)");
        } else if (g.wearing("Pearl bolts (e)", "Pearl dragon bolts (e)")) {
            double chance = 0.06 * procScale;
            int divisor = m.hasAttribute("fiery") ? 15 : 20;
            int bonus = rangedLvl / (zcb ? divisor - 2 : divisor);
            dist = bonusDamageProc(dist, chance, bonus, false);
            note("Pearl bolts (e)");
        } else if (g.wearing("Diamond bolts (e)", "Diamond dragon bolts (e)")) {
            double chance = 0.1 * procScale;
            int effectMax = max * (zcb ? 126 : 115) / 100;
            HitDist effect = HitDist.linear(1.0, 0, effectMax);
            dist = dist.transform(h -> new HitDist(concat(
                    effect.scaleProbability(chance).hits(),
                    List.of(h.withProbability(1 - chance)))), true);
            note("Diamond bolts (e)");
        } else if (g.wearing("Dragonstone bolts (e)", "Dragonstone dragon bolts (e)")
                && !m.hasAttribute("fiery") && !m.isDragon()) {
            double chance = 0.06 * procScale;
            int bonus = rangedLvl * 2 / (zcb ? 9 : 10);
            dist = bonusDamageProc(dist, chance, bonus, true);
            note("Dragonstone bolts (e)");
        } else if (g.wearing("Onyx bolts (e)", "Onyx dragon bolts (e)") && !isUndead()) {
            double chance = 0.11 * procScale;
            int effectMax = max * (zcb ? 132 : 120) / 100;
            HitDist effect = HitDist.linear(1.0, 0, effectMax);
            dist = dist.transform(h -> new HitDist(concat(
                    effect.scaleProbability(chance).hits(),
                    List.of(h.withProbability(1 - chance)))), false);
            note("Onyx bolts (e)");
        }
        return dist;
    }

    private AttackDist bonusDamageProc(AttackDist dist, double chance, int bonusDmg,
                                       boolean accurateOnly) {
        return dist.transform(h -> new HitDist(List.of(
                new WeightedHit(chance, h.damage() + bonusDmg, h.accurate()),
                h.withProbability(1 - chance))), !accurateOnly);
    }

    // -------------------------------------------------------- NPC transforms

    AttackDist applyNpcTransforms(AttackDist dist) {
        AttackType style = p.getAttackType();
        if (isImmune()) {
            note("Target is immune to this attack style/weapon");
            return AttackDist.of(new HitDist(List.of(WeightedHit.inaccurate(1.0))));
        }

        if ("Zulrah".equals(m.name)) {
            dist = dist.transform(Transforms.cappedReroll(50, 5, 45), true);
            note("Zulrah: hits above 50 rerolled to 45-50");
        }
        if ("Fragment of Seren".equals(m.name)) {
            dist = dist.transform(Transforms.linearMin(2, 22), true);
        }
        if (("Kraken".equals(m.name) || "Cave kraken".equals(m.name))
                && style == AttackType.RANGED) {
            dist = dist.transform(Transforms.division(7, 1), true);
            note("Kraken: ranged damage divided by 7");
        }
        if (CombatConstants.VERZIK_P1_IDS.contains(m.id) && !g.wearing("Dawnbringer")) {
            int limit = isMelee() ? 10 : 3;
            dist = dist.transform(Transforms.linearMin(limit, 0), true);
            note("Verzik P1 damage cap without Dawnbringer");
        }
        if (CombatConstants.TEKTON_IDS.contains(m.id) && style == AttackType.MAGIC) {
            dist = dist.transform(Transforms.division(5, 1), true);
            note("Tekton: magic damage divided by 5");
        }
        if (CombatConstants.GLOWING_CRYSTAL_IDS.contains(m.id) && style == AttackType.MAGIC) {
            dist = dist.transform(Transforms.division(3, 0), true);
        }
        if ((CombatConstants.OLM_MELEE_HAND_IDS.contains(m.id)
                || CombatConstants.OLM_HEAD_IDS.contains(m.id)) && style == AttackType.MAGIC) {
            dist = dist.transform(Transforms.division(3, 0), true);
            note("Olm: magic damage divided by 3");
        }
        if ((CombatConstants.OLM_MAGE_HAND_IDS.contains(m.id)
                || CombatConstants.OLM_MELEE_HAND_IDS.contains(m.id)) && style == AttackType.RANGED) {
            dist = dist.transform(Transforms.division(3, 0), true);
            note("Olm: ranged damage divided by 3");
        }
        if (CombatConstants.ICE_DEMON_IDS.contains(m.id)
                && !"fire".equals(spellElement())
                && !(p.getSpell() != null && p.getSpell().isDemonbane())) {
            dist = dist.transform(Transforms.division(3, 0), true);
            note("Ice demon: non-fire damage divided by 3");
        }
        if ("Slagilith".equals(m.name) && !g.pickaxe()) {
            dist = dist.transform(Transforms.division(3, 0), true);
        }
        if (CombatConstants.NIGHTMARE_TOTEM_IDS.contains(m.id) && style == AttackType.MAGIC) {
            dist = dist.transform(Transforms.multiply(2, 1, 0), true);
            note("Nightmare totem: double magic damage");
        }
        if (m.name != null && List.of("Slash Bash", "Zogre", "Skogre").contains(m.name)) {
            if (p.getSpell() != null && "Crumble Undead".equals(p.getSpell().name)) {
                dist = dist.transform(Transforms.division(2, 0), true);
            } else if (style != AttackType.RANGED
                    || !g.ammoName().contains(" brutal")
                    || !"Comp ogre bow".equals(g.weaponName())) {
                dist = dist.transform(Transforms.division(4, 0), true);
            }
            note("Zogre damage reduction");
        }
        if (isVampyreTier(2)) {
            if (!g.vampyrebane(true) && g.wearing("Efaritay's aid")) {
                dist = dist.transform(Transforms.division(2, 0), true);
                note("Vampyre T2: half damage with Efaritay's aid");
            } else if (g.silverWeapon()) {
                dist = dist.transform(Transforms.flatLimit(10, 0), true);
                note("Vampyre T2: damage capped at 10 with silver weapon");
            }
        }

        int flatArmour = m.defensive.flatArmour;
        if (flatArmour > 0 && style != AttackType.MAGIC) {
            dist = dist.transform(Transforms.add(-flatArmour, 0), false);
            note("Flat armour " + flatArmour);
        }
        return dist;
    }

    boolean isImmune() {
        AttackType style = p.getAttackType();

        if (CombatConstants.IMMUNE_TO_MAGIC_IDS.contains(m.id) && style == AttackType.MAGIC) {
            return true;
        }
        if (CombatConstants.IMMUNE_TO_RANGED_IDS.contains(m.id) && style == AttackType.RANGED) {
            return true;
        }
        if (CombatConstants.IMMUNE_TO_MELEE_IDS.contains(m.id) && isMelee()) {
            // Zulrah can technically be reached with a polearm
            if (!(CombatConstants.ZULRAH_IDS.contains(m.id) && g.polearm())) {
                return true;
            }
        }
        if (m.hasAttribute("flying") && isMelee()) {
            if (CombatConstants.VESPULA_IDS.contains(m.id)) {
                return true;
            }
            if (!g.polearm() && !g.salamander()) {
                return true;
            }
        }
        if (CombatConstants.IMMUNE_TO_NON_SALAMANDER_MELEE_IDS.contains(m.id)
                && isMelee() && !g.salamander()) {
            return true;
        }
        if (isVampyreTier(3) && !g.vampyrebane(false)) {
            return true;
        }
        if (isVampyreTier(2) && !g.vampyrebane(true) && !g.wearing("Efaritay's aid")
                && !g.silverWeapon()) {
            return true;
        }
        if (CombatConstants.GUARDIAN_IDS.contains(m.id) && (!isMelee() || !g.pickaxe())) {
            return true;
        }
        if (m.isLeafy() && !g.leafBladedWeapon()) {
            return true;
        }
        return !m.hasAttribute("rat") && g.ratBoneWeapon();
    }

    // -------------------------------------------------------------------- TTK

    /** Expected attacks to kill via dynamic programming over the total damage histogram. */
    private double expectedHitsToKill(AttackDist dist) {
        Map<Integer, Double> hist = dist.totalDamageHistogram();
        int startHp = Math.max(1, m.skills.hp);
        double zeroProb = hist.getOrDefault(0, 0.0);
        if (1 - zeroProb < 1e-9) {
            return 0;
        }
        int maxDmg = dist.maxDamage();
        double[] htk = new double[startHp + 1];
        for (int hp = 1; hp <= startHp; hp++) {
            double val = 1.0;
            for (int dmg = 1; dmg <= Math.min(hp, maxDmg); dmg++) {
                Double prob = hist.get(dmg);
                if (prob != null) {
                    val += prob * htk[hp - dmg];
                }
            }
            htk[hp] = val / (1 - zeroProb);
        }
        return htk[startHp];
    }

    // ---------------------------------------------------------------- helpers

    private static List<WeightedHit> concat(List<WeightedHit> a, List<WeightedHit> b) {
        List<WeightedHit> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    private static boolean containsAny(String s, String... fragments) {
        for (String fragment : fragments) {
            if (s.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
