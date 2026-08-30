package com.example.killsound.audio;

import java.nio.file.Path;
import java.util.Objects;

public class SoundFile {

    private final String filename;
    private final Path path;

    public SoundFile(String filename, Path path) {
        this.filename = filename;
        this.path = path;
    }

    public String getFilename() {
        return filename;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public String toString() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SoundFile soundFile = (SoundFile) o;
        return Objects.equals(filename, soundFile.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename);
    }
}
