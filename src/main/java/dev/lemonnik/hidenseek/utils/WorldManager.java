package dev.lemonnik.hidenseek.utils;

import dev.lemonnik.hidenseek.Main;
import dev.lemonnik.hidenseek.sql.QueryBuilder;
import dev.lemonnik.hidenseek.sql.SQLManager;
import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWriter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class WorldManager {
    private static final ArrayList<World> worlds = new ArrayList<>();
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

            worlds.add(new World(id, instanceContainer));
            Main.info("Loaded world " + id);
        }

        Main.info("Set spawn world to " + spawnWorldId);
    }

    public static @Nullable World getWorld(Instance instance) {
        return worlds.stream().filter(world -> world.world()
                .equals(instance))
                .findFirst()
                .orElse(null);
    }

    private static String fixId(String id) {
        return id.trim().toLowerCase().replace(" ", "_");
    }

    public static void setWorldSpawn(String id, Pos spawn) {
        BadPractices.yum(() -> SQLManager.insertOrUpdate("spawns", List.of(
                SQLManager.ROW_WORLD_ID,
                SQLManager.ROW_X,
                SQLManager.ROW_Y,
                SQLManager.ROW_Z
        ), List.of(
                id,
                spawn.x(),
                spawn.y(),
                spawn.z()
        )));
    }

    public static @Nullable Pos getWorldSpawn(String id) {
        return BadPractices.interrogate(() -> {
            var select = SQLManager.conn.prepareStatement(QueryBuilder.select("spawns", List.of(
                    SQLManager.ROW_X,
                    SQLManager.ROW_Y,
                    SQLManager.ROW_Z
            ), List.of(SQLManager.ROW_WORLD_ID)));
            select.setString(1, id);

            ResultSet result = select.executeQuery();
            if (!result.next()) return null;

            return new Pos(
                    result.getDouble(SQLManager.ROW_X.name()),
                    result.getDouble(SQLManager.ROW_Y.name()),
                    result.getDouble(SQLManager.ROW_Z.name())
            );
        });
    }

    public static @Nullable World getWorld(String id) {
        return worlds.stream().filter(world -> world.id()
                .equals(fixId(id)))
                .findFirst()
                .orElse(null);
    }

    public static World getRandomWorld() {
        List<World> candidates = worlds.stream()
                .filter(world -> !world.id().equals(spawnWorldId))
                .toList();

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No worlds available besides the spawn world");
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public static World getSpawnWorld() {
        World world = getWorld(spawnWorldId);
        if (world != null) {
            return world;
        } else {
            throw new RuntimeException("Could not find world with id " + spawnWorldId);
        }
    }
}
