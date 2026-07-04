package com.osrs.dps.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.Monster;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Loads the bundled wiki equipment and monster data. */
public final class DataRepository {

    private static DataRepository instance;

    private final List<EquipmentItem> equipment;
    private final List<Monster> monsters;

    private DataRepository(List<EquipmentItem> equipment, List<Monster> monsters) {
        this.equipment = equipment;
        this.monsters = monsters;
    }

    public static synchronized DataRepository get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static DataRepository load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream eq = open("/data/equipment.json");
             InputStream mon = open("/data/monsters.json")) {
            List<EquipmentItem> equipment = mapper.readValue(eq, new TypeReference<List<EquipmentItem>>() {});
            List<Monster> monsters = mapper.readValue(mon, new TypeReference<List<Monster>>() {});
            equipment.sort(Comparator.comparing(EquipmentItem::displayName, String.CASE_INSENSITIVE_ORDER));
            monsters.sort(Comparator.comparing(Monster::displayName, String.CASE_INSENSITIVE_ORDER));
            return new DataRepository(equipment, monsters);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load bundled wiki data", e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = DataRepository.class.getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Missing bundled resource: " + resource);
        }
        return in;
    }

    public List<EquipmentItem> allEquipment() {
        return equipment;
    }

    public List<Monster> allMonsters() {
        return monsters;
    }

    public List<EquipmentItem> equipmentForSlot(EquipmentSlot slot) {
        return equipment.stream()
                .filter(item -> slot.jsonName().equals(item.slot))
                .toList();
    }

    /** Find an equipment item by exact display name (case-insensitive). */
    public EquipmentItem findEquipment(String displayName) {
        if (displayName == null) {
            return null;
        }
        String wanted = displayName.toLowerCase(Locale.ROOT);
        return equipment.stream()
                .filter(item -> item.displayName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .orElse(null);
    }

    /** Find a monster by exact display name (case-insensitive). */
    public Monster findMonster(String displayName) {
        if (displayName == null) {
            return null;
        }
        String wanted = displayName.toLowerCase(Locale.ROOT);
        return monsters.stream()
                .filter(m -> m.displayName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .orElse(null);
    }
}
