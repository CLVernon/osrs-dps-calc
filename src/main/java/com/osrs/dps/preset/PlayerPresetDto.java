package com.osrs.dps.preset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import com.osrs.dps.model.Stance;

import java.util.HashMap;
import java.util.Map;

/** JSON-friendly form of a player setup; equipment is stored by display name. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerPresetDto {

    public String name;
    public Map<String, String> equipment = new HashMap<>();
    public String attackType;
    public String stance;
    public String styleName;
    public String prayer;
    public String potion;
    public String spell;
    public boolean onSlayerTask;
    public boolean inWilderness;
    public boolean forinthrySurge;
    public boolean markOfDarkness;
    public boolean chargeSpell;
    public boolean kandarinDiary;
    public boolean sunfireRunes;
    public int chinchompaDistance = 5;

    public static PlayerPresetDto from(PlayerSetup setup) {
        PlayerPresetDto dto = new PlayerPresetDto();
        dto.name = setup.getName();
        for (Map.Entry<EquipmentSlot, EquipmentItem> e : setup.getEquipment().entrySet()) {
            dto.equipment.put(e.getKey().jsonName(), e.getValue().displayName());
        }
        dto.attackType = setup.getAttackType().name();
        dto.stance = setup.getStance().name();
        dto.styleName = setup.getStyleName();
        dto.prayer = setup.getPrayer().name();
        dto.potion = setup.getPotion().name();
        dto.spell = setup.getSpell() == null ? null : setup.getSpell().name;
        dto.onSlayerTask = setup.isOnSlayerTask();
        dto.inWilderness = setup.isInWilderness();
        dto.forinthrySurge = setup.isForinthrySurge();
        dto.markOfDarkness = setup.isMarkOfDarkness();
        dto.chargeSpell = setup.isChargeSpell();
        dto.kandarinDiary = setup.isKandarinDiary();
        dto.sunfireRunes = setup.isSunfireRunes();
        dto.chinchompaDistance = setup.getChinchompaDistance();
        return dto;
    }

    public PlayerSetup toSetup(DataRepository data) {
        PlayerSetup setup = new PlayerSetup(name == null ? "Unnamed" : name);
        if (equipment != null) {
            for (Map.Entry<String, String> e : equipment.entrySet()) {
                EquipmentSlot slot = EquipmentSlot.fromJsonName(e.getKey());
                EquipmentItem item = data.findEquipment(e.getValue());
                if (slot != null && item != null) {
                    setup.setEquipped(slot, item);
                }
            }
        }
        setup.setAttackType(parse(AttackType.class, attackType, AttackType.CRUSH));
        setup.setStance(parse(Stance.class, stance, Stance.ACCURATE));
        if (styleName != null) {
            setup.setStyleName(styleName);
        }
        setup.setPrayer(parse(Prayer.class, prayer, Prayer.NONE));
        setup.setPotion(parse(Potion.class, potion, Potion.NONE));
        setup.setSpell(spell == null ? null : data.findSpell(spell));
        setup.setOnSlayerTask(onSlayerTask);
        setup.setInWilderness(inWilderness);
        setup.setForinthrySurge(forinthrySurge);
        setup.setMarkOfDarkness(markOfDarkness);
        setup.setChargeSpell(chargeSpell);
        setup.setKandarinDiary(kandarinDiary);
        setup.setSunfireRunes(sunfireRunes);
        setup.setChinchompaDistance(chinchompaDistance);
        return setup;
    }

    private static <T extends Enum<T>> T parse(Class<T> type, String value, T fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
