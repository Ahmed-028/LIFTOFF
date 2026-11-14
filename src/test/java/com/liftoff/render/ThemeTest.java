package com.liftoff.render;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Keeps the Java style guide and the stylesheet tokens identical. */
class ThemeTest {

    private static final Path STYLESHEET = Path.of("src/main/resources/com/liftoff/css/liftoff.css");
    private static final Pattern BLOCK = Pattern.compile("(?m)^\\.(root|world-[a-z]+)\\s*\\{([^}]*)}");
    private static final Pattern TOKEN = Pattern.compile("(-[a-z0-9-]+)\\s*:\\s*(#[0-9A-Fa-f]{6})\\s*;");

    @Test
    void everyJavaTokenMatchesTheStylesheet() throws IOException {
        Map<String, Color> css = new HashMap<>();
        var blocks = BLOCK.matcher(Files.readString(STYLESHEET));
        while (blocks.find()) {
            String prefix = blocks.group(1).equals("root") ? "" : blocks.group(1) + "/";
            var tokens = TOKEN.matcher(blocks.group(2));
            while (tokens.find()) {
                css.put(prefix + tokens.group(1), Color.web(tokens.group(2)));
            }
        }
        Theme.stylesheetTokens().forEach((name, color) -> {
            assertNotNull(css.get(name), "liftoff.css has no token " + name);
            assertEquals(css.get(name), color, "Theme and liftoff.css disagree on " + name);
        });
    }
}
