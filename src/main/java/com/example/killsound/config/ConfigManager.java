package com.example.killsound.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("KillSound");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configPath;
    private KillSoundConfig config;

    public ConfigManager(Path configPath) {
        this.configPath = configPath;
        this.config = new KillSoundConfig();
    }

    public void loadConfig() {
        if (!Files.exists(configPath)) {
            config = new KillSoundConfig();
            saveConfig();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(configPath)) {
            Object parsed = GSON.fromJson(reader, (Type) KillSoundConfig.class);
            if (parsed instanceof KillSoundConfig loaded) {
                loaded.validate();
                this.config = loaded;
                return;
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.warn("Failed to parse config file, restoring defaults: " + e.getMessage());
        }

        this.config = new KillSoundConfig();
        saveConfig();
    }

    public void saveConfig() {
        try {
            Path parent = configPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save config file: " + e.getMessage());
        }
    }

    public KillSoundConfig getConfig() {
        return config;
    }
}
