package com.liftoff.engine;

/** How a resource stands against its limit. */
public enum ResourceLevel {
    /** Comfortably inside the limit. */
    OK,
    /** Inside the limit, but with little margin left. */
    TIGHT,
    /** Over the limit. */
    OVER,
    /** The limit is not known yet, because the part that sets it has not been chosen. */
    UNKNOWN
}
