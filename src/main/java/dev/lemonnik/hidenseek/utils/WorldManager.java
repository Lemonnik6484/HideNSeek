package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;
import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class WorldManager {
    private static final LinkedHashMap<String, InstanceContainer> worlds = new LinkedHashMap<>();
    private static String spawnWorldId = "lobby";

    public static void loadWorlds() {
        for (File file : Objects.requireNonNull(new File("worlds").listFiles())) {
            if (file.isDirectory()) {
                InstanceManager instanceManager = MinecraftServer.getInstanceManager();
                InstanceContainer instanceContainer = instanceManager.createInstanceContainer(new AnvilLoader(Path.of("worlds/" + file.getName()), DimensionType.OVERWORLD.key()));

                CompletableFuture.runAsync(() -> {
                    LightingChunk.relight(instanceContainer, instanceContainer.getChunks());
                    instanceContainer.saveChunksToStorage();
                });

                String id = fixId(file.getName());

                worlds.put(id, instanceContainer);
                Main.info("Loaded world " + id);
            }
        }

        spawnWorldId = System.getProperty("lobby") != null ? System.getProperty("lobby") : "lobby";
        Main.info("Set spawn world to " + spawnWorldId);
    }

    private static String fixId(String id) {
        return id.trim().toLowerCase().replace(" ", "_");
    }

    public static InstanceContainer getWorld(String id) {
        return worlds.get(fixId(id));
    }

    public static InstanceContainer getSpawnWorld() {
        if (worlds.containsKey(spawnWorldId)) {
            return worlds.get(spawnWorldId);
        } else {
            throw new RuntimeException("Could not find world with id " + spawnWorldId);
        }
    }
}
