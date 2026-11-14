package com.liftoff.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.engine.sim.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public final class MissionLog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;
    private final List<Entry> entries = new ArrayList<>();


    public record Entry(String missionId, Difficulty difficulty, double score, int stars, String finishedAt) {
    }


    public MissionLog() {
        this(defaultPath());
    }


    public MissionLog(Path file) {
        this.file = file;
        entries.addAll(read(file));
    }


    public static MissionLog inMemory() {
        return new MissionLog(null);
    }

    public static Path defaultPath() {
        return Path.of(System.getProperty("user.home"), ".liftoff", "mission-log.json");
    }


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


    public List<Entry> entries() {
        return List.copyOf(entries);
    }


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

            return List.of();
        }
    }
}
