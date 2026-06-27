package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.AdminsList;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;

public class StopCommand extends Command {
    public StopCommand() {
        super("stop");

        setDefaultExecutor((sender, commandContext) -> {
            if (AdminsList.is(sender.identity().uuid())) {
                MinecraftServer.getSchedulerManager().scheduleEndOfTick(MinecraftServer::stopCleanly);
            }
        });
    }
}
