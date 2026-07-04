package com.osrs.dps.preset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.osrs.dps.data.DataRepository;
import com.osrs.dps.model.Monster;
import com.osrs.dps.model.PlayerCharacter;
import com.osrs.dps.model.PlayerSetup;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves and loads player/monster presets as JSON files under the user's
 * application data directory (%APPDATA%/osrs-dps-calc/presets).
 */
public class PresetManager {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final Path playerDir;
    private final Path monsterDir;
    private final Path characterFile;

    public PresetManager() {
        this(defaultBaseDir());
    }

    public PresetManager(Path baseDir) {
        this.playerDir = baseDir.resolve("presets").resolve("players");
        this.monsterDir = baseDir.resolve("presets").resolve("monsters");
        this.characterFile = baseDir.resolve("character.json");
        try {
            Files.createDirectories(playerDir);
            Files.createDirectories(monsterDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create preset directories", e);
        }
    }

    // --- Character ---

    /** Saves the shared character; failures are logged, never thrown. */
    public void saveCharacter(PlayerCharacter character) {
        try {
            mapper.writeValue(characterFile.toFile(), character);
        } catch (IOException e) {
            System.err.println("Could not save character: " + e.getMessage());
        }
    }

    /** Loads the saved character, or a fresh maxed character if none exists. */
    public PlayerCharacter loadCharacter() {
        if (Files.isRegularFile(characterFile)) {
            try {
                return mapper.readValue(characterFile.toFile(), PlayerCharacter.class);
            } catch (IOException e) {
                System.err.println("Could not load character: " + e.getMessage());
            }
        }
        return new PlayerCharacter();
    }

    private static Path defaultBaseDir() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("osrs-dps-calc");
    }

    // --- Player presets ---

    public void savePlayerPreset(PlayerSetup setup) throws IOException {
        Path file = playerDir.resolve(sanitize(setup.getName()) + ".json");
        mapper.writeValue(file.toFile(), PlayerPresetDto.from(setup));
    }

    public List<PlayerSetup> loadPlayerPresets(DataRepository data) {
        List<PlayerSetup> setups = new ArrayList<>();
        for (Path file : listJsonFiles(playerDir)) {
            try {
                setups.add(mapper.readValue(file.toFile(), PlayerPresetDto.class).toSetup(data));
            } catch (IOException e) {
                System.err.println("Skipping unreadable player preset " + file + ": " + e.getMessage());
            }
        }
        return setups;
    }

    public void deletePlayerPreset(String name) throws IOException {
        Files.deleteIfExists(playerDir.resolve(sanitize(name) + ".json"));
    }

    public List<String> listPlayerPresetNames() {
        return listJsonFiles(playerDir).stream()
                .map(f -> stripExtension(f.getFileName().toString()))
                .toList();
    }

    // --- Monster presets ---

    public void saveMonsterPreset(Monster monster) throws IOException {
        Path file = monsterDir.resolve(sanitize(monster.displayName()) + ".json");
        mapper.writeValue(file.toFile(), monster);
    }

    public List<Monster> loadMonsterPresets() {
        List<Monster> monsters = new ArrayList<>();
        for (Path file : listJsonFiles(monsterDir)) {
            try {
                monsters.add(mapper.readValue(file.toFile(), Monster.class));
            } catch (IOException e) {
                System.err.println("Skipping unreadable monster preset " + file + ": " + e.getMessage());
            }
        }
        return monsters;
    }

    public void deleteMonsterPreset(String displayName) throws IOException {
        Files.deleteIfExists(monsterDir.resolve(sanitize(displayName) + ".json"));
    }

    // --- Helpers ---

    private static List<Path> listJsonFiles(Path dir) {
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
