package com.liftoff;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.BuildIssue;
import com.liftoff.engine.DesignIssue;
import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.engine.score.ReportTexts;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.model.Difficulty;
import com.liftoff.model.MeasurementType;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.StepUnit;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionFlow;
import com.liftoff.session.MissionStep;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every message key the code can ask for exists in messages.properties and is a valid pattern. */
class MessageKeysTest {

    private static final ResourceBundle MESSAGES =
            ResourceBundle.getBundle("com.liftoff.i18n.messages", Locale.ENGLISH);

    private static void assertKey(String key) {
        assertTrue(MESSAGES.containsKey(key), "messages.properties has no entry for " + key);
        assertFalse(MESSAGES.getString(key).isBlank(), key + " is blank");
        new MessageFormat(MESSAGES.getString(key));   // throws on a malformed pattern
    }

    @Test
    void everyBuildIssueKindHasAWarning() {
        for (BuildIssue.Kind kind : BuildIssue.Kind.values()) {
            assertKey("warning." + kind);
        }
    }

    @Test
    void everyDesignIssueKindHasAMessage() {
        for (DesignIssue.Kind kind : DesignIssue.Kind.values()) {
            assertKey("design." + kind);
        }
        assertKey("design.RESOLUTION_UNKNOWN");
    }

    @Test
    void everyLaunchIssueHasAMessage() {
        for (LaunchIssue.Kind kind : LaunchIssue.Kind.values()) {
            assertKey(kind.messageKey());
        }
    }

    @Test
    void everyWhyCardHasAMessageAndAnEnglishFallback() {
        assertEquals(34, ReportBuilder.messageKeys().size());
        for (String key : ReportBuilder.messageKeys()) {
            assertKey(key);
            assertTrue(ReportTexts.pattern(key) != null, "no fallback text for " + key);
        }
    }

    @Test
    void reportBuilderListsEveryKeyItDeclares() throws IllegalAccessException {
        List<String> declared = new ArrayList<>();
        for (Field field : ReportBuilder.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                declared.add((String) field.get(null));
            }
        }
        assertEquals(new TreeSet<>(declared), new TreeSet<>(ReportBuilder.messageKeys()));
    }

    @Test
    void everyStepLockReasonHasAMessage() throws IllegalAccessException {
        int reasons = 0;
        for (Field field : MissionFlow.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                assertKey((String) field.get(null));
                reasons++;
            }
        }
        assertEquals(7, reasons);
        assertKey(GameSession.NO_MISSION);
        assertKey(GameSession.ALREADY_FLOWN);
        assertKey(GameSession.FLIGHT_OUTDATED);
    }

    @Test
    void everySimulationEventInTheCatalogHasAMessage() {
        var events = CatalogLoader.loadFromClasspath().events();
        assertFalse(events.isEmpty());
        events.forEach(event -> assertKey(event.messageKey()));
    }

    @Test
    void everyEnumShownOnScreenHasAName() {
        for (ModuleSlot slot : ModuleSlot.values()) {
            assertKey("slot." + slot);
        }
        for (MissionStep step : List.of(MissionStep.BRIEFING, MissionStep.SHIP, MissionStep.SUPPLIES,
                MissionStep.REVIEW)) {
            assertKey("step." + step);
        }
        for (String state : List.of("CURRENT", "OPEN", "LOCKED")) {
            assertKey("step.state." + state);
            assertKey("step.symbol." + state);
        }
        for (ResourceLevel level : ResourceLevel.values()) {
            assertKey("level." + level);
        }
        for (ResourceType type : ResourceType.values()) {
            assertKey("resource." + type);
        }
        for (MeasurementType type : MeasurementType.values()) {
            assertKey("measurement." + type);
        }
        for (Difficulty difficulty : Difficulty.values()) {
            assertKey("mission.difficulty." + difficulty);
        }
        for (StepUnit unit : StepUnit.values()) {
            assertKey("duration." + unit);
            assertKey("duration.single." + unit);
        }
        for (ValueBasis basis : ValueBasis.values()) {
            assertKey("basis." + basis);
        }
    }

    @Test
    void everyStateWordCarriesASymbolSoNothingIsShownByColourAlone() {
        for (ResourceLevel level : ResourceLevel.values()) {
            String text = MESSAGES.getString("level." + level);
            assertFalse(Character.isLetterOrDigit(text.codePointAt(0)), "level." + level + " needs a symbol: " + text);
            assertTrue(text.chars().anyMatch(Character::isLetter), "level." + level + " needs a word: " + text);
        }
    }

    @Test
    void everyKeyWrittenInTheSourceOrTheLayoutsExists() throws IOException {
        // Literal keys: text.get("key"...) and getString("key") in Java, text="%key" in FXML.
        Pattern inJava = Pattern.compile("(?:text\\.get|getString|text\\.label\\(text\\.get)\\(\\s*\"([a-zA-Z0-9_.]+)\"");
        Pattern inFxml = Pattern.compile("\"%([a-zA-Z0-9_.]+)\"");
        TreeSet<String> missing = new TreeSet<>();
        int checked = 0;
        try (Stream<Path> files = Files.walk(Path.of("src", "main"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                Pattern pattern = name.endsWith(".java") ? inJava : name.endsWith(".fxml") ? inFxml : null;
                if (pattern == null) {
                    continue;
                }
                Matcher match = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
                while (match.find()) {
                    String key = match.group(1);
                    if (key.endsWith(".")) {
                        continue;   // a prefix completed at run time; covered by the enum tests above
                    }
                    checked++;
                    if (!MESSAGES.containsKey(key)) {
                        missing.add(key + " (" + name + ")");
                    }
                }
            }
        }
        assertTrue(checked > 100, "the scan found too few keys to be trusted: " + checked);
        assertEquals(new TreeSet<String>(), missing);
    }
}
