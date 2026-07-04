package com.osrs.dps.calc;

import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerSetup;

import java.util.List;

/**
 * Name-based detection of gear with special effects. Matches the wiki DPS tool's
 * canonical item names (the bundled data uses the same names).
 */
public final class Gear {

    private final PlayerSetup p;

    public Gear(PlayerSetup setup) {
        this.p = setup;
    }

    private String nameAt(EquipmentSlot slot) {
        EquipmentItem item = p.getEquipped(slot);
        return item == null ? "" : item.name;
    }

    /** True if any equipped item's name equals one of the given names. */
    public boolean wearing(String... names) {
        for (EquipmentItem item : p.getEquipment().values()) {
            for (String name : names) {
                if (item.name.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** True if all of the given item names are equipped. */
    public boolean wearingAll(String... names) {
        outer:
        for (String name : names) {
            for (EquipmentItem item : p.getEquipment().values()) {
                if (item.name.equals(name)) {
                    continue outer;
                }
            }
            return false;
        }
        return true;
    }

    /** True if any equipped item name contains the given fragment. */
    public boolean wearingContains(String fragment) {
        return p.getEquipment().values().stream().anyMatch(i -> i.name.contains(fragment));
    }

    public String weaponName() {
        return nameAt(EquipmentSlot.WEAPON);
    }

    public String weaponCategory() {
        EquipmentItem weapon = p.getWeapon();
        return weapon == null || weapon.category == null ? "" : weapon.category;
    }

    public String ammoName() {
        return nameAt(EquipmentSlot.AMMO);
    }

    private boolean isMelee() {
        return p.getAttackType().isMelee();
    }

    // --- Void ---

    public boolean voidRobes() {
        return wearing("Void knight top", "Void knight top (or)", "Elite void top", "Elite void top (or)")
                && wearing("Void knight robe", "Void knight robe (or)", "Elite void robe", "Elite void robe (or)")
                && wearing("Void knight gloves", "Void knight gloves (or)");
    }

    public boolean eliteVoidRobes() {
        return wearing("Elite void top", "Elite void top (or)")
                && wearing("Elite void robe", "Elite void robe (or)")
                && wearing("Void knight gloves", "Void knight gloves (or)");
    }

    public boolean meleeVoid() {
        return voidRobes() && wearing("Void melee helm", "Void melee helm (or)");
    }

    public boolean rangedVoid() {
        return voidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
    }

    public boolean eliteRangedVoid() {
        return eliteVoidRobes() && wearing("Void ranger helm", "Void ranger helm (or)");
    }

    public boolean magicVoid() {
        return voidRobes() && wearing("Void mage helm", "Void mage helm (or)");
    }

    public boolean eliteMagicVoid() {
        return eliteVoidRobes() && wearing("Void mage helm", "Void mage helm (or)");
    }

    // --- Slayer / salve ---

    public boolean imbuedBlackMask() {
        String head = nameAt(EquipmentSlot.HEAD);
        return head.startsWith("Black mask (i)") || head.startsWith("Slayer helmet (i)");
    }

    public boolean blackMask() {
        String head = nameAt(EquipmentSlot.HEAD);
        return imbuedBlackMask() || head.startsWith("Black mask") || head.startsWith("Slayer helmet");
    }

    public boolean salve() {
        return "Salve amulet".equals(nameAt(EquipmentSlot.NECK));
    }

    public boolean salveE() {
        return "Salve amulet (e)".equals(nameAt(EquipmentSlot.NECK));
    }

    public boolean salveI() {
        return "Salve amulet(i)".equals(nameAt(EquipmentSlot.NECK));
    }

    public boolean salveEi() {
        return "Salve amulet(ei)".equals(nameAt(EquipmentSlot.NECK));
    }

    // --- Weapons ---

    public boolean fang() {
        return wearing("Osmumten's fang", "Osmumten's fang (or)");
    }

    public boolean scythe() {
        return weaponName().contains("of vitur");
    }

    public boolean twistedBow() {
        return wearing("Twisted bow");
    }

    public boolean tumekensShadow() {
        return "Tumeken's shadow".equals(weaponName());
    }

    public boolean crystalBow() {
        return wearing("Crystal bow") || weaponName().contains("Bow of faerdhinen");
    }

    public boolean blowpipe() {
        return wearing("Toxic blowpipe", "Blazing blowpipe");
    }

    public boolean keris() {
        return weaponName().contains("Keris");
    }

    public boolean tzhaarWeapon() {
        return wearing("Tzhaar-ket-em", "Tzhaar-ket-om", "Tzhaar-ket-om (t)",
                "Toktz-xil-ak", "Toktz-xil-ek", "Toktz-mej-tal");
    }

    public boolean obsidianArmour() {
        return wearingAll("Obsidian helmet", "Obsidian platelegs", "Obsidian platebody");
    }

    public boolean berserkerNecklace() {
        return wearing("Berserker necklace", "Berserker necklace (or)");
    }

    public boolean ratBoneWeapon() {
        return wearing("Bone mace", "Bone shortbow", "Bone staff");
    }

    public boolean smokeStaff() {
        return wearing("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
    }

    public boolean silverWeapon() {
        if (ammoName().startsWith("Silver bolts") && p.getAttackType() == AttackType.RANGED) {
            return true;
        }
        return isMelee() && wearing("Blessed axe", "Ivandis flail", "Blisterwood flail",
                "Silver sickle", "Silver sickle (b)", "Emerald sickle", "Emerald sickle (b)",
                "Enchanted emerald sickle (b)", "Ruby sickle (b)", "Enchanted ruby sickle (b)",
                "Blisterwood sickle", "Silverlight", "Darklight", "Arclight", "Rod of ivandis",
                "Wolfbane");
    }

    /** Weapons that pierce vampyre tier 2 (and 3 minus rod) damage caps. */
    public boolean vampyrebane(boolean tier2) {
        if (!tier2 && !isMelee()) {
            return false;
        }
        return (tier2 && wearing("Rod of ivandis"))
                || wearing("Ivandis flail", "Blisterwood sickle", "Blisterwood flail",
                        "Hallowed flail", "Sunspear");
    }

    public boolean leafBladedWeapon() {
        if (isMelee() && wearing("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword")) {
            return true;
        }
        if (p.getSpell() != null && "Magic Dart".equals(p.getSpell().name)) {
            return true;
        }
        return p.getAttackType() == AttackType.RANGED
                && wearing("Broad arrows", "Broad bolts", "Amethyst broad bolts");
    }

    /** Weapons that deal full damage to the Corporeal Beast. */
    public boolean corpbaneWeapon() {
        String weapon = weaponName();
        boolean stab = p.getAttackType() == AttackType.STAB;
        if (fang()) {
            return stab;
        }
        if (weapon.endsWith("halberd")) {
            return stab;
        }
        if (weapon.contains("spear") && !"Blue moon spear".equals(weapon)) {
            return stab;
        }
        return false;
    }

    /** Revenant (wilderness) weapon boost, requires target in the Wilderness. */
    public boolean revWeaponBuffApplicable() {
        if (!p.isInWilderness()) {
            return false;
        }
        return switch (p.getAttackType()) {
            case MAGIC -> wearing("Accursed sceptre", "Accursed sceptre (a)",
                    "Thammaron's sceptre", "Thammaron's sceptre (a)");
            case RANGED -> wearing("Craw's bow", "Webweaver bow");
            default -> wearing("Ursine chainmace", "Viggora's chainmace");
        };
    }

    public boolean chargeSpellApplicable() {
        if (!p.isChargeSpell() || p.getSpell() == null) {
            return false;
        }
        return switch (p.getSpell().name) {
            case "Saradomin Strike" -> wearing("Saradomin cape", "Imbued saradomin cape",
                    "Saradomin max cape", "Imbued saradomin max cape");
            case "Claws of Guthix" -> wearing("Guthix cape", "Imbued guthix cape",
                    "Guthix max cape", "Imbued guthix max cape");
            case "Flames of Zamorak" -> wearing("Zamorak cape", "Imbued zamorak cape",
                    "Zamorak max cape", "Imbued zamorak max cape");
            default -> false;
        };
    }

    // --- Sets ---

    public boolean dharokSet() {
        return wearingAll("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
    }

    public boolean veracSet() {
        return wearingAll("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");
    }

    public boolean karilSet() {
        return wearingAll("Karil's coif", "Karil's leathertop", "Karil's leatherskirt",
                "Karil's crossbow", "Amulet of the damned");
    }

    public boolean ahrimSet() {
        return wearingAll("Ahrim's staff", "Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt",
                "Amulet of the damned");
    }

    public int inquisitorPieces() {
        int pieces = 0;
        if (nameAt(EquipmentSlot.HEAD).startsWith("Inquisitor's great helm")) {
            pieces++;
        }
        if (nameAt(EquipmentSlot.BODY).startsWith("Inquisitor's hauberk")) {
            pieces++;
        }
        if (nameAt(EquipmentSlot.LEGS).startsWith("Inquisitor's plateskirt")) {
            pieces++;
        }
        return pieces;
    }

    public boolean inquisitorsMace() {
        return weaponName().startsWith("Inquisitor's mace");
    }

    /** Crystal armour pieces weighted as the game does: helm 1, legs 2, body 3. */
    public int crystalArmourWeight() {
        int weight = 0;
        if (nameAt(EquipmentSlot.HEAD).startsWith("Crystal helm")) {
            weight += 1;
        }
        if (nameAt(EquipmentSlot.LEGS).startsWith("Crystal legs")) {
            weight += 2;
        }
        if (nameAt(EquipmentSlot.BODY).startsWith("Crystal body")) {
            weight += 3;
        }
        return weight;
    }

    /** Virtus robe pieces (magic damage bonus with ancient spells). */
    public int virtusPieces() {
        int pieces = 0;
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.BODY, EquipmentSlot.LEGS)) {
            if (nameAt(slot).contains("Virtus")) {
                pieces++;
            }
        }
        return pieces;
    }

    /** Powered staff category (built-in spell). */
    public boolean poweredStaff() {
        return "Powered Staff".equals(weaponCategory());
    }

    public boolean salamander() {
        return "Salamander".equals(weaponCategory());
    }

    public boolean chinchompa() {
        return "Chinchompas".equals(weaponCategory());
    }

    public boolean pickaxe() {
        return "Pickaxe".equals(weaponCategory());
    }

    public boolean polearm() {
        return "Polearm".equals(weaponCategory());
    }
}
