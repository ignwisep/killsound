package com.example.killsound;

import com.example.killsound.audio.CustomSoundManager;
import com.example.killsound.config.ConfigManager;
import com.example.killsound.config.KillSoundConfig;
import com.example.killsound.detection.DeathDetector;
import com.example.killsound.detection.KillDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class KillSoundClient implements ClientModInitializer {

    public static final String MOD_ID = "killsound";
    public static final Logger LOGGER = LoggerFactory.getLogger("KillSound");

    private static KillSoundClient instance;

    private ConfigManager configManager;
    private CustomSoundManager soundManager;
    private KillDetector killDetector;
    private DeathDetector deathDetector;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("KillSound initializing...");

        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path configFile = gameDir.resolve("config").resolve("killsound.json");
        Path soundsDir = gameDir.resolve("killsound").resolve("sounds");

        this.configManager = new ConfigManager(configFile);
        this.configManager.loadConfig();

        this.soundManager = new CustomSoundManager(soundsDir);
        this.soundManager.initialize();

        this.killDetector = new KillDetector();
        this.deathDetector = new DeathDetector();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            killDetector.onClientTick(client);
            deathDetector.onClientTick(client);
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            killDetector.reset();
            deathDetector.reset();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            killDetector.reset();
            deathDetector.reset();
        });

        LOGGER.info("KillSound initialized successfully.");
    }

    public static KillSoundClient getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public KillSoundConfig getConfig() {
        return configManager.getConfig();
    }

    public CustomSoundManager getSoundManager() {
        return soundManager;
    }

    public KillDetector getKillDetector() {
        return killDetector;
    }

    public DeathDetector getDeathDetector() {
        return deathDetector;
    }
}
