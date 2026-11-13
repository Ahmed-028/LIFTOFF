package com.liftoff.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorLogTest {

    @TempDir
    Path folder;

    @Test
    void recordsTheErrorItsCauseAndWhereItHappened() throws IOException {
        Path file = folder.resolve("deep").resolve("error.log");
        ErrorLog log = new ErrorLog(file);

        assertTrue(log.record(new IllegalStateException("screen broke", new ArithmeticException("root cause"))));

        String written = Files.readString(file);
        assertTrue(written.contains("IllegalStateException: screen broke"), written);
        assertTrue(written.contains("ArithmeticException: root cause"), written);
        assertTrue(written.contains("ErrorLogTest"), "the stack trace names where it happened");
        assertEquals(file, log.file().orElseThrow());
    }

    @Test
    void laterErrorsAreAddedBelowEarlierOnes() throws IOException {
        Path file = folder.resolve("error.log");
        ErrorLog log = new ErrorLog(file);
        log.record(new RuntimeException("first"));
        log.record(new RuntimeException("second"));

        String written = Files.readString(file);
        assertTrue(written.indexOf("first") >= 0 && written.indexOf("first") < written.indexOf("second"), written);
    }

    @Test
    void aLogThatCannotBeWrittenNeverThrows() throws IOException {
        Path blocked = Files.createDirectory(folder.resolve("error.log"));

        assertFalse(new ErrorLog(blocked).record(new RuntimeException("lost")));
        assertFalse(new ErrorLog(null).record(new RuntimeException("memory only")));
        assertFalse(new ErrorLog(folder.resolve("x.log")).record(null));
        assertTrue(new ErrorLog(null).file().isEmpty());
    }

    @Test
    void theGameKeepsItsLogBesideTheMissionLog() {
        Path file = ErrorLog.inUserHome().file().orElseThrow();
        assertEquals("error.log", file.getFileName().toString());
        assertEquals(".liftoff", file.getParent().getFileName().toString());
    }
}
