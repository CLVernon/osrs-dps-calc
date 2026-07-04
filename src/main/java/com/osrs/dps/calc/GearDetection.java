package com.osrs.dps.calc;

import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerSetup;

import java.util.Locale;

/** Name-based detection of gear with special effects. */
final class GearDetection {

    private GearDetection() {
    }

    static String itemName(PlayerSetup setup, EquipmentSlot slot) {
        EquipmentItem item = setup.getEquipped(slot);
        return item == null ? "" : item.name.toLowerCase(Locale.ROOT);
    }

    static String weaponName(PlayerSetup setup) {
        return itemName(setup, EquipmentSlot.WEAPON);
    }

    static String weaponCategory(PlayerSetup setup) {
        EquipmentItem weapon = setup.getWeapon();
        return weapon == null || weapon.category == null ? "" : weapon.category;
    }

    static boolean wearing(PlayerSetup setup, EquipmentSlot slot, String namePart) {
        return itemName(setup, slot).contains(namePart);
    }

    // --- Void ---

    private static boolean hasVoidBody(PlayerSetup setup) {
        String body = itemName(setup, EquipmentSlot.BODY);
        return body.contains("void knight top") || body.contains("elite void top");
    }

    private static boolean hasVoidLegs(PlayerSetup setup) {
        String legs = itemName(setup, EquipmentSlot.LEGS);
        return legs.contains("void knight robe") || legs.contains("elite void robe");
    }

    private static boolean hasVoidGloves(PlayerSetup setup) {
        return wearing(setup, EquipmentSlot.HANDS, "void knight gloves");
    }

    private static boolean hasVoidSet(PlayerSetup setup, String helmName) {
        return wearing(setup, EquipmentSlot.HEAD, helmName)
                && hasVoidBody(setup) && hasVoidLegs(setup) && hasVoidGloves(setup);
    }

    private static boolean hasEliteVoid(PlayerSetup setup) {
        return itemName(setup, EquipmentSlot.BODY).contains("elite void top")
                && itemName(setup, EquipmentSlot.LEGS).contains("elite void robe");
    }

    static boolean meleeVoid(PlayerSetup setup) {
        return hasVoidSet(setup, "void melee helm");
    }

    static boolean rangedVoid(PlayerSetup setup) {
        return hasVoidSet(setup, "void ranger helm");
    }

    static boolean rangedEliteVoid(PlayerSetup setup) {
        return rangedVoid(setup) && hasEliteVoid(setup);
    }

    static boolean magicVoid(PlayerSetup setup) {
        return hasVoidSet(setup, "void mage helm");
    }

    static boolean magicEliteVoid(PlayerSetup setup) {
        return magicVoid(setup) && hasEliteVoid(setup);
    }

    // --- Salve / slayer helm ---

    enum SalveVariant { NONE, REGULAR, ENCHANTED, IMBUED, ENCHANTED_IMBUED }

    static SalveVariant salve(PlayerSetup setup) {
        String neck = itemName(setup, EquipmentSlot.NECK).replace(" ", "");
        if (!neck.contains("salveamulet")) {
            return SalveVariant.NONE;
        }
        boolean enchanted = neck.contains("(e)") || neck.contains("(ei)");
        boolean imbued = neck.contains("(i)") || neck.contains("(ei)");
        if (enchanted && imbued) {
            return SalveVariant.ENCHANTED_IMBUED;
        }
        if (enchanted) {
            return SalveVariant.ENCHANTED;
        }
        if (imbued) {
            return SalveVariant.IMBUED;
        }
        return SalveVariant.REGULAR;
    }

    static boolean slayerHelmOrBlackMask(PlayerSetup setup) {
        String head = itemName(setup, EquipmentSlot.HEAD);
        return head.contains("slayer helmet") || head.contains("black mask");
    }

    static boolean slayerHelmImbued(PlayerSetup setup) {
        String head = itemName(setup, EquipmentSlot.HEAD);
        return (head.contains("slayer helmet") || head.contains("black mask")) && head.contains("(i)");
    }

    // --- Special weapons ---

    static boolean twistedBow(PlayerSetup setup) {
        return weaponName(setup).contains("twisted bow");
    }

    static boolean tumekensShadow(PlayerSetup setup) {
        return weaponName(setup).contains("tumeken's shadow");
    }

    static boolean osmumtensFang(PlayerSetup setup) {
        return weaponName(setup).contains("osmumten's fang");
    }

    static boolean scytheOfVitur(PlayerSetup setup) {
        return weaponName(setup).contains("scythe of vitur");
    }

