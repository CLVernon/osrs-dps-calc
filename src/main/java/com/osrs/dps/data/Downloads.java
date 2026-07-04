package com.osrs.dps.data;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Small HTTP download helper built on HttpURLConnection, which works in
 * restricted environments where java.net.http's NIO selector cannot start.
 */
final class Downloads {

    private Downloads() {
    }

    /** Downloads a URL to a file; returns false on any failure (never throws). */
    static boolean fetchToFile(String url, Path target, int timeoutMillis) {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "osrs-dps-calc-desktop (personal project)");
            try {
                if (connection.getResponseCode() != 200) {
                    return false;
                }
                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                connection.disconnect();
            }
            return Files.isRegularFile(target) && Files.size(target) > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
