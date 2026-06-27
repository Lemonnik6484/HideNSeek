package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.commands.PermCommand;
import dev.lemonnik.hidenseek.commands.StopCommand;
import dev.lemonnik.hidenseek.commands.WorldCommand;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

import java.util.ArrayList;
import java.util.List;

public class Commands {
    private static final List<Command> commands = new ArrayList<>();

    static {
        commands.add(new StopCommand());
        commands.add(new PermCommand());
        commands.add(new WorldCommand());
    }

    public static void registerCommands() {
        for (Command command : commands) {
            MinecraftServer.getCommandManager().register(command);
        }
    }
}