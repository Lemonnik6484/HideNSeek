package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.commands.Commands;
import dev.lemonnik.hidenseek.utils.AdminsList;
import dev.lemonnik.hidenseek.utils.OpsList;
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

    static void main() {
        AdminsList.load();
        OpsList.load();

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
        });

        int port = System.getProperty("port") != null ? Integer.parseInt(System.getProperty("port")) : 25565;
        String address = System.getProperty("address") != null ? System.getProperty("address") : "0.0.0.0";

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
