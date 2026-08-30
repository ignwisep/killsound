package com.example.killsound.audio;

import com.example.killsound.KillSoundClient;
import com.example.killsound.config.KillSoundConfig;
import com.example.killsound.util.FileUtil;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomSoundManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("KillSound");

    private final Path soundsDir;
    private final List<SoundFile> availableSounds = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> cachedBuffers = new ConcurrentHashMap<>();
    private final List<Integer> activeSources = new CopyOnWriteArrayList<>();

    public CustomSoundManager(Path soundsDir) {
        this.soundsDir = soundsDir;
    }

    public void initialize() {
        FileUtil.ensureDirectoryExists(soundsDir);
        refreshSounds();
        LOGGER.info("Sound directory initialized: " + soundsDir.toAbsolutePath());
    }

    public synchronized void refreshSounds() {
        clearCachedBuffers();
        cleanStoppedSources();
        availableSounds.clear();

        FileUtil.ensureDirectoryExists(soundsDir);

        List<SoundFile> loaded = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(soundsDir)) {
            for (Path path : stream) {
                if (FileUtil.isValidOggFile(path)) {
                    loaded.add(new SoundFile(path.getFileName().toString(), path));
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to scan sound directory: " + e.getMessage());
        }

        loaded.sort((a, b) -> a.getFilename().compareToIgnoreCase(b.getFilename()));
        availableSounds.addAll(loaded);
        LOGGER.info("Loaded " + availableSounds.size() + " sound files.");
    }

    public List<SoundFile> getAvailableSounds() {
        return Collections.unmodifiableList(availableSounds);
    }

    public List<String> getAvailableSoundNames() {
        List<String> names = new ArrayList<>();
        for (SoundFile soundFile : availableSounds) {
            names.add(soundFile.getFilename());
        }
        return names;
    }

    public boolean isAvailable(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        for (SoundFile soundFile : availableSounds) {
            if (soundFile.getFilename().equalsIgnoreCase(filename)) {
                return true;
            }
        }
        return false;
    }

    public void playEnemyKillSound() {
        KillSoundConfig config = KillSoundClient.getInstance().getConfig();
        if (config.enemyKillEnabled) {
            LOGGER.info("Playing enemy kill sound: " + config.enemyKillSound);
            play(config.enemyKillSound, config.enemyKillVolume, config.enemyKillPitch);
        }
    }

    public void playOwnDeathSound() {
        KillSoundConfig config = KillSoundClient.getInstance().getConfig();
        if (config.ownDeathEnabled) {
            LOGGER.info("Playing own death sound: " + config.ownDeathSound);
            play(config.ownDeathSound, config.ownDeathVolume, config.ownDeathPitch);
        }
    }

    public synchronized void play(String filename, float volume, float pitch) {
        if (filename == null || filename.isBlank()) {
            return;
        }

        Path soundPath = soundsDir.resolve(filename);
        if (!FileUtil.isValidOggFile(soundPath)) {
            LOGGER.warn("Configured sound does not exist or is invalid: " + filename);
            return;
        }

        cleanStoppedSources();

        Integer bufferId = getOrCreateBuffer(soundPath);
        if (bufferId == null) {
            return;
        }

        try {
            int sourceId = AL10.alGenSources();
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                LOGGER.warn("OpenAL error generating source: " + error);
                return;
            }

            float clampedVolume = Math.max(0.0f, Math.min(2.0f, volume));
            float clampedPitch = Math.max(0.5f, Math.min(2.0f, pitch));

            AL10.alSourcei(sourceId, AL10.AL_BUFFER, bufferId);
            AL10.alSourcef(sourceId, AL10.AL_GAIN, clampedVolume);
            AL10.alSourcef(sourceId, AL10.AL_PITCH, clampedPitch);
            AL10.alSourcei(sourceId, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
            AL10.alSourcef(sourceId, AL10.AL_ROLLOFF_FACTOR, 0.0f);
            AL10.alSource3f(sourceId, AL10.AL_POSITION, 0.0f, 0.0f, 0.0f);
            AL10.alSource3f(sourceId, AL10.AL_VELOCITY, 0.0f, 0.0f, 0.0f);
            AL10.alSource3f(sourceId, AL10.AL_DIRECTION, 0.0f, 0.0f, 0.0f);
            AL10.alSourcei(sourceId, AL10.AL_LOOPING, AL10.AL_FALSE);
            AL10.alSourcePlay(sourceId);

            activeSources.add(sourceId);
        } catch (Throwable t) {
            LOGGER.warn("Failed to play sound " + filename + ": " + t.getMessage());
        }
    }

    private Integer getOrCreateBuffer(Path soundPath) {
        String pathKey = soundPath.toAbsolutePath().toString();
        if (cachedBuffers.containsKey(pathKey)) {
            return cachedBuffers.get(pathKey);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer channelsBuffer = stack.mallocInt(1);
            IntBuffer sampleRateBuffer = stack.mallocInt(1);
            ShortBuffer pcm = STBVorbis.stb_vorbis_decode_filename(pathKey, channelsBuffer, sampleRateBuffer);

            if (pcm == null) {
                LOGGER.warn("Invalid OGG file or decoder failed: " + soundPath.getFileName());
                return null;
            }

            int channels = channelsBuffer.get(0);
            int sampleRate = sampleRateBuffer.get(0);
            int format = switch (channels) {
                case 1 -> AL10.AL_FORMAT_MONO16;
                case 2 -> AL10.AL_FORMAT_STEREO16;
                default -> {
                    LOGGER.warn("Unsupported channel count in " + soundPath.getFileName() + ": " + channels);
                    yield -1;
                }
            };
            if (format == -1) {
                return null;
            }

            int bufferId = AL10.alGenBuffers();
            int error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                LOGGER.warn("OpenAL error creating buffer: " + error);
                return null;
            }

            AL10.alBufferData(bufferId, format, pcm, sampleRate);
            error = AL10.alGetError();
            if (error != AL10.AL_NO_ERROR) {
                LOGGER.warn("OpenAL error uploading audio buffer: " + error);
                AL10.alDeleteBuffers(bufferId);
                return null;
            }

            cachedBuffers.put(pathKey, bufferId);
            return bufferId;
        } catch (Throwable t) {
            LOGGER.warn("Error decoding sound " + soundPath.getFileName() + ": " + t.getMessage());
            return null;
        }
    }

    public synchronized void cleanStoppedSources() {
        activeSources.removeIf(sourceId -> {
            int state = AL10.alGetSourcei(sourceId, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED || state == AL10.AL_INITIAL) {
                AL10.alDeleteSources(sourceId);
                return true;
            }
            return false;
        });
    }

    private void clearCachedBuffers() {
        for (int bufferId : cachedBuffers.values()) {
            AL10.alDeleteBuffers(bufferId);
        }
        cachedBuffers.clear();
    }

    public synchronized void stopAll() {
        for (int sourceId : activeSources) {
            AL10.alSourceStop(sourceId);
            AL10.alDeleteSources(sourceId);
        }
        activeSources.clear();
        clearCachedBuffers();
    }

    public void openSoundsFolder() {
        FileUtil.openFolder(soundsDir);
    }

    public Path getSoundsDir() {
        return soundsDir;
    }
}
