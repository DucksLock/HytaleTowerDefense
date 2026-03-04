package dev.duckslock.grid;

/**
 * Defines what a grid square can be used for.
 * Every square in the TD arena is exactly one of these types.
 */
public enum GridSquareType {

    /**
     * Player may build a tower here.
     */
    BUILDABLE,

    /**
     * Enemy walk route — no building allowed.
     */
    PATH,

    /**
     * Impassable terrain, walls, or decoration — no building allowed.
     */
    BLOCKED,

    /**
     * The player's base row — losing lives happens here when enemies reach it.
     */
    BASE
}
