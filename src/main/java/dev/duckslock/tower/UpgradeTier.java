package dev.duckslock.tower;

import java.util.Collections;
import java.util.Map;

public final class UpgradeTier {

    private final int cost;
    private final String name;
    private final Map<String, Double> statDeltas;

    public UpgradeTier(int cost, String name, Map<String, Double> statDeltas) {
        this.cost = Math.max(0, cost);
        this.name = name == null ? "Upgrade" : name;
        this.statDeltas = statDeltas == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(statDeltas);
    }

    public int getCost() {
        return cost;
    }

    public String getName() {
        return name;
    }

    public Map<String, Double> getStatDeltas() {
        return statDeltas;
    }
}
