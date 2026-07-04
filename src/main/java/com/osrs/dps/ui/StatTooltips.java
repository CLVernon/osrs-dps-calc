package com.osrs.dps.ui;

import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.SpellData;

/** Builds hover-tooltip stat summaries for dropdown entries. */
public final class StatTooltips {

    private StatTooltips() {
    }

    public static String forEquipment(EquipmentItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Attack   stab ").append(pad(item.offensive.stab))
                .append("  slash ").append(pad(item.offensive.slash))
                .append("  crush ").append(pad(item.offensive.crush))
                .append("  magic ").append(pad(item.offensive.magic))
                .append("  ranged ").append(pad(item.offensive.ranged));
        sb.append("\nDefence  stab ").append(pad(item.defensive.stab))
                .append("  slash ").append(pad(item.defensive.slash))
                .append("  crush ").append(pad(item.defensive.crush))
                .append("  magic ").append(pad(item.defensive.magic))
                .append("  ranged ").append(pad(item.defensive.ranged));
        sb.append("\nMelee str ").append(pad(item.bonuses.str))
                .append("  Ranged str ").append(pad(item.bonuses.rangedStr))
                .append("  Magic dmg ").append(item.bonuses.magicStr / 10.0).append('%')
                .append("  Prayer ").append(pad(item.bonuses.prayer));
        if ("weapon".equals(item.slot)) {
            sb.append("\nSpeed ").append(item.speed).append(" ticks");
            if (item.category != null && !item.category.isBlank()) {
                sb.append("  |  ").append(item.category);
            }
            if (item.twoHanded) {
                sb.append("  |  two-handed");
            }
        }
        return sb.toString();
    }

    public static String forMonster(Monster m) {
        StringBuilder sb = new StringBuilder();
        sb.append("HP ").append(m.skills.hp)
                .append("  Def ").append(m.skills.def)
                .append("  Magic ").append(m.skills.magic)
                .append("  Size ").append(m.size);
        sb.append("\nDef bonus stab ").append(pad(m.defensive.stab))
                .append("  slash ").append(pad(m.defensive.slash))
                .append("  crush ").append(pad(m.defensive.crush))
                .append("  magic ").append(pad(m.defensive.magic));
        sb.append("\nRanged def light ").append(pad(m.defensive.light))
                .append("  standard ").append(pad(m.defensive.standard))
                .append("  heavy ").append(pad(m.defensive.heavy));
        if (m.defensive.flatArmour > 0) {
            sb.append("\nFlat armour ").append(m.defensive.flatArmour);
        }
        if (m.attributes != null && !m.attributes.isEmpty()) {
            sb.append("\nAttributes: ").append(String.join(", ", m.attributes));
        }
        if (m.weakness != null && m.weakness.element != null
                && !"none".equalsIgnoreCase(m.weakness.element)) {
            sb.append("\nWeak to ").append(m.weakness.element)
                    .append(" (+").append(m.weakness.severity).append("%)");
        }
        return sb.toString();
    }

    public static String forSpell(SpellData spell) {
        StringBuilder sb = new StringBuilder();
        sb.append("Max hit ").append(spell.maxHit);
        if (spell.spellbook != null) {
            sb.append("  |  ").append(spell.spellbook).append(" spellbook");
        }
        if (spell.effectiveElement() != null) {
            sb.append("  |  ").append(spell.effectiveElement()).append(" element");
        }
        return sb.toString();
    }

    private static String pad(int value) {
        return (value > 0 ? "+" : "") + value;
    }
}
