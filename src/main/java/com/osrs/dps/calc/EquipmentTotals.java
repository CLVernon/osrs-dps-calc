package com.osrs.dps.calc;

import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;

/**
 * Aggregated equipment bonuses with the wiki tool's gear-level adjustments applied
 * (Tumeken's shadow tripling, Dinh's bulwark strength, Virtus + ancients,
 * elite void mage visible bonus, Keris partisan of amascut penalty outside ToA).
 */
final class EquipmentTotals {

    int str;
    int rangedStr;
    /** Magic damage bonus in tenths of a percent. */
    int magicStrTenths;
    int stab;
    int slash;
    int crush;
    int ranged;
    int magic;

    EquipmentTotals(PlayerSetup p, Monster m, Gear g) {
        for (EquipmentItem item : p.getEquipment().values()) {
            str += item.bonuses.str;
            rangedStr += item.bonuses.rangedStr;
            magicStrTenths += item.bonuses.magicStr;
            stab += item.offensive.stab;
            slash += item.offensive.slash;
            crush += item.offensive.crush;
            ranged += item.offensive.ranged;
            magic += item.offensive.magic;
        }

        // The shadow's bonus multiplier does not apply when manually casting a spell
        boolean manualCast = p.getStance() == com.osrs.dps.model.Stance.MANUAL_CAST;
        if (g.tumekensShadow() && !manualCast) {
            int factor = CombatConstants.TOA_MONSTER_IDS.contains(m.id) ? 4 : 3;
            magicStrTenths = Math.min(1000, magicStrTenths * factor);
            magic *= factor;
        }

        if ("Keris partisan of amascut".equals(g.weaponName())
                && !CombatConstants.TOA_MONSTER_IDS.contains(m.id)) {
            str -= 22;
            stab -= 50;
        }

        if (g.wearing("Dinh's bulwark", "Dinh's blazing bulwark")) {
            int defenceSum = 0;
            for (EquipmentItem item : p.getEquipment().values()) {
                defenceSum += item.defensive.stab + item.defensive.slash
                        + item.defensive.crush + item.defensive.ranged;
            }
            str += Math.max(0, (defenceSum - 800) / 12 - 38);
        }

        if (p.getSpell() != null && "ancient".equals(p.getSpell().spellbook)
                && p.isCastingSpell()) {
            magicStrTenths += 30 * g.virtusPieces();
        }

        if (g.eliteMagicVoid()) {
            magicStrTenths += 50;
        }
    }

    int attackBonus(AttackType type) {
        return switch (type) {
            case STAB -> stab;
            case SLASH -> slash;
            case CRUSH -> crush;
            case RANGED -> ranged;
            case MAGIC -> magic;
        };
    }

    /** Base max hit formula from effective level and a strength-style bonus. */
    static int maxHitFromEffective(int effectiveLevel, int strengthBonus) {
        return (effectiveLevel * (strengthBonus + 64) + 320) / 640;
    }
}
