package com.example.killsound.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger("KillSound");
    private static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024;

    public static void ensureDirectoryExists(Path directory) {
        if (directory == null) {
            return;
        }
        try {
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create directory: " + e.getMessage());
        }
    }

    public static boolean isValidOggFile(Path path) {
        if (path == null) {
            return false;
        }
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return false;
        }
        if (!path.getFileName().toString().toLowerCase().endsWith(".ogg")) {
            return false;
        }
        if (!Files.isReadable(path)) {
            return false;
        }
        try {
            long size = Files.size(path);
            return size > 0 && size <= MAX_FILE_SIZE_BYTES;
        } catch (IOException e) {
            return false;
        }
    }

    public static void openFolder(Path directory) {
        if (directory == null) {
            return;
        }
        ensureDirectoryExists(directory);
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(directory.toFile());
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    new ProcessBuilder("explorer.exe", directory.toAbsolutePath().toString()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", directory.toAbsolutePath().toString()).start();
                } else {
                    new ProcessBuilder("xdg-open", directory.toAbsolutePath().toString()).start();
                }
            }
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            LOGGER.warn("Failed to open directory: " + e.getMessage());
        }
    }
}
