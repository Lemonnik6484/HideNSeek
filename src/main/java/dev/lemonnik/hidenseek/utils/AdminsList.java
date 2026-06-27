package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminsList {
    private static List<UUID> admins = new ArrayList<>();
    private static final String file = "admins.txt";

    public static void addAdmin(UUID uuid) {
        admins.add(uuid);
        save();
    }

    public static void removeAdmin(UUID uuid) {
        admins.remove(uuid);
        save();
    }

    public static List<UUID> getAdmins() {
        return admins;
    }

    public static boolean isAdmin(UUID uuid) {
        return admins.contains(uuid);
    }

    public static void load() {
        FileUtils.checkFile(file);

        try {
            List<UUID> loaded = new ArrayList<>();

            for (String line : Files.readAllLines(Path.of(file))) {
                loaded.add(UUID.fromString(line));
            }

            admins = loaded.stream().toList();
        } catch (IOException e) {
            Main.warn("Failed to load admins list: " + e.getMessage());
        }
    }

    public static void save() {
        FileUtils.checkFile(file);

        try {
            Files.write(
                    Path.of(file),
                    admins.stream()
                            .map(UUID::toString)
                            .toList()
            );
        } catch (IOException e) {
            Main.warn("Failed to save admins list: " + e.getMessage());
        }
    }
}
