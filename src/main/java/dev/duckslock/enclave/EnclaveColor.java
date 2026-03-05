package dev.duckslock.enclave;

/**
 * Team color metadata mapped by enclave index.
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

    public static final EnclaveColor[] VALUES = values();

    private final int index;
    private final String displayName;
    private final String hexColor;

    EnclaveColor(int index, String displayName, String hexColor) {
        this.index = index;
        this.displayName = displayName;
        this.hexColor = hexColor;
    }

    public static EnclaveColor fromIndex(int index) {
        if (index >= 0 && index < VALUES.length) {
            return VALUES[index];
        }
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
