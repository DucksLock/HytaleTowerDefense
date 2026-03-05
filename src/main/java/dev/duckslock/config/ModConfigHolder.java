package dev.duckslock.config;

public final class ModConfigHolder {

    private static volatile TDConfig config = TDConfig.defaults();

    private ModConfigHolder() {
    }

    public static TDConfig get() {
        return config;
    }

    public static void set(TDConfig config) {
        ModConfigHolder.config = config == null ? TDConfig.defaults() : config;
    }
}
