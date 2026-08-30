package com.example.killsound.detection;

import com.example.killsound.KillSoundClient;
import com.example.killsound.config.KillSoundConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class DeathDetector {

    private boolean deathHandled;

    public void onClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        boolean isDead = !player.isAlive() || player.isDeadOrDying() || player.getHealth() <= 0.0f;

        if (isDead) {
            if (!deathHandled) {
                deathHandled = true;
                KillSoundConfig config = KillSoundClient.getInstance().getConfig();
                if (config != null && config.ownDeathEnabled && KillSoundClient.getInstance().getSoundManager() != null) {
                    KillSoundClient.getInstance().getSoundManager().playOwnDeathSound();
                }
            }
        } else {
            deathHandled = false;
        }
    }

    public void reset() {
        deathHandled = false;
    }
}
