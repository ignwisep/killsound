package com.example.killsound.detection;

import com.example.killsound.KillSoundClient;
import com.example.killsound.config.KillSoundConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.regex.Pattern;

public class DeathDetector {

    private boolean deathHandled;
    private long lastDeathTriggerTime;

    public void onClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        boolean isDead = !player.isAlive() || player.isDeadOrDying() || player.getHealth() <= 0.0f || client.screen instanceof DeathScreen;

        if (isDead) {
            triggerDeath();
        } else {
            deathHandled = false;
        }
    }

    public void onDeathMessage(Component message, String localPlayerName) {
        if (message == null || localPlayerName == null || localPlayerName.isBlank()) {
            return;
        }

        if (message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key.startsWith("death.")) {
                Object[] args = translatable.getArgs();
                if (args.length >= 1) {
                    String victim = extractText(args[0]);
                    if (isNameMatch(victim, localPlayerName)) {
                        triggerDeath();
                        return;
                    }
                }
            }
        }

        String rawText = message.getString();
        String clean = rawText.replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                .replaceAll("§x(§[0-9a-fA-F]){6}", "")
                .replaceAll("[\\uE000-\\uF8FF\\uD800-\\uDFFF]", " ")
                .replaceAll("[\\p{So}\\p{Sk}\\p{Cn}]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        String lower = clean.toLowerCase();
        String lowerName = localPlayerName.toLowerCase();

        boolean containsName = Pattern.compile("(?i)(^|[^a-zA-Z0-9_])" + Pattern.quote(lowerName) + "($|[^a-zA-Z0-9_])").matcher(clean).find();

        if (!containsName) {
            if (lower.contains("you died") || lower.contains("you were killed") || lower.startsWith("[death] you")) {
                triggerDeath();
            }
            return;
        }

        if (lower.startsWith(lowerName + ":") || lower.startsWith("<" + lowerName + ">") || lower.startsWith("[" + lowerName + "]")) {
            return;
        }

        boolean isVictim = false;

        if (lower.startsWith(lowerName + " was ") || lower.startsWith(lowerName + " died") || lower.startsWith(lowerName + " drowned")
                || lower.startsWith(lowerName + " fell") || lower.startsWith(lowerName + " burned") || lower.startsWith(lowerName + " blew up")) {
            isVictim = true;
        } else if (lower.contains(" killed " + lowerName) || lower.contains(" eliminated " + lowerName) || lower.contains(" obliterated " + lowerName)) {
            isVictim = true;
        } else if (lower.startsWith("you died") || lower.startsWith("you were slain") || lower.startsWith("you were killed")) {
            isVictim = true;
        }

        if (isVictim) {
            triggerDeath();
        }
    }

    private String extractText(Object arg) {
        if (arg instanceof Component comp) {
            return comp.getString();
        } else if (arg != null) {
            return arg.toString();
        }
        return "";
    }

    private boolean isNameMatch(String text, String name) {
        if (text == null || name == null) {
            return false;
        }
        return text.trim().equalsIgnoreCase(name.trim());
    }

    public synchronized void triggerDeath() {
        long now = System.currentTimeMillis();
        if (now - lastDeathTriggerTime < 500) {
            return;
        }
        lastDeathTriggerTime = now;

        if (!deathHandled) {
            deathHandled = true;
            KillSoundConfig config = KillSoundClient.getInstance().getConfig();
            if (config != null && config.ownDeathEnabled && KillSoundClient.getInstance().getSoundManager() != null) {
                KillSoundClient.getInstance().getSoundManager().playOwnDeathSound();
            }
        }
    }

    public void reset() {
        deathHandled = false;
    }
}
