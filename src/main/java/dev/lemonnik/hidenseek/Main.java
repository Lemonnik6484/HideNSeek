package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.commands.Commands;
import dev.lemonnik.hidenseek.utils.AdminsList;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.extras.lan.OpenToLAN;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.anvil.AnvilLoader;
import net.minestom.server.world.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("Server");

    static void main(String[] args) {
        System.setProperty("minestom.chunk-view-distance", "16");
        System.setProperty("minestom.entity-view-distance", "16");

        AdminsList.load();

        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        Commands.registerCommands();

        OpenToLAN.open();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        if (Path.of("worlds/Lobby").toFile().exists()) {
            instanceContainer.setChunkLoader(new AnvilLoader(Path.of("worlds/Lobby"), DimensionType.OVERWORLD.key()));
        } else {
            info("Failed to load " + Path.of("worlds/Lobby").toFile().getAbsolutePath());
        }

        CompletableFuture.runAsync(() -> {
            LightingChunk.relight(instanceContainer, instanceContainer.getChunks());
            instanceContainer.saveChunksToStorage();
        });

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 0, 0));
            player.setGameMode(GameMode.SPECTATOR);

            int spawnChunkX = 0;
            int spawnChunkZ = 0;
            for (int x = spawnChunkX - 2; x <= spawnChunkX + 2; x++) {
                for (int z = spawnChunkZ - 2; z <= spawnChunkZ + 2; z++) {
                    instanceContainer.loadChunk(x, z);
                }
            }
        });

        int port = 25565;
        String address = "0.0.0.0";

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    port = Integer.parseInt(arg.substring("--port=".length()));
                    continue;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port: " + arg);
                    System.exit(1);
                }
            }
            if (arg.startsWith("--address=")) {
                try {
                    address = arg.substring("--address=".length()).trim();
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid address: " + arg);
                    System.exit(1);
                }
            }
        }

        minecraftServer.start(address, port);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void error(String message) {
        logger.error(message);
    }
}
