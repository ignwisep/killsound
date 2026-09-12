package com.example.killsound.gui;

import com.example.killsound.KillSoundClient;
import com.example.killsound.audio.CustomSoundManager;
import com.example.killsound.config.KillSoundConfig;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DropdownStringControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class KillSoundConfigScreen {

    public static Screen create(Screen parent) {
        KillSoundConfig config = KillSoundClient.getInstance().getConfig();
        CustomSoundManager soundManager = KillSoundClient.getInstance().getSoundManager();
        soundManager.refreshSounds();

        List<String> soundNames = new ArrayList<>(soundManager.getAvailableSoundNames());
        if (soundNames.isEmpty()) {
            soundNames.add("No .ogg files found");
        }

        List<String> availableChoices = soundNames;

        Option<Boolean> enemyKillEnabledOption = Option.<Boolean>createBuilder()
                .name(Component.literal("Enabled"))
                .description(OptionDescription.of(Component.literal("Play sound when local player kills another player.")))
                .binding(true, () -> config.enemyKillEnabled, val -> config.enemyKillEnabled = val)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<String> enemyKillSoundOption = Option.<String>createBuilder()
                .name(Component.literal("Sound File"))
                .description(OptionDescription.of(Component.literal("Select an OGG sound file from killsound/sounds directory.")))
                .binding("victory.ogg", () -> config.enemyKillSound, val -> config.enemyKillSound = val)
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(availableChoices))
                .build();

        Option<Float> enemyKillVolumeOption = Option.<Float>createBuilder()
                .name(Component.literal("Volume"))
                .description(OptionDescription.of(Component.literal("Enemy kill playback volume.")))
                .binding(1.0f, () -> config.enemyKillVolume, val -> config.enemyKillVolume = val)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f).formatValue(v -> Component.literal(Math.round(v * 100) + "%")))
                .build();

        Option<Float> enemyKillPitchOption = Option.<Float>createBuilder()
                .name(Component.literal("Pitch"))
                .description(OptionDescription.of(Component.literal("Enemy kill playback pitch.")))
                .binding(1.0f, () -> config.enemyKillPitch, val -> config.enemyKillPitch = val)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.05f).formatValue(v -> Component.literal(Math.round(v * 100) + "%")))
                .build();

        ButtonOption testEnemyKillButton = ButtonOption.createBuilder()
                .name(Component.literal("Test Enemy Kill Sound"))
                .description(OptionDescription.of(Component.literal("Play the configured enemy kill sound with current volume and pitch.")))
                .action((yaclScreen, btn) -> {
                    String sound = enemyKillSoundOption.pendingValue();
                    if (sound == null || sound.isBlank()) {
                        sound = config.enemyKillSound;
                    }
                    Float vol = enemyKillVolumeOption.pendingValue();
                    float volume = (vol != null) ? vol : config.enemyKillVolume;
                    Float pit = enemyKillPitchOption.pendingValue();
                    float pitch = (pit != null) ? pit : config.enemyKillPitch;

                    soundManager.stopAll();
                    soundManager.play(sound, volume, pitch);
                })
                .build();

        Option<Boolean> ownDeathEnabledOption = Option.<Boolean>createBuilder()
                .name(Component.literal("Enabled"))
                .description(OptionDescription.of(Component.literal("Play sound when local player dies.")))
                .binding(true, () -> config.ownDeathEnabled, val -> config.ownDeathEnabled = val)
                .controller(BooleanControllerBuilder::create)
                .build();

        Option<String> ownDeathSoundOption = Option.<String>createBuilder()
                .name(Component.literal("Sound File"))
                .description(OptionDescription.of(Component.literal("Select an OGG sound file from killsound/sounds directory.")))
                .binding("death.ogg", () -> config.ownDeathSound, val -> config.ownDeathSound = val)
                .controller(opt -> DropdownStringControllerBuilder.create(opt).values(availableChoices))
                .build();

        Option<Float> ownDeathVolumeOption = Option.<Float>createBuilder()
                .name(Component.literal("Volume"))
                .description(OptionDescription.of(Component.literal("Own death playback volume.")))
                .binding(1.0f, () -> config.ownDeathVolume, val -> config.ownDeathVolume = val)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.0f, 2.0f).step(0.05f).formatValue(v -> Component.literal(Math.round(v * 100) + "%")))
                .build();

        Option<Float> ownDeathPitchOption = Option.<Float>createBuilder()
                .name(Component.literal("Pitch"))
                .description(OptionDescription.of(Component.literal("Own death playback pitch.")))
                .binding(1.0f, () -> config.ownDeathPitch, val -> config.ownDeathPitch = val)
                .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0.5f, 2.0f).step(0.05f).formatValue(v -> Component.literal(Math.round(v * 100) + "%")))
                .build();

        ButtonOption testOwnDeathButton = ButtonOption.createBuilder()
                .name(Component.literal("Test Own Death Sound"))
                .description(OptionDescription.of(Component.literal("Play the configured own death sound with current volume and pitch.")))
                .action((yaclScreen, btn) -> {
                    String sound = ownDeathSoundOption.pendingValue();
                    if (sound == null || sound.isBlank()) {
                        sound = config.ownDeathSound;
                    }
                    Float vol = ownDeathVolumeOption.pendingValue();
                    float volume = (vol != null) ? vol : config.ownDeathVolume;
                    Float pit = ownDeathPitchOption.pendingValue();
                    float pitch = (pit != null) ? pit : config.ownDeathPitch;

                    soundManager.stopAll();
                    soundManager.play(sound, volume, pitch);
                })
                .build();

        ButtonOption refreshSoundsButton = ButtonOption.createBuilder()
                .name(Component.literal("Refresh Sound List"))
                .description(OptionDescription.of(Component.literal("Scan the killsound/sounds folder for newly added .ogg files.")))
                .action((yaclScreen, btn) -> soundManager.refreshSounds())
                .build();

        ButtonOption openSoundsFolderButton = ButtonOption.createBuilder()
                .name(Component.literal("Open Sounds Folder"))
                .description(OptionDescription.of(Component.literal("Open the .minecraft/killsound/sounds folder in file explorer.")))
                .action((yaclScreen, btn) -> soundManager.openSoundsFolder())
                .build();

        ConfigCategory enemyKillCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Enemy Kill Sound"))
                .option(enemyKillEnabledOption)
                .option(enemyKillSoundOption)
                .option(enemyKillVolumeOption)
                .option(enemyKillPitchOption)
                .option(testEnemyKillButton)
                .build();

        ConfigCategory ownDeathCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Own Death Sound"))
                .option(ownDeathEnabledOption)
                .option(ownDeathSoundOption)
                .option(ownDeathVolumeOption)
                .option(ownDeathPitchOption)
                .option(testOwnDeathButton)
                .build();

        ConfigCategory managementCategory = ConfigCategory.createBuilder()
                .name(Component.literal("Sound Management"))
                .option(refreshSoundsButton)
                .option(openSoundsFolderButton)
                .build();

        YetAnotherConfigLib yacl = YetAnotherConfigLib.createBuilder()
                .title(Component.literal("KillSound Settings"))
                .category(enemyKillCategory)
                .category(ownDeathCategory)
                .category(managementCategory)
                .save(() -> KillSoundClient.getInstance().getConfigManager().saveConfig())
                .build();

        return yacl.generateScreen(parent);
    }
}
