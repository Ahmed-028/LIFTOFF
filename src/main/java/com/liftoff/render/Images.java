package com.liftoff.render;

import com.liftoff.util.Background;
import javafx.scene.image.Image;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pictures from the game's resources, decoded once and shared by every screen. A picture can be
 * decoded ahead of time on a background thread; asking for it later then costs nothing.
 */
public final class Images {

    private static final Map<String, CompletableFuture<Image>> CACHE = new ConcurrentHashMap<>();

    private Images() {
    }

    /** The picture at its own size, or null if there is no such resource. */
    public static Image get(String resource) {
        return get(resource, 0, 0);
    }

    /** The picture fitted inside width x height (0 keeps the size), or null if there is no such resource. */
    public static Image get(String resource, double width, double height) {
        return CACHE.computeIfAbsent(key(resource, width, height),
                key -> CompletableFuture.completedFuture(decode(resource, width, height))).join();
    }

    /** Starts decoding the picture on a background thread. */
    public static void preload(String resource) {
        CACHE.computeIfAbsent(key(resource, 0, 0),
                key -> CompletableFuture.supplyAsync(() -> decode(resource, 0, 0), Background.POOL));
    }

    private static String key(String resource, double width, double height) {
        return resource + "@" + width + "x" + height;
    }

    private static Image decode(String resource, double width, double height) {
        var url = Images.class.getResource(resource);
        if (url == null) {
            return null;
        }
        return width > 0 || height > 0
                ? new Image(url.toExternalForm(), width, height, true, true)
                : new Image(url.toExternalForm());
    }
}
