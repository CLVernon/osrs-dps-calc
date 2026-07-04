package com.osrs.dps.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.osrs.dps.model.EquipmentItem;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.SpellData;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Loads the wiki equipment, monster and spell data. Prefers the auto-updated
 * cache in the user data directory ({@link DataUpdater}); falls back to the
 * data bundled with the application.
 */
public final class DataRepository {

    private static DataRepository instance;

    private final List<EquipmentItem> equipment;
    private final List<Monster> monsters;
    private final List<SpellData> spells;

    private DataRepository(List<EquipmentItem> equipment, List<Monster> monsters,
                           List<SpellData> spells) {
        this.equipment = equipment;
        this.monsters = monsters;
        this.spells = spells;
    }

    public static synchronized DataRepository get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** Forces a reload (e.g. after the updater has fetched fresh data). */
    public static synchronized void reload() {
        instance = load();
    }

    private static DataRepository load() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream eq = open("equipment.json");
             InputStream mon = open("monsters.json");
             InputStream sp = open("spells.json")) {
            List<EquipmentItem> equipment = mapper.readValue(eq, new TypeReference<List<EquipmentItem>>() {});
            List<Monster> monsters = mapper.readValue(mon, new TypeReference<List<Monster>>() {});
            List<SpellData> spells = mapper.readValue(sp, new TypeReference<List<SpellData>>() {});
            equipment.sort(Comparator.comparing(EquipmentItem::displayName, String.CASE_INSENSITIVE_ORDER));
            monsters.sort(Comparator.comparing(Monster::displayName, String.CASE_INSENSITIVE_ORDER));
            return new DataRepository(equipment, monsters, spells);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load wiki data", e);
        }
    }

    /** Opens a data file: cached copy if present and readable, else the bundled resource. */
    private static InputStream open(String fileName) throws IOException {
        Path cached = DataUpdater.dataDir().resolve(fileName);
        if (Files.isRegularFile(cached) && Files.size(cached) > 0) {
            return Files.newInputStream(cached);
        }
        InputStream in = DataRepository.class.getResourceAsStream("/data/" + fileName);
        if (in == null) {
            throw new IllegalStateException("Missing bundled resource: " + fileName);
        }
        return in;
    }

    public List<EquipmentItem> allEquipment() {
        return equipment;
    }

    public List<Monster> allMonsters() {
        return monsters;
    }

    public List<SpellData> allSpells() {
        return spells;
    }

    public List<SpellData> spellsForSpellbook(String spellbook) {
        return spells.stream().filter(s -> spellbook.equals(s.spellbook)).toList();
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
                .filter(mo -> mo.displayName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .orElse(null);
    }

    /** Find a spell by name (case-insensitive), preferring the standard spellbook. */
    public SpellData findSpell(String name) {
        if (name == null) {
            return null;
        }
        String wanted = name.toLowerCase(Locale.ROOT);
        return spells.stream()
                .filter(s -> s.name != null && s.name.toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst()
                .orElse(null);
    }
}
