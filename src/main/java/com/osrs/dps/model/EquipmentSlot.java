package com.osrs.dps.model;

/** Equipment slots as named in the wiki equipment data. */
public enum EquipmentSlot {
    HEAD("head", "Head"),
    CAPE("cape", "Cape"),
    NECK("neck", "Neck"),
    AMMO("ammo", "Ammo"),
    WEAPON("weapon", "Weapon"),
    SHIELD("shield", "Shield"),
    BODY("body", "Body"),
    LEGS("legs", "Legs"),
    HANDS("hands", "Hands"),
    FEET("feet", "Feet"),
    RING("ring", "Ring");

    private final String jsonName;
    private final String displayName;

    EquipmentSlot(String jsonName, String displayName) {
        this.jsonName = jsonName;
        this.displayName = displayName;
    }

    public String jsonName() {
        return jsonName;
    }

    public String displayName() {
        return displayName;
    }

    public static EquipmentSlot fromJsonName(String name) {
        for (EquipmentSlot slot : values()) {
            if (slot.jsonName.equals(name)) {
                return slot;
            }
        }
        return null;
    }
}
