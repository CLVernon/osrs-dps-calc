package com.osrs.dps.data;

import com.osrs.dps.model.PlayerCharacter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Imports character levels from the official OSRS hiscores. */
public final class HiscoresClient {

    /** Hiscore boards for the different account types. */
    public enum GameMode {
        REGULAR("Regular", "hiscore_oldschool"),
        IRONMAN("Ironman", "hiscore_oldschool_ironman"),
        HARDCORE_IRONMAN("Hardcore Ironman", "hiscore_oldschool_hardcore_ironman"),
        ULTIMATE_IRONMAN("Ultimate Ironman", "hiscore_oldschool_ultimate");

        private final String displayName;
        private final String endpoint;

        GameMode(String displayName, String endpoint) {
            this.displayName = displayName;
            this.endpoint = endpoint;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // Skill line indices in the index_lite CSV (rank,level,xp per line)
    private static final int ATTACK = 1;
    private static final int DEFENCE = 2;
    private static final int STRENGTH = 3;
    private static final int HITPOINTS = 4;
    private static final int RANGED = 5;
    private static final int MAGIC = 7;
    private static final int MINING = 15;

    private HiscoresClient() {
    }

    /**
     * Fetches a character's levels from the hiscores.
     *
     * @throws java.io.IOException when the player is not found or the request fails
     */
    public static PlayerCharacter fetch(String username, GameMode mode) throws java.io.IOException {
        String encoded = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
        String url = "https://secure.runescape.com/m=" + mode.endpoint
                + "/index_lite.ws?player=" + encoded;
        String body = Downloads.fetchString(url, 20_000);
        if (body == null || body.isBlank()) {
            throw new java.io.IOException(
                    "Player \"" + username + "\" was not found on the " + mode + " hiscores");
        }

        String[] lines = body.split("\\R");
        PlayerCharacter character = new PlayerCharacter();
        character.name = username.trim();
        character.attack = level(lines, ATTACK);
        character.defence = level(lines, DEFENCE);
        character.strength = level(lines, STRENGTH);
        character.hitpoints = Math.max(10, level(lines, HITPOINTS));
        character.ranged = level(lines, RANGED);
        character.magic = level(lines, MAGIC);
        character.mining = level(lines, MINING);
        character.currentHitpoints = 0; // full health
        return character;
    }

    private static int level(String[] lines, int index) throws java.io.IOException {
        if (index >= lines.length) {
            throw new java.io.IOException("Unexpected hiscores response format");
        }
        String[] parts = lines[index].split(",");
        if (parts.length < 2) {
            throw new java.io.IOException("Unexpected hiscores response format");
        }
        try {
            // unranked skills are reported as -1
            return Math.max(1, Integer.parseInt(parts[1].trim()));
        } catch (NumberFormatException e) {
            throw new java.io.IOException("Unexpected hiscores response format");
        }
    }
}
