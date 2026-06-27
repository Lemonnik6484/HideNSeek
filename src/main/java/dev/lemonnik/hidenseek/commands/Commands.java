package dev.lemonnik.hidenseek.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

import java.util.ArrayList;
import java.util.List;

public class Commands {
    private static final List<Command> commands = new ArrayList<>();

    static {
        commands.add(new StopCommand());
        commands.add(new OpCommand());
        commands.add(new DeOpCommand());
        commands.add(new AdminCommand());
        commands.add(new UnAdminCommand());
    }

    public static void registerCommands() {
        for (Command command : commands) {
            MinecraftServer.getCommandManager().register(command);
        }
    }
}