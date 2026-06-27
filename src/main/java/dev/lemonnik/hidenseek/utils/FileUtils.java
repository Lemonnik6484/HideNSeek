package dev.lemonnik.hidenseek.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.lemonnik.hidenseek.Main.error;

public class FileUtils {
    public static void checkDir(String dirStr) {
        Path dir = Path.of(dirStr);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (IOException e) {
                error("Could not create directory: " + dir);
                throw new RuntimeException(e);
            }
        }
    }

    public static void checkFile(String fileStr) {
        Path file = Path.of(fileStr);
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
            } catch (IOException e) {
                error("Could not create file: " + file);
                throw new RuntimeException(e);
            }
        }
    }
}
