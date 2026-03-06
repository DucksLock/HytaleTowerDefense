package dev.duckslock.combat;

import java.util.Locale;

public enum ElementType {
    NONE,
    COMPOSITE,
    FIRE,
    NATURE,
    EARTH,
    LIGHT,
    DARKNESS,
    WATER;

    public static ElementType parseOrDefault(String value, ElementType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return ElementType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public boolean isElemental() {
        return this != NONE && this != COMPOSITE;
    }
}
