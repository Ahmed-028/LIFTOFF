package com.liftoff.session;

import com.liftoff.engine.sim.Difficulty;
import com.liftoff.session.MissionLog.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionLogTest {

    @TempDir
    Path folder;

    private static Entry entry(String missionId, double score) {
        return new Entry(missionId, Difficulty.CADET, score, 2, "2026-11-13T10:00:00Z");
    }

    @Test
    void aLogWithNoFileStartsEmpty() {
        MissionLog log = new MissionLog(folder.resolve("none.json"));

        assertTrue(log.entries().isEmpty());
        assertTrue(log.top(3).isEmpty());
    }

    @Test
    void entriesSurviveARoundTripThroughTheFile() {
        Path file = folder.resolve("deep").resolve("mission-log.json");
        MissionLog log = new MissionLog(file);
        Entry first = new Entry("farm-watch", Difficulty.MISSION_DIRECTOR, 87.5, 3, "2026-11-13T10:00:00Z");

        assertTrue(log.add(first));
        assertTrue(log.add(entry("moon-landing", 40)));

        assertTrue(Files.exists(file));
        assertEquals(List.of(first, entry("moon-landing", 40)), new MissionLog(file).entries());
    }

    @Test
    void aCorruptFileIsTreatedAsEmptyAndCanBeWrittenAgain() throws IOException {
        Path file = folder.resolve("mission-log.json");
        Files.writeString(file, "{ this is not a mission log ");

        MissionLog log = new MissionLog(file);
        assertTrue(log.entries().isEmpty());

        log.add(entry("farm-watch", 70));
        assertEquals(List.of(entry("farm-watch", 70)), new MissionLog(file).entries());
    }

    @Test
    void aFileOfTheWrongShapeIsTreatedAsEmpty() throws IOException {
        Path file = folder.resolve("mission-log.json");
        Files.writeString(file, "[{ \"player\": \"someone\" }]");

        assertTrue(new MissionLog(file).entries().isEmpty());
    }

    @Test
    void topReturnsTheBestScoresFirst() {
        MissionLog log = new MissionLog(folder.resolve("mission-log.json"));
        log.add(entry("a", 40));
        log.add(entry("b", 90));
        log.add(entry("c", 65));

        assertEquals(List.of("b", "c"), log.top(2).stream().map(Entry::missionId).toList());
        assertEquals(List.of("b", "c", "a"), log.top(10).stream().map(Entry::missionId).toList());
        assertTrue(log.top(0).isEmpty());
    }

    @Test
    void anInMemoryLogKeepsEntriesButWritesNothing() {
        MissionLog log = MissionLog.inMemory();

        assertFalse(log.add(entry("a", 40)));
        assertEquals(1, log.entries().size());
    }

    @Test
    void anUnwritablePathDoesNotStopTheGame() throws IOException {
        Path blocker = Files.writeString(folder.resolve("a-file"), "x");
        MissionLog log = new MissionLog(blocker.resolve("mission-log.json"));

        assertFalse(log.add(entry("a", 40)));
        assertEquals(1, log.entries().size());
    }

    @Test
    void anEntryRecordsTheMissionAndNeverAPerson() {
        List<String> fields = Stream.of(Entry.class.getRecordComponents()).map(RecordComponent::getName).toList();

        assertEquals(List.of("missionId", "difficulty", "score", "stars", "finishedAt"), fields);
    }

    @Test
    void theDefaultLogLivesInTheLiftoffFolderOfTheHomeDirectory() {
        assertEquals(Path.of(System.getProperty("user.home"), ".liftoff", "mission-log.json"),
                MissionLog.defaultPath());
    }
}
