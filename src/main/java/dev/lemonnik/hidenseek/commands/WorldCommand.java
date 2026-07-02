package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.World;
import dev.lemonnik.hidenseek.SessionManager;
import dev.lemonnik.hidenseek.WorldManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

public class WorldCommand extends Command {
    public WorldCommand() {
        super("world");

        var worldId = ArgumentType.String("world-id");

        addSyntax((sender, context) -> {
            final String id = context.get(worldId);

            if (!(sender instanceof Player player)) {
                return;
            }

            if (player.getPermissionLevel() < 3) {
                player.sendMessage("You do not have permission to use this command.");
                return;
            }

            World world = WorldManager.getWorld(id);
            if (world == null) {
                player.sendMessage("Unknown world: " + id);
                return;
            }

            SessionManager.teleportToWorld(player, world);
        }, worldId);

    }
}
