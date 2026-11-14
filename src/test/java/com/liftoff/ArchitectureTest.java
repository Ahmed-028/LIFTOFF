package com.liftoff;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class ArchitectureTest {

    private static final Path SOURCES = Path.of("src", "main", "java", "com", "liftoff");
    private static final List<String> LOGIC_PACKAGES = List.of("model", "engine", "data", "session", "util", "minigame", "audio");

    @Test
    void logicPackagesNeverImportJavaFx() throws IOException {
        List<String> offenders = new ArrayList<>();
        int filesChecked = 0;

        for (String packageName : LOGIC_PACKAGES) {
            Path directory = SOURCES.resolve(packageName);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    filesChecked++;
                    if (Files.readString(file).contains("javafx.")) {
                        offenders.add(file.toString());
                    }
                }
            }
        }

        assertTrue(filesChecked > 0, "no logic sources found under " + SOURCES.toAbsolutePath());
        assertFalse(!offenders.isEmpty(), "JavaFX used in logic packages: " + offenders);
    }
}
