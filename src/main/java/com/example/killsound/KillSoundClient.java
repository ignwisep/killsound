package com.example.killsound;

import com.example.killsound.audio.CustomSoundManager;
import com.example.killsound.config.ConfigManager;
import com.example.killsound.config.KillSoundConfig;
import com.example.killsound.detection.DeathDetector;
import com.example.killsound.detection.KillDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
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

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && player == Minecraft.getInstance().player) {
                killDetector.onPlayerAttack(entity);
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() && player == Minecraft.getInstance().player) {
                killDetector.onPlayerUseBlock(hitResult.getBlockPos());
            }
            return InteractionResult.PASS;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleIncomingMessage(message);
        });

        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            handleIncomingMessage(message);
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

    private void handleIncomingMessage(Component message) {
        if (message == null) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        String localPlayerName = player.getScoreboardName();
        if (localPlayerName == null || localPlayerName.isBlank()) {
            localPlayerName = player.getName().getString();
        }

        deathDetector.onDeathMessage(message, localPlayerName);
        killDetector.onGameOrChatMessage(message, localPlayerName);
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
