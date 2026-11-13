package com.liftoff.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Where unexpected errors are written down, so a player can be shown a friendly screen while the
 * cause is kept for whoever fixes it. Recording never throws: a log that cannot be written is
 * not allowed to make a bad moment worse.
 */
public final class ErrorLog {

    private static final Logger LOGGER = Logger.getLogger("com.liftoff");

    private final Path file;

    /** @param file where errors are appended, or null to keep them out of the file system */
    public ErrorLog(Path file) {
        this.file = file;
    }

    /** The log beside the mission log, in the user's home folder. */
    public static ErrorLog inUserHome() {
        return new ErrorLog(Path.of(System.getProperty("user.home"), ".liftoff", "error.log"));
    }

    public Optional<Path> file() {
        return Optional.ofNullable(file);
    }

    /**
     * Writes the error and its whole stack trace to the console log and to the file.
     *
     * @return true if it reached the file
     */
    public boolean record(Throwable error) {
        if (error == null) {
            return false;
        }
        LOGGER.log(Level.SEVERE, "Unexpected error", error);
        if (file == null) {
            return false;
        }
        StringWriter text = new StringWriter();
        text.write(Instant.now() + System.lineSeparator());
        error.printStackTrace(new PrintWriter(text, true));
        text.write(System.lineSeparator());
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, text.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }
}
