package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;
import net.minestom.server.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PermsList {
    private static ArrayList<PermLevelPlayer> list = new ArrayList<>();
    private static final String file = "permissions.csv";

    public static void set(PermLevelPlayer player) {
        if (player.level != 0 && !list.contains(player)) {
            add(player);
        } else if (player.level == 0 && list.contains(player)) {
            remove(player);
        }
    }

    public static void set(Player player) {
        set(new PermLevelPlayer(
                player.getUsername(),
                player.getUuid(),
                player.getPermissionLevel()
        ));
    }

    private static void add(PermLevelPlayer player) {
        list.add(player);
        save();
    }

    private static void remove(PermLevelPlayer player) {
        list.remove(player);
        save();
    }

    public static int getLevel(UUID uuid) {
        for (PermLevelPlayer player : list) {
            if (player.uuid.equals(uuid)) {
                return player.level;
            }
        }
        return 0;
    }

    public static void load() {
        FileUtils.checkFile(file);

        try {
            List<PermLevelPlayer> loaded = new ArrayList<>();

            for (String line : Files.readAllLines(Path.of(file))) {
                String[] split = line.split(",");

                loaded.add(new PermLevelPlayer(
                        split[0],
                        UUID.fromString(split[1]),
                        Integer.parseInt(split[2])
                ));
            }

            list.clear();
            list.addAll(loaded);
        } catch (IOException e) {
            Main.warn("Failed to load list list: " + e.getMessage());
        }
    }

    public static void save() {
        FileUtils.checkFile(file);

        try {
            List<String> lines = new ArrayList<>();

            for (PermLevelPlayer player : list) {
                lines.add(player.username + "," + player.uuid.toString() + "," + player.level);
            }

            Files.write(Path.of(file), lines);
        } catch (IOException e) {
            Main.warn("Failed to save list list: " + e.getMessage());
        }
    }

    public record PermLevelPlayer(String username, UUID uuid, int level) {}
}
