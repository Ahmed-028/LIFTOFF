package com.liftoff.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.engine.sim.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The finished missions on this computer and their scores, kept as JSON in the user's home folder.
 * It ranks missions, never people: no name is asked for or stored, and nothing leaves the machine.
 */
public final class MissionLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;
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
        this.file = file;
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
     * @return true if the log was saved; false if it is in memory only or the file could not be
     *         written (the entry is kept for this session either way)
     */
    public boolean add(Entry entry) {
        entries.add(entry);
        if (file == null) {
            return false;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), entries);
            return true;
        } catch (IOException e) {
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
            List<Entry> read = MAPPER.readValue(file.toFile(),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Entry.class));
            return read == null ? List.of() : read;
        } catch (IOException | RuntimeException e) {
            // A damaged log is not worth stopping the game for: start again from empty.
            return List.of();
        }
    }
}
