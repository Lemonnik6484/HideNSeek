package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.sql.SQLManager;
import dev.lemonnik.hidenseek.utils.BadPractices;
import net.kyori.adventure.text.Component;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.extras.lan.OpenToLAN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger("Server");
    private static final int DEFAULT_PORT = 25565;
    private static final String DEFAULT_ADDRESS = "0.0.0.0";

    static void main(String[] args) {
        BadPractices.yum(SQLManager::init);

        MinecraftServer minecraftServer = MinecraftServer.init(new Auth.Online());
        Commands.registerCommands();
        OpenToLAN.open();
        WorldManager.load();
        SessionManager.init();
        Events.register();

        int port = getIntProperty("port", DEFAULT_PORT);
        String address = getStringProperty("address", DEFAULT_ADDRESS);

        minecraftServer.start(address, port);
        info("Server started on " + address + ":" + port);

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

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    private static String getStringProperty(String key, String defaultValue) {
        String value = System.getProperty(key);
        return value != null ? value : defaultValue;
    }

    public static void announce(Component component) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.sendMessage(component);
        }
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
