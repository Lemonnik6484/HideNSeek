package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class OpsList {
    private static List<UUID> ops = new ArrayList<>();
    private static final String file = "ops.txt";

    public static void add(UUID uuid) {
        ops.add(uuid);
        AdminsList.add(uuid);
        save();
    }

    public static void remove(UUID uuid) {
        ops.remove(uuid);
        AdminsList.remove(uuid);
        save();
    }

    public static List<UUID> getAll() {
        return ops;
    }

    public static boolean is(UUID uuid) {
        return ops.contains(uuid);
    }

    public static void load() {
        FileUtils.checkFile(file);

        try {
            List<UUID> loaded = new ArrayList<>();

            for (String line : Files.readAllLines(Path.of(file))) {
                loaded.add(UUID.fromString(line));
            }

            ops = loaded.stream().toList();
        } catch (IOException e) {
            Main.warn("Failed to load ops list: " + e.getMessage());
        }
    }

    public static void save() {
        FileUtils.checkFile(file);

        try {
            Files.write(
                    Path.of(file),
                    ops.stream()
                            .map(UUID::toString)
                            .toList()
            );
        } catch (IOException e) {
            Main.warn("Failed to save ops list: " + e.getMessage());
        }
    }
}
