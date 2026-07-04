package com.osrs.dps.calc;

import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Spell;
import com.osrs.dps.model.Stance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DPS engine implementing the OSRS wiki combat formulas
 * (https://oldschool.runescape.wiki/w/Damage_per_second), including the major
 * special-case weapons and set effects.
 */
public final class DpsCalculator {

    private DpsCalculator() {
    }

    public static DpsResult calculate(PlayerSetup player, Monster monster) {
        return switch (player.getAttackType()) {
            case STAB, SLASH, CRUSH -> melee(player, monster);
            case RANGED -> ranged(player, monster);
            case MAGIC -> magic(player, monster);
        };
    }

    // ------------------------------------------------------------------ melee

    private static DpsResult melee(PlayerSetup p, Monster m) {
        List<String> notes = new ArrayList<>();
        AttackType type = p.getAttackType();

        int effAttack = (int) Math.floor(p.visibleAttack() * p.getPrayer().meleeAccuracy())
                + p.getStance().meleeAttackBonus() + 8;
        int effStrength = (int) Math.floor(p.visibleStrength() * p.getPrayer().meleeStrength())
                + p.getStance().meleeStrengthBonus() + 8;
        if (GearDetection.meleeVoid(p)) {
            effAttack = (int) Math.floor(effAttack * 1.1);
            effStrength = (int) Math.floor(effStrength * 1.1);
            notes.add("Void melee: +10% accuracy and strength");
        }

        int maxHit = CombatMath.maxHit(effStrength, p.meleeStrengthBonus());
        long attackRoll = (long) effAttack * (p.attackBonus(type) + 64);

        // Salve takes priority over the slayer helmet; they never stack.
        GearDetection.SalveVariant salve = GearDetection.salve(p);
        if (m.isUndead() && salve != GearDetection.SalveVariant.NONE) {
            boolean enchanted = salve == GearDetection.SalveVariant.ENCHANTED
                    || salve == GearDetection.SalveVariant.ENCHANTED_IMBUED;
            double mult = enchanted ? 1.2 : 7.0 / 6.0;
            attackRoll = (long) Math.floor(attackRoll * mult);
            maxHit = (int) Math.floor(maxHit * mult);
            notes.add("Salve amulet vs undead: " + (enchanted ? "+20%" : "+16.67%"));
        } else if (p.isOnSlayerTask() && GearDetection.slayerHelmOrBlackMask(p)) {
            attackRoll = (long) Math.floor(attackRoll * 7.0 / 6.0);
            maxHit = (int) Math.floor(maxHit * 7.0 / 6.0);
            notes.add("Slayer helmet/black mask on task: +16.67%");
        }

        if (m.isDragon() && GearDetection.dragonHunterLance(p)) {
            attackRoll = (long) Math.floor(attackRoll * 1.2);
            maxHit = (int) Math.floor(maxHit * 1.2);
            notes.add("Dragon hunter lance vs dragon: +20%");
        }
        if (m.isDemon()) {
            if (GearDetection.arclightOrEmberlight(p)) {
                attackRoll = (long) Math.floor(attackRoll * 1.7);
                maxHit = (int) Math.floor(maxHit * 1.7);
                notes.add("Arclight/Emberlight vs demon: +70%");
            } else if (GearDetection.silverlightOrDarklight(p)) {
                maxHit = (int) Math.floor(maxHit * 1.6);
                notes.add("Silverlight/Darklight vs demon: +60% damage");
            }
        }
        if (m.isLeafy() && GearDetection.leafBladed(p)
                && GearDetection.weaponName(p).contains("battleaxe")) {
            maxHit = (int) Math.floor(maxHit * 1.175);
            notes.add("Leaf-bladed battleaxe vs leafy: +17.5% damage");
        }
        if (type == AttackType.CRUSH) {
            int pieces = GearDetection.inquisitorPieces(p);
            if (pieces > 0) {
                double bonus = pieces * 0.005 + (pieces == 3 ? 0.01 : 0);
                attackRoll = (long) Math.floor(attackRoll * (1 + bonus));
                maxHit = (int) Math.floor(maxHit * (1 + bonus));
                notes.add(String.format("Inquisitor's (%d pc): +%.1f%% crush", pieces, bonus * 100));
            }
        }

        int styleDefBonus = switch (type) {
            case STAB -> m.defensive.stab;
            case SLASH -> m.defensive.slash;
            default -> m.defensive.crush;
        };
        long defenceRoll = (long) (m.skills.def + 9) * (styleDefBonus + 64);

        double accuracy = CombatMath.accuracy(attackRoll, defenceRoll);
        double avgDamage;

        if (GearDetection.osmumtensFang(p)) {
            // Fang rolls accuracy twice and damages between 15% and 85% of max.
            accuracy = 1 - (1 - accuracy) * (1 - accuracy);
            int minHit = (int) Math.floor(maxHit * 0.15);
            int effectiveMax = maxHit - minHit;
            avgDamage = accuracy
                    * CombatMath.expectedHitDamage(minHit, effectiveMax, m.defensive.flatArmour);
            maxHit = effectiveMax;
            notes.add("Osmumten's fang: accuracy rerolled, hits 15%-85% of max");
        } else if (GearDetection.scytheOfVitur(p) && m.size >= 2) {
            // Scythe hits up to 3 times vs large targets at 100%/50%/25% strength.
            int hits = Math.min(m.size, 3);
            double total = 0;
            int currentMax = maxHit;
            for (int i = 0; i < hits; i++) {
                total += accuracy
                        * CombatMath.expectedHitDamage(0, currentMax, m.defensive.flatArmour);
                currentMax /= 2;
            }
            avgDamage = total;
            notes.add("Scythe of Vitur vs size " + m.size + ": " + hits + " hits");
        } else {
            avgDamage = accuracy * CombatMath.expectedHitDamage(0, maxHit, m.defensive.flatArmour);
            if (GearDetection.keris(p) && m.isKalphite()) {
                maxHit = maxHit * 4 / 3;
                // 1/51 attacks deal triple damage.
                avgDamage = accuracy
                        * CombatMath.expectedHitDamage(0, maxHit, m.defensive.flatArmour)
                        * (1 + 2.0 / 51.0);
                notes.add("Keris vs kalphite: +33% damage, 1/51 triple hit");
            }
        }

        if (m.defensive.flatArmour > 0) {
            notes.add("Flat armour " + m.defensive.flatArmour + " reduces each hit");
        }

        return result(p, maxHit, accuracy, avgDamage, notes);
    }

    // ----------------------------------------------------------------- ranged

    private static DpsResult ranged(PlayerSetup p, Monster m) {
        List<String> notes = new ArrayList<>();

        int effAttack = (int) Math.floor(p.visibleRanged() * p.getPrayer().rangedAccuracy())
                + p.getStance().rangedAttackBonus() + 8;
        int effStrength = (int) Math.floor(p.visibleRanged() * p.getPrayer().rangedStrength())
                + p.getStance().rangedStrengthBonus() + 8;
        if (GearDetection.rangedVoid(p)) {
            boolean elite = GearDetection.rangedEliteVoid(p);
            effAttack = (int) Math.floor(effAttack * 1.1);
            effStrength = (int) Math.floor(effStrength * (elite ? 1.125 : 1.1));
            notes.add(elite ? "Elite void ranged: +10% accuracy, +12.5% damage"
                            : "Void ranged: +10% accuracy and damage");
        }

        int maxHit = CombatMath.maxHit(effStrength, p.rangedStrengthBonus());
        long attackRoll = (long) effAttack * (p.attackBonus(AttackType.RANGED) + 64);

        GearDetection.SalveVariant salve = GearDetection.salve(p);
        boolean salveImbued = salve == GearDetection.SalveVariant.IMBUED
                || salve == GearDetection.SalveVariant.ENCHANTED_IMBUED;
        if (m.isUndead() && salveImbued) {
            double mult = salve == GearDetection.SalveVariant.ENCHANTED_IMBUED ? 1.2 : 7.0 / 6.0;
            attackRoll = (long) Math.floor(attackRoll * mult);
            maxHit = (int) Math.floor(maxHit * mult);
            notes.add("Salve amulet (i) vs undead");
        } else if (p.isOnSlayerTask() && GearDetection.slayerHelmImbued(p)) {
            attackRoll = (long) Math.floor(attackRoll * 1.15);
            maxHit = (int) Math.floor(maxHit * 1.15);
            notes.add("Slayer helmet (i) on task: +15%");
        }

        if (m.isDragon() && GearDetection.dragonHunterCrossbow(p)) {
            attackRoll = (long) Math.floor(attackRoll * 1.3);
            maxHit = (int) Math.floor(maxHit * 1.25);
            notes.add("Dragon hunter crossbow vs dragon: +30% accuracy, +25% damage");
        }

        if (GearDetection.twistedBow(p)) {
            int magic = Math.max(m.skills.magic, m.offensive.magic);
            magic = Math.min(magic, 250);
            int accPercent = 140 + (3 * magic - 10) / 100
                    - (int) Math.pow(3 * magic / 10 - 100, 2) / 100;
            int dmgPercent = 250 + (3 * magic - 14) / 100
                    - (int) Math.pow(3 * magic / 10 - 140, 2) / 100;
            accPercent = Math.min(accPercent, 140);
            dmgPercent = Math.min(dmgPercent, 250);
            attackRoll = attackRoll * accPercent / 100;
            maxHit = maxHit * dmgPercent / 100;
            notes.add(String.format("Twisted bow vs magic %d: %d%% accuracy, %d%% damage",
                    magic, accPercent, dmgPercent));
        }

        if (GearDetection.crystalBow(p)) {
            int accTenths = GearDetection.crystalArmourAccuracyTenths(p);
            int dmgTenths = GearDetection.crystalArmourDamageTenths(p);
            if (accTenths > 0) {
                attackRoll = (long) Math.floor(attackRoll * (1 + accTenths / 1000.0));
                maxHit = (int) Math.floor(maxHit * (1 + dmgTenths / 1000.0));
                notes.add(String.format("Crystal armour: +%.1f%% accuracy, +%.1f%% damage",
                        accTenths / 10.0, dmgTenths / 10.0));
            }
        }

        // Monsters have separate ranged defences vs heavy/standard/light weapons.
        String category = GearDetection.weaponCategory(p);
        int rangedDefBonus = switch (category) {
            case "Crossbow", "Bulwark" -> m.defensive.heavy;
            case "Thrown", "Blaster" -> m.defensive.light;
            default -> m.defensive.standard;
        };
        long defenceRoll = (long) (m.skills.def + 9) * (rangedDefBonus + 64);

        double accuracy = CombatMath.accuracy(attackRoll, defenceRoll);
        double avgDamage = accuracy * CombatMath.expectedHitDamage(0, maxHit, m.defensive.flatArmour);
        if (m.defensive.flatArmour > 0) {
            notes.add("Flat armour " + m.defensive.flatArmour + " reduces each hit");
        }

        return result(p, maxHit, accuracy, avgDamage, notes);
    }

    // ------------------------------------------------------------------ magic

    private static DpsResult magic(PlayerSetup p, Monster m) {
        List<String> notes = new ArrayList<>();
        int visibleMagic = p.visibleMagic();

        // Base max hit: powered staff built-in spell or an autocast spell.
        int baseMax;
        Spell spell = null;
        if (GearDetection.isPoweredStaff(p) && !p.isCastingSpell()) {
            baseMax = GearDetection.poweredStaffMaxHit(p, visibleMagic);
            if (baseMax < 0) {
                notes.add("Unknown powered staff - using Trident of the seas formula");
                baseMax = visibleMagic / 3 - 5;
            }
        } else if (p.getSpell() != null) {
            spell = p.getSpell();
            baseMax = spell.baseMaxHit(visibleMagic);
        } else {
            notes.add("No spell selected and weapon is not a powered staff");
            return new DpsResult(0, 0, 0, p.attackSpeedTicks(), 0, notes);
        }

        boolean shadow = GearDetection.tumekensShadow(p);
        int magicDamageTenths = p.magicDamageBonusTenths();
        int magicAttackBonus = p.attackBonus(AttackType.MAGIC);
        if (shadow) {
            magicDamageTenths = Math.min(magicDamageTenths * 3, 1000);
            magicAttackBonus = magicAttackBonus * 3;
            notes.add("Tumeken's shadow: equipment magic bonuses tripled (damage capped +100%)");
        }
        double damageBonus = magicDamageTenths / 1000.0;
        if (GearDetection.magicEliteVoid(p)) {
            damageBonus += 0.025;
            notes.add("Elite void magic: +2.5% damage");
        }

        int maxHit = (int) Math.floor(baseMax * (1 + damageBonus));

        int effMagic = (int) Math.floor(visibleMagic * p.getPrayer().magicAccuracy())
                + p.getStance().magicAttackBonus() + 9;
        if (GearDetection.magicVoid(p)) {
            effMagic = (int) Math.floor(effMagic * 1.45);
            notes.add("Void magic: +45% accuracy");
        }
        long attackRoll = (long) effMagic * (magicAttackBonus + 64);

        GearDetection.SalveVariant salve = GearDetection.salve(p);
        boolean salveImbued = salve == GearDetection.SalveVariant.IMBUED
                || salve == GearDetection.SalveVariant.ENCHANTED_IMBUED;
        if (m.isUndead() && salveImbued) {
            double mult = salve == GearDetection.SalveVariant.ENCHANTED_IMBUED ? 1.2 : 7.0 / 6.0;
            attackRoll = (long) Math.floor(attackRoll * mult);
            maxHit = (int) Math.floor(maxHit * mult);
            notes.add("Salve amulet (i) vs undead");
        } else if (p.isOnSlayerTask() && GearDetection.slayerHelmImbued(p)) {
            attackRoll = (long) Math.floor(attackRoll * 1.15);
            maxHit = (int) Math.floor(maxHit * 1.15);
            notes.add("Slayer helmet (i) on task: +15%");
        }

        // Elemental weakness boosts the attack roll of matching spells.
        if (spell != null && spell.element() != null && m.weakness != null
                && spell.element().equalsIgnoreCase(m.weakness.element)) {
            attackRoll = (long) Math.floor(attackRoll * (1 + m.weakness.severity / 100.0));
            notes.add(String.format("%s weakness: +%d%% accuracy",
                    capitalize(m.weakness.element), m.weakness.severity));
        }

        long defenceRoll = (long) (m.skills.magic + 9) * (m.defensive.magic + 64);

        double accuracy = CombatMath.accuracy(attackRoll, defenceRoll);
        double avgDamage = accuracy * CombatMath.expectedHitDamage(0, maxHit, m.defensive.flatArmour);
        if (m.defensive.flatArmour > 0) {
            notes.add("Flat armour " + m.defensive.flatArmour + " reduces each hit");
        }

        return result(p, maxHit, accuracy, avgDamage, notes);
    }

    // ---------------------------------------------------------------- helpers

    private static DpsResult result(PlayerSetup p, int maxHit, double accuracy,
                                    double avgDamage, List<String> notes) {
        int speed = p.attackSpeedTicks();
        double dps = avgDamage / (speed * DpsResult.SECONDS_PER_TICK);
        return new DpsResult(maxHit, accuracy, avgDamage, speed, dps, notes);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
