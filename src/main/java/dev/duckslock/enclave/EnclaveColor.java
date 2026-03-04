package dev.duckslock.enclave;

/**
 * The eight player colours, matching classic Warcraft III team colours.
 * Indices 0-7 map directly to enclaves 0-7 in assignment order.
 */
public enum EnclaveColor {

    RED(0, "Red", "#CC2200"),
    BLUE(1, "Blue", "#0042FF"),
    TEAL(2, "Teal", "#1CE6B9"),
    PURPLE(3, "Purple", "#540081"),
    YELLOW(4, "Yellow", "#FFFC01"),
    ORANGE(5, "Orange", "#FE8A0E"),
    GREEN(6, "Green", "#20C000"),
    PINK(7, "Pink", "#E55BB0");

    // -------------------------------------------------------------------------

    public static final EnclaveColor[] VALUES = values();

    private final int index;
    private final String displayName;
    private final String hexColor;   // CSS hex, useful for UI / chat colouring

    EnclaveColor(int index, String displayName, String hexColor) {
        this.index = index;
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    /**
     * Returns the EnclaveColor for the given 0-based index.
     * Throws IllegalArgumentException for out-of-range values.
     */
    public static EnclaveColor fromIndex(int index) {
        if (index >= 0 && index < VALUES.length) return VALUES[index];
        throw new IllegalArgumentException("No EnclaveColor for index " + index);
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHexColor() {
        return hexColor;
    }
}
