package dev.duckslock.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class TDConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private TDConfigManager() {
    }

    public static TDConfig loadOrCreate(Path configPath, HytaleLogger logger) {
        TDConfig config = null;

        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                config = GSON.fromJson(json, TDConfig.class);
            }
        } catch (IOException | JsonParseException ex) {
            logger.at(Level.WARNING).log("Failed to load config '%s': %s", configPath, ex.toString());
        }

        if (config == null) {
            config = TDConfig.defaults();
            logger.at(Level.INFO).log("Using default Tower Defense config.");
        }

        config.sanitize(logger);
        save(configPath, config, logger);
        return config;
    }

    public static void save(Path configPath, TDConfig config, HytaleLogger logger) {
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(configPath, GSON.toJson(config));
        } catch (IOException ex) {
            logger.at(Level.WARNING).log("Failed to write config '%s': %s", configPath, ex.toString());
        }
    }
}
