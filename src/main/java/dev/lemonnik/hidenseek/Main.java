package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.sql.SQLManager;
import dev.lemonnik.hidenseek.utils.BadPractices;
import dev.lemonnik.hidenseek.utils.PermsList;
import dev.lemonnik.hidenseek.utils.World;
import dev.lemonnik.hidenseek.utils.WorldManager;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerGameModeRequestEvent;
import net.minestom.server.event.player.PlayerLoadedEvent;
import net.minestom.server.extras.lan.OpenToLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("Server");

    static void main(String[] args) {
        BadPractices.yum(SQLManager::init);

        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());

        Commands.registerCommands();

        OpenToLAN.open();

        WorldManager.loadWorlds();

        World spawnWorld = WorldManager.getSpawnWorld();
        Pos spawnPos = WorldManager.getWorldSpawn(spawnWorld.id());
        if (spawnPos == null) {
            spawnPos = Pos.ZERO;
        }

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        Pos finalSpawnPos = spawnPos;
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(spawnWorld.world());
            player.setRespawnPoint(finalSpawnPos);
            player.setGameMode(GameMode.ADVENTURE);
            player.setPermissionLevel(PermsList.getLevel(player.getUuid()));
        });

        globalEventHandler.addListener(PlayerLoadedEvent.class, event -> {
            final Player player = event.getPlayer();
            SessionManager.onPlayerJoin(player);
        });

        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
           SessionManager.onPlayerLeave(event.getPlayer());
        });

        globalEventHandler.addListener(PlayerGameModeRequestEvent.class, event -> {
            final Player player = event.getPlayer();
            if (player.getPermissionLevel() >= 3) {
                player.setGameMode(event.getRequestedGameMode());
            }
        });

        int port = System.getProperty("port") != null ? Integer.parseInt(System.getProperty("port")) : 25575;
        String address = System.getProperty("address") != null ? System.getProperty("address") : "0.0.0.0";

        minecraftServer.start(address, port);

        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                MinecraftServer.getCommandManager().execute(MinecraftServer.getCommandManager().getConsoleSender(), line);
            }
        }, "console-thread");

        consoleThread.setDaemon(true);
        consoleThread.start();
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
