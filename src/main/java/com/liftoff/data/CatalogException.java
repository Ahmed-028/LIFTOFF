package com.liftoff.data;

/** A catalog file is missing, malformed, or contains an entry that breaks the catalog rules. */
public class CatalogException extends RuntimeException {

    public CatalogException(String message) {
        super(message);
    }

    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