    static boolean dragonHunterLance(PlayerSetup setup) {
        return weaponName(setup).contains("dragon hunter lance");
    }

    static boolean dragonHunterCrossbow(PlayerSetup setup) {
        return weaponName(setup).contains("dragon hunter crossbow");
    }

    static boolean arclightOrEmberlight(PlayerSetup setup) {
        String w = weaponName(setup);
        return w.contains("arclight") || w.contains("emberlight");
    }

    static boolean silverlightOrDarklight(PlayerSetup setup) {
        String w = weaponName(setup);
        return w.contains("silverlight") || w.contains("darklight");
    }

    static boolean keris(PlayerSetup setup) {
        return weaponName(setup).contains("keris");
    }

    static boolean leafBladed(PlayerSetup setup) {
        return weaponName(setup).contains("leaf-bladed");
    }

    static boolean crystalBow(PlayerSetup setup) {
        String w = weaponName(setup);
        return w.contains("crystal bow") || w.contains("bow of faerdhinen");
    }

    /** Crystal armour damage bonus in percent (helm 2.5, body 7.5, legs 5), ×10 to stay integral. */
    static int crystalArmourDamageTenths(PlayerSetup setup) {
        int tenths = 0;
        if (itemName(setup, EquipmentSlot.HEAD).contains("crystal helm")) {
            tenths += 25;
        }
        if (itemName(setup, EquipmentSlot.BODY).contains("crystal body")) {
            tenths += 75;
        }
        if (itemName(setup, EquipmentSlot.LEGS).contains("crystal legs")) {
            tenths += 50;
        }
        return tenths;
    }

    /** Crystal armour accuracy bonus in tenths of a percent (helm 5%, body 15%, legs 10%). */
    static int crystalArmourAccuracyTenths(PlayerSetup setup) {
        int tenths = 0;
        if (itemName(setup, EquipmentSlot.HEAD).contains("crystal helm")) {
            tenths += 50;
        }
        if (itemName(setup, EquipmentSlot.BODY).contains("crystal body")) {
            tenths += 150;
        }
        if (itemName(setup, EquipmentSlot.LEGS).contains("crystal legs")) {
            tenths += 100;
        }
        return tenths;
    }

    /** Number of Inquisitor's pieces worn (crush bonus). */
    static int inquisitorPieces(PlayerSetup setup) {
        int pieces = 0;
        if (itemName(setup, EquipmentSlot.HEAD).contains("inquisitor's great helm")) {
            pieces++;
        }
        if (itemName(setup, EquipmentSlot.BODY).contains("inquisitor's hauberk")) {
            pieces++;
        }
        if (itemName(setup, EquipmentSlot.LEGS).contains("inquisitor's plateskirt")) {
            pieces++;
        }
        return pieces;
    }

    // --- Powered staves ---

    static boolean isPoweredStaff(PlayerSetup setup) {
        return "Powered Staff".equals(weaponCategory(setup));
    }

    /**
     * Built-in max hit of a powered staff at the given visible magic level,
     * or -1 for unknown powered staves.
     */
    static int poweredStaffMaxHit(PlayerSetup setup, int visibleMagic) {
        String w = weaponName(setup);
        if (w.contains("tumeken's shadow")) {
            return visibleMagic / 3 + 1;
        }
        if (w.contains("sanguinesti staff") || w.contains("holy sanguinesti")) {
            return visibleMagic / 3 - 1;
        }
        if (w.contains("trident of the swamp")) {
            return visibleMagic / 3 - 2;
        }
        if (w.contains("trident of the seas")) {
            return visibleMagic / 3 - 5;
        }
        if (w.contains("accursed sceptre")) {
            return visibleMagic / 3 - 1;
        }
        if (w.contains("thammaron's sceptre")) {
            return visibleMagic / 3 - 8;
        }
        if (w.contains("warped sceptre")) {
            return (visibleMagic * 8 + 96) / 37;
        }
        if (w.contains("crystal staff") || w.contains("corrupted staff")) {
            // Gauntlet staves: basic 23, attuned 31, perfected 39 at tier
            if (w.contains("basic")) {
                return 23;
            }
            if (w.contains("attuned")) {
                return 31;
            }
            if (w.contains("perfected")) {
                return 39;
            }
        }
        if (w.contains("bone staff")) {
            return visibleMagic / 3 + 5;
        }
        if (w.contains("starter staff")) {
            return visibleMagic / 3 - 8;
        }
        return -1;
    }
}
