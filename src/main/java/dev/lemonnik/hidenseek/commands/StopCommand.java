package dev.lemonnik.hidenseek.commands;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop");

        setDefaultExecutor((sender, commandContext) -> {
            if (!(sender instanceof Player player) || player.getPermissionLevel() >= 4) {
                MinecraftServer.getSchedulerManager().scheduleEndOfTick(MinecraftServer::stopCleanly);
            }
        });
    }
}
