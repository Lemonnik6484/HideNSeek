package dev.lemonnik.hidenseek.commands;

import net.minestom.server.MinecraftServer;

public class Commands {
    public static void registerCommands() {
        MinecraftServer.getCommandManager().register(new StopCommand());
        MinecraftServer.getCommandManager().register(new AdminCommand());
        MinecraftServer.getCommandManager().register(new UnAdminCommand());
    }
}
