package com.osrs.dps.preset;

import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.AttackType;
import com.osrs.dps.model.EquipmentSlot;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerSetup;
import com.osrs.dps.model.Potion;
import com.osrs.dps.model.Prayer;
import com.osrs.dps.model.Stance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void playerPresetRoundTrip() throws IOException {
        DataRepository data = DataRepository.get();
        PresetManager manager = new PresetManager(tempDir);

        PlayerSetup setup = new PlayerSetup("Max melee");
        setup.setAttackLevel(99);
        setup.setStrengthLevel(99);
        setup.setEquipped(EquipmentSlot.WEAPON, data.findEquipment("Abyssal whip"));
        setup.setEquipped(EquipmentSlot.NECK, data.findEquipment("Amulet of fury"));
        setup.setAttackType(AttackType.SLASH);
        setup.setStance(Stance.AGGRESSIVE);
        setup.setPrayer(Prayer.PIETY);
        setup.setPotion(Potion.SUPER_COMBAT);
        setup.setOnSlayerTask(true);

        setup.setStyleName("Lash");
        setup.setStance(Stance.CONTROLLED);
        manager.savePlayerPreset(setup);
        List<PlayerSetup> loaded = manager.loadPlayerPresets(data);

        assertEquals(1, loaded.size());
        PlayerSetup restored = loaded.get(0);
        assertEquals("Max melee", restored.getName());
        assertEquals("Abyssal whip", restored.getWeapon().name);
        assertEquals(Prayer.PIETY, restored.getPrayer());
        assertEquals(Potion.SUPER_COMBAT, restored.getPotion());
        assertEquals("Lash", restored.getStyleName());
        assertEquals(Stance.CONTROLLED, restored.getStance());
        assertTrue(restored.isOnSlayerTask());
    }

    @Test
    void monsterPresetRoundTrip() throws IOException {
        PresetManager manager = new PresetManager(tempDir);

        Monster monster = new Monster();
        monster.name = "Custom boss";
        monster.skills.def = 250;
        monster.skills.hp = 800;
        monster.defensive.slash = 120;
        monster.attributes = List.of("undead");

        manager.saveMonsterPreset(monster);
        List<Monster> loaded = manager.loadMonsterPresets();

        assertEquals(1, loaded.size());
        assertEquals("Custom boss", loaded.get(0).name);
        assertEquals(250, loaded.get(0).skills.def);
        assertTrue(loaded.get(0).isUndead());
    }

    @Test
    void characterRoundTrip() {
        PresetManager manager = new PresetManager(tempDir);
        com.osrs.dps.model.PlayerCharacter character = new com.osrs.dps.model.PlayerCharacter();
        character.name = "Test Char";
        character.attack = 75;
        character.magic = 94;
        character.currentHitpoints = 50;
        manager.saveCharacter(character);

        com.osrs.dps.model.PlayerCharacter loaded = manager.loadCharacter();
        assertEquals("Test Char", loaded.name);
        assertEquals(75, loaded.attack);
        assertEquals(94, loaded.magic);
        assertEquals(50, loaded.currentHitpoints);
    }

    @Test
    void sharedCharacterAffectsAllSetups() {
        com.osrs.dps.model.PlayerCharacter character = new com.osrs.dps.model.PlayerCharacter();
        PlayerSetup a = new PlayerSetup("A");
        a.setCharacter(character);
        PlayerSetup b = a.copy();
        character.strength = 80;
        assertEquals(80, a.getStrengthLevel());
        assertEquals(80, b.getStrengthLevel(), "copies share the same character");
    }

    @Test
    void deleteRemovesPreset() throws IOException {
        DataRepository data = DataRepository.get();
        PresetManager manager = new PresetManager(tempDir);
        manager.savePlayerPreset(new PlayerSetup("Temp"));
        assertEquals(1, manager.listPlayerPresetNames().size());
        manager.deletePlayerPreset("Temp");
        assertEquals(0, manager.listPlayerPresetNames().size());
        assertEquals(0, manager.loadPlayerPresets(data).size());
    }
}
