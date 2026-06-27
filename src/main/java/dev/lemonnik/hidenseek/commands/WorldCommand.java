package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.WorldManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;

public class WorldCommand extends Command {
    public WorldCommand() {
        super("world");

        var worldId = ArgumentType.String("world-id");

        addSyntax((sender, context) -> {
            final String id = context.get(worldId);

            if (sender instanceof Player player && player.getPermissionLevel() >= 3) {
                InstanceContainer world = WorldManager.getWorld(id);
                if (world != null) {
                    player.setInstance(world);
                }
            }
        }, worldId);

    }
}
