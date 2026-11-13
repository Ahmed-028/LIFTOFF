package com.liftoff.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.model.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * The finished missions on this computer and their scores, kept as JSON in the user's home folder.
 * It ranks missions, never people: no name is asked for or stored, and nothing leaves the machine.
 */
public final class MissionLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;
    private final Executor writer;
    private final List<Entry> entries = new ArrayList<>();

    /**
     * One finished mission.
     *
     * @param finishedAt when it ended, as an ISO-8601 instant
     */
    public record Entry(String missionId, Difficulty difficulty, double score, int stars, String finishedAt) {
    }

    /** The log at {@link #defaultPath()}. */
    public MissionLog() {
        this(defaultPath());
    }

    /** @param file where the log is kept; a missing or unreadable file means an empty log */
    public MissionLog(Path file) {
        this(file, null);
    }

    /**
     * @param writer where the file is written, so a slow disk never holds up the caller; null writes
     *               at once. Reading happens here, so construct the log off the FX thread too.
     */
    public MissionLog(Path file, Executor writer) {
        this.file = file;
        this.writer = writer;
        entries.addAll(read(file));
    }

    /** A log that lives only as long as the session and never touches the disk. */
    public static MissionLog inMemory() {
        return new MissionLog(null);
    }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".liftoff", "mission-log.json");
    }

    /**
     * Adds the entry and saves the log.
     *
     * @return true if the log was saved, or handed to the background writer; false if it is in
     *         memory only or the file could not be written (the entry is kept for this session either way)
     */
    public boolean add(Entry entry) {
        entries.add(entry);
        if (file == null) {
            return false;
        }
        List<Entry> snapshot = List.copyOf(entries);
        if (writer == null) {
            return save(snapshot);
        }
        writer.execute(() -> save(snapshot));
        return true;
    }

    private boolean save(List<Entry> snapshot) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    /** Every entry, oldest first. */
    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    /** The best {@code count} missions, highest score first; equal scores keep their order of play. */
    public List<Entry> top(int count) {
        return entries.stream()
                .sorted(Comparator.comparingDouble(Entry::score).reversed())
                .limit(Math.max(0, count))
                .toList();
    }

    private static List<Entry> read(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return List.of();
        }
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            if (root == null || !root.isArray()) {
                return List.of();
            }
            // Entry by entry, so one damaged line does not throw away the rest of the log.
            List<Entry> read = new ArrayList<>();
            for (JsonNode node : root) {
                entry(node).ifPresent(read::add);
            }
            return read;
        } catch (IOException | RuntimeException e) {
            // A damaged log is not worth stopping the game for: start again from empty.
            return List.of();
        }
    }

    private static Optional<Entry> entry(JsonNode node) {
        JsonNode mission = node.get("missionId");
        JsonNode score = node.get("score");
        JsonNode stars = node.get("stars");
        JsonNode finished = node.get("finishedAt");
        Difficulty difficulty = difficulty(node.path("difficulty").asText(""));
        if (mission == null || !mission.isTextual() || mission.asText().isBlank() || difficulty == null
                || score == null || !score.isNumber() || stars == null || !stars.isInt()
                || finished == null || !finished.isTextual()) {
            return Optional.empty();
        }
        return Optional.of(new Entry(mission.asText(), difficulty, score.asDouble(), stars.asInt(),
                finished.asText()));
    }

    /** Reads today's names and the ones logs were written with before difficulty moved onto the mission. */
    private static Difficulty difficulty(String name) {
        return switch (name) {
            case "EASY", "CADET" -> Difficulty.EASY;
            case "MEDIUM", "ENGINEER" -> Difficulty.MEDIUM;
            case "HARD", "MISSION_DIRECTOR" -> Difficulty.HARD;
            default -> null;
        };
    }
}
