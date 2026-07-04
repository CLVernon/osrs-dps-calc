package com.osrs.dps.data;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Loads item/monster icons from the OSRS wiki, caching them on disk under the
 * user data directory. Images load asynchronously; callers receive a callback
 * on the JavaFX thread once the image is available.
 */
public final class ImageCache {

    private static final String WIKI_IMAGE_BASE = "https://oldschool.runescape.wiki/images/";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "image-cache");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Image> MEMORY = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Image>> IN_FLIGHT = new ConcurrentHashMap<>();

    private ImageCache() {
    }

    private static Path cacheDir() {
        return DataUpdater.baseDir().resolve("cache").resolve("images");
    }

    /** Returns the image immediately if cached in memory, else null. */
    public static Image cached(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        return MEMORY.get(imageName);
    }

    /**
     * Loads the image asynchronously; the callback runs on the JavaFX thread.
     * The callback is not invoked when the image cannot be loaded.
     */
    public static void load(String imageName, Consumer<Image> onLoaded) {
        if (imageName == null || imageName.isBlank()) {
            return;
        }
        Image memory = MEMORY.get(imageName);
        if (memory != null) {
            onLoaded.accept(memory);
            return;
        }
        IN_FLIGHT.computeIfAbsent(imageName, name -> CompletableFuture.supplyAsync(
                () -> loadBlocking(name), EXECUTOR))
                .thenAccept(image -> {
                    IN_FLIGHT.remove(imageName);
                    if (image != null) {
                        MEMORY.put(imageName, image);
                        javafx.application.Platform.runLater(() -> onLoaded.accept(image));
                    }
                });
    }

    private static Image loadBlocking(String imageName) {
        try {
            Files.createDirectories(cacheDir());
            Path file = cacheDir().resolve(sanitize(imageName));
            if (!Files.isRegularFile(file) || Files.size(file) == 0) {
                String encoded = URLEncoder.encode(imageName.replace(' ', '_'), StandardCharsets.UTF_8)
                        .replace("+", "%20");
                if (!Downloads.fetchToFile(WIKI_IMAGE_BASE + encoded, file, 20_000)) {
                    Files.deleteIfExists(file);
                    return null;
                }
            }
            try (InputStream in = Files.newInputStream(file)) {
                Image image = new Image(in, 32, 32, true, true);
                return image.isError() ? null : image;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
