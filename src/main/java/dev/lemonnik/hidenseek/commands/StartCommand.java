package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.SessionManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class StartCommand extends Command {
    public StartCommand() {
        super("start", "skip");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player) || player.getPermissionLevel() >= 1) {
                SessionManager.skipIntermission();
            }
        });
    }
}