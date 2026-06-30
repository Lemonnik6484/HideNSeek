package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWriter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class WorldManager {
    private static final LinkedHashMap<String, InstanceContainer> worlds = new LinkedHashMap<>();
    private static final String spawnWorldId = System.getProperty("lobby") != null ? System.getProperty("lobby") : "lobby";

    public static enum WorldType {
        ANVIL,
        POLAR
    }

    public static void loadWorlds() {
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        WorldType type = null;

        for (File file : Objects.requireNonNull(new File("worlds").listFiles())) {
            InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

            if (file.isDirectory() && Files.exists(file.toPath().resolve("level.dat"))) {
                type = WorldType.ANVIL;
            } else if (file.getName().endsWith(".polar")) {
                type = WorldType.POLAR;
            }

            Path path = Path.of("worlds/" + file.getName());

            switch (type) {
                case ANVIL: {
                    try {
                        var polarWorld = AnvilPolar.anvilToPolar(path);
                        var polarWorldBytes = PolarWriter.write(polarWorld);

                        Files.write(Path.of("worlds/" + file.getName() + ".polar"), polarWorldBytes);
                        type = WorldType.POLAR;
                    } catch (IOException e) {
                        Main.error(String.format("Failed to convert %s to .polar. Reason: %s\nFalling back to Anvil!", file.getName(), e.getMessage()));
                        instanceContainer.setChunkLoader(new AnvilLoader(path, DimensionType.OVERWORLD.key()));
                        break;
                    }
                }
                case POLAR: {
                    try {
                        instanceContainer.setChunkLoader(new PolarLoader(path));
                    } catch (IOException e) {
                        Main.error(String.format("Failed to load world %s. Reason: %s", file.getName(), e.getMessage()));
                        throw new RuntimeException(e);
                    }
                    break;
                }
                case null:
                    break;

            }

            CompletableFuture.runAsync(() -> {
                LightingChunk.relight(instanceContainer, instanceContainer.getChunks());
                instanceContainer.saveChunksToStorage();
            });

            String id = fixId(file.getName());

            worlds.put(id, instanceContainer);
            Main.info("Loaded world " + id);
        }

        Main.info("Set spawn world to " + spawnWorldId);
    }

    private static String fixId(String id) {
        return id.trim().toLowerCase().replace(" ", "_");
    }

    public static InstanceContainer getWorld(String id) {
        return worlds.get(fixId(id));
    }

    public static InstanceContainer getRandomWorld() {
        List<InstanceContainer> candidates = worlds.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(spawnWorldId))
                .map(Map.Entry::getValue)
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No worlds available besides the spawn world");
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public static InstanceContainer getSpawnWorld() {
        if (worlds.containsKey(spawnWorldId)) {
            return worlds.get(spawnWorldId);
        } else {
            throw new RuntimeException("Could not find world with id " + spawnWorldId);
        }
    }
}
