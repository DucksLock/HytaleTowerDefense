package dev.duckslock.combat;

import java.util.EnumMap;
import java.util.Map;

/**
 * WC3 Element TD wheel:
 * Light -> Darkness -> Water -> Fire -> Nature -> Earth -> Light
 */
public final class ElementWheel {

    private static final Map<ElementType, ElementType> NEXT = new EnumMap<>(ElementType.class);
    private static final Map<ElementType, ElementType> PREVIOUS = new EnumMap<>(ElementType.class);

    static {
        NEXT.put(ElementType.LIGHT, ElementType.DARKNESS);
        NEXT.put(ElementType.DARKNESS, ElementType.WATER);
        NEXT.put(ElementType.WATER, ElementType.FIRE);
        NEXT.put(ElementType.FIRE, ElementType.NATURE);
        NEXT.put(ElementType.NATURE, ElementType.EARTH);
        NEXT.put(ElementType.EARTH, ElementType.LIGHT);

        for (Map.Entry<ElementType, ElementType> entry : NEXT.entrySet()) {
            PREVIOUS.put(entry.getValue(), entry.getKey());
        }
    }

    private ElementWheel() {
    }

    public static double damageMultiplier(ElementType attacker, ElementType defender) {
        ElementType safeAttacker = attacker == null ? ElementType.NONE : attacker;
        ElementType safeDefender = defender == null ? ElementType.NONE : defender;

        // Composite towers do neutral to elementals and reduced to non-elemental creeps.
        if (safeAttacker == ElementType.COMPOSITE) {
            return safeDefender == ElementType.NONE ? 0.8d : 1.0d;
        }

        // Non-elemental attackers are neutral.
        if (!safeAttacker.isElemental()) {
            return 1.0d;
        }

        // Elemental attacks against non-elemental creeps are neutral.
        if (!safeDefender.isElemental()) {
            return 1.0d;
        }

        if (NEXT.get(safeAttacker) == safeDefender) {
            return 2.0d;
        }

        if (PREVIOUS.get(safeAttacker) == safeDefender) {
            return 0.5d;
        }

        return 1.0d;
    }
}
