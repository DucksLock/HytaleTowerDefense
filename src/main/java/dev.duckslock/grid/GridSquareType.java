package dev.duckslock.grid;

public enum GridSquareType {
    /** Enemy walking route — clicking just moves the commander */
    PATH,
    /** Player can place a tower here (if unoccupied) */
    BUILDABLE,
    /** Terrain, base structure, or decoration — no interaction */
    BLOCKED
}
