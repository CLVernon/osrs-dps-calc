package com.osrs.dps.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Downloads the latest wiki data (equipment/monsters/spells) from the
 * weirdgloop/osrs-dps-calc dataset into the user data directory on startup.
 * The check runs at most once per day; failures fall back to existing data.
 */
public final class DataUpdater {

    private static final String BASE_URL =
            "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/main/cdn/json/";
    private static final List<String> FILES = List.of("equipment.json", "monsters.json", "spells.json");

    private DataUpdater() {
    }

    public static Path baseDir() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null ? Path.of(appData) : Path.of(System.getProperty("user.home"));
        return base.resolve("osrs-dps-calc");
    }

    public static Path dataDir() {
        return baseDir().resolve("data");
    }

    /** True if a data refresh is due (no cache, or last check older than a day). */
    public static boolean updateDue() {
        Path stamp = dataDir().resolve(".last-update");
        if (!Files.isRegularFile(stamp)) {
            return true;
        }
        try {
            Instant last = Instant.parse(Files.readString(stamp).trim());
            return last.isBefore(Instant.now().minus(1, ChronoUnit.DAYS));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Downloads all data files. Files are only replaced once fully downloaded,
     * so a mid-download failure never corrupts the cache.
     *
     * @return true if new data was downloaded
     */
    public static boolean update() {
        try {
            Files.createDirectories(dataDir());
            for (String file : FILES) {
                Path temp = dataDir().resolve(file + ".tmp");
                if (!Downloads.fetchToFile(BASE_URL + file, temp, 60_000)) {
                    Files.deleteIfExists(temp);
                    System.err.println("Data update failed for " + file);
                    return false;
                }
                Files.move(temp, dataDir().resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(dataDir().resolve(".last-update"), Instant.now().toString());
            return true;
        } catch (Exception e) {
            // Never let a network problem break startup - bundled/cached data is used instead.
            System.err.println("Data update failed: " + e.getMessage());
            return false;
        }
    }

    /** Runs the daily update check; returns true if fresh data was fetched. */
    public static boolean updateIfDue() {
        return updateDue() && update();
    }
}
