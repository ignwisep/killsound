package com.example.killsound.detection;

import com.example.killsound.KillSoundClient;
import com.example.killsound.audio.CustomSoundManager;
import com.example.killsound.config.KillSoundConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;

public class KillDetector {

    private int previousPlayerKills;
    private boolean initialized;

    public void onClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        StatsCounter stats = player.getStats();
        int current = stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));

        if (!initialized) {
            previousPlayerKills = current;
            initialized = true;
            return;
        }

        if (current > previousPlayerKills) {
            int difference = Math.min(current - previousPlayerKills, 5);
            KillSoundConfig config = KillSoundClient.getInstance().getConfig();
            CustomSoundManager soundManager = KillSoundClient.getInstance().getSoundManager();

            if (config != null && config.enemyKillEnabled && soundManager != null) {
                for (int i = 0; i < difference; i++) {
                    soundManager.playEnemyKillSound();
                }
            }
            previousPlayerKills = current;
        } else if (current < previousPlayerKills) {
            previousPlayerKills = current;
        }
    }

    public void reset() {
        previousPlayerKills = 0;
        initialized = false;
    }
}
