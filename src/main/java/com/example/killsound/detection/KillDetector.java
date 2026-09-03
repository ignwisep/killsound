package com.example.killsound.detection;

import com.example.killsound.KillSoundClient;
import com.example.killsound.audio.CustomSoundManager;
import com.example.killsound.config.KillSoundConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class KillDetector {

    private int previousPlayerKills;
    private boolean initialized;
    private long lastCrystalDetonationTime;
    private Vec3 lastCrystalDetonationPos;
    private long lastKillTriggerTime;

    private final Map<UUID, Long> recentAttackedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Vec3> previousPlayerPositions = new ConcurrentHashMap<>();
    private final Set<UUID> handledVictims = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void onClientTick(Minecraft client) {
        if (client == null) {
            return;
        }

        LocalPlayer player = client.player;
        if (player == null) {
            reset();
            return;
        }

        checkStatistics(player);
        checkWorldPlayerDeaths(client, player);
        cleanExpiredAttacks();
    }

    private void checkStatistics(LocalPlayer player) {
        try {
            StatsCounter stats = player.getStats();
            int current = stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS));

            if (!initialized) {
                previousPlayerKills = current;
                initialized = true;
                return;
            }

            if (current > previousPlayerKills) {
                int difference = Math.min(current - previousPlayerKills, 5);
                for (int i = 0; i < difference; i++) {
                    triggerKill();
                }
                previousPlayerKills = current;
            } else if (current < previousPlayerKills) {
                previousPlayerKills = current;
            }
        } catch (Throwable ignored) {
        }
    }

    private void checkWorldPlayerDeaths(Minecraft client, LocalPlayer player) {
        if (client.level == null || player == null) {
            return;
        }

        List<AbstractClientPlayer> players = client.level.players();
        if (players == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean hasRecentCrystal = (now - lastCrystalDetonationTime) <= 1000;
        Vec3 crystalPos = (lastCrystalDetonationPos != null) ? lastCrystalDetonationPos : player.position();

        Set<UUID> currentVisiblePlayers = new HashSet<>();

        for (AbstractClientPlayer other : players) {
            if (other == null || other == player) {
                continue;
            }

            UUID victimId = other.getUUID();
            currentVisiblePlayers.add(victimId);
            Vec3 otherPos = other.position();

            boolean isVictimDead = !other.isAlive() || other.isDeadOrDying() || other.getHealth() <= 0.0f || other.deathTime > 0;

            if (isVictimDead) {
                if (!handledVictims.contains(victimId)) {
                    boolean wasDirectFatalHit = (now - recentAttackedPlayers.getOrDefault(victimId, 0L)) <= 850 && otherPos.distanceToSqr(player.position()) <= 36.0;
                    boolean isNearCrystalExplosion = hasRecentCrystal && otherPos.distanceToSqr(crystalPos) <= 64.0;

                    if (wasDirectFatalHit || isNearCrystalExplosion) {
                        handledVictims.add(victimId);
                        triggerKill();
                    }
                }
            } else {
                handledVictims.remove(victimId);

                Vec3 previousPos = previousPlayerPositions.get(victimId);
                if (previousPos != null) {
                    double distSq = previousPos.distanceToSqr(otherPos);
                    if (distSq > 400.0) {
                        boolean wasNearOurExplosion = hasRecentCrystal && previousPos.distanceToSqr(crystalPos) <= 64.0;
                        boolean wasDirectFatalHit = (now - recentAttackedPlayers.getOrDefault(victimId, 0L)) <= 850 && previousPos.distanceToSqr(player.position()) <= 36.0;
                        if ((wasNearOurExplosion || wasDirectFatalHit) && !handledVictims.contains(victimId)) {
                            handledVictims.add(victimId);
                            triggerKill();
                        }
                    }
                }
                previousPlayerPositions.put(victimId, otherPos);
            }
        }

        for (Map.Entry<UUID, Vec3> entry : previousPlayerPositions.entrySet()) {
            UUID victimId = entry.getKey();
            if (!currentVisiblePlayers.contains(victimId)) {
                Vec3 lastPos = entry.getValue();
                boolean wasNearOurExplosion = hasRecentCrystal && lastPos.distanceToSqr(crystalPos) <= 64.0;
                boolean wasDirectFatalHit = (now - recentAttackedPlayers.getOrDefault(victimId, 0L)) <= 850 && lastPos.distanceToSqr(player.position()) <= 36.0;
                if ((wasNearOurExplosion || wasDirectFatalHit) && !handledVictims.contains(victimId)) {
                    if (lastPos.distanceToSqr(player.position()) <= 400.0) {
                        handledVictims.add(victimId);
                        triggerKill();
                    }
                }
            }
        }

        previousPlayerPositions.keySet().retainAll(currentVisiblePlayers);
    }

    private void cleanExpiredAttacks() {
        long now = System.currentTimeMillis();
        recentAttackedPlayers.entrySet().removeIf(entry -> (now - entry.getValue()) > 2000);
    }

    public void onPlayerAttack(Entity target) {
        if (target instanceof Player victim) {
            recentAttackedPlayers.put(victim.getUUID(), System.currentTimeMillis());
        }
        if (target != null && target.getType() == EntityType.END_CRYSTAL) {
            this.lastCrystalDetonationTime = System.currentTimeMillis();
            this.lastCrystalDetonationPos = target.position();
        }
    }

    public void onPlayerUseBlock(BlockPos pos) {
        if (pos != null) {
            this.lastCrystalDetonationTime = System.currentTimeMillis();
            this.lastCrystalDetonationPos = Vec3.atCenterOf(pos);
        }
    }

    public void onGameOrChatMessage(Component message, String localPlayerName) {
        if (message == null || localPlayerName == null || localPlayerName.isBlank()) {
            return;
        }

        if (message.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (key.startsWith("death.attack.") || key.startsWith("death.fell.")) {
                Object[] args = translatable.getArgs();
                if (args.length >= 2) {
                    String victim = extractText(args[0]);
                    String killer = extractText(args[1]);
                    if (isNameMatch(killer, localPlayerName) && !isNameMatch(victim, localPlayerName)) {
                        triggerKill();
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
            return;
        }

        if (lower.startsWith(lowerName + ":") || lower.startsWith("<" + lowerName + ">") || lower.startsWith("[" + lowerName + "]")) {
            return;
        }

        boolean isKiller = false;

        if (lower.contains(" by " + lowerName)) {
            isKiller = true;
        } else if (lower.contains(" fighting " + lowerName)) {
            isKiller = true;
        } else if (lower.contains(" escaping " + lowerName)) {
            isKiller = true;
        } else if (lower.contains(" from " + lowerName)) {
            isKiller = true;
        } else if (lower.contains(" " + lowerName + " killed ") || lower.startsWith(lowerName + " killed ")) {
            isKiller = true;
        } else if (lower.contains(" " + lowerName + " eliminated ") || lower.startsWith(lowerName + " eliminated ")) {
            isKiller = true;
        } else if (lower.contains(" " + lowerName + " obliterated ") || lower.startsWith(lowerName + " obliterated ")) {
            isKiller = true;
        } else if (lower.contains(" " + lowerName + " scored a kill")) {
            isKiller = true;
        } else if (lower.startsWith("you killed ") || lower.startsWith("you have slain ") || lower.startsWith("you eliminated ") || lower.startsWith("[kill] you killed")) {
            isKiller = true;
        } else if (lower.endsWith(" " + lowerName) || lower.endsWith("[" + lowerName + "]") || lower.endsWith("(" + lowerName + ")")) {
            isKiller = true;
        }

        if (isKiller) {
            triggerKill();
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

    public synchronized void triggerKill() {
        long now = System.currentTimeMillis();
        if (now - lastKillTriggerTime < 250) {
            return;
        }
        lastKillTriggerTime = now;

        KillSoundConfig config = KillSoundClient.getInstance().getConfig();
        CustomSoundManager soundManager = KillSoundClient.getInstance().getSoundManager();

        if (config != null && config.enemyKillEnabled && soundManager != null) {
            soundManager.playEnemyKillSound();
        }
    }

    public void reset() {
        previousPlayerKills = 0;
        initialized = false;
        lastCrystalDetonationTime = 0;
        lastCrystalDetonationPos = null;
        recentAttackedPlayers.clear();
        previousPlayerPositions.clear();
        handledVictims.clear();
    }
}
