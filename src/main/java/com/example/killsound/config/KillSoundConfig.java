package com.example.killsound.config;

public class KillSoundConfig {

    public boolean enemyKillEnabled = true;
    public String enemyKillSound = "victory.ogg";
    public float enemyKillVolume = 1.0f;
    public float enemyKillPitch = 1.0f;

    public boolean ownDeathEnabled = true;
    public String ownDeathSound = "death.ogg";
    public float ownDeathVolume = 1.0f;
    public float ownDeathPitch = 1.0f;

    public void validate() {
        if (enemyKillSound == null) {
            enemyKillSound = "victory.ogg";
        }
        if (ownDeathSound == null) {
            ownDeathSound = "death.ogg";
        }
        enemyKillVolume = Math.max(0.0f, Math.min(2.0f, enemyKillVolume));
        enemyKillPitch = Math.max(0.5f, Math.min(2.0f, enemyKillPitch));
        ownDeathVolume = Math.max(0.0f, Math.min(2.0f, ownDeathVolume));
        ownDeathPitch = Math.max(0.5f, Math.min(2.0f, ownDeathPitch));
    }
}
