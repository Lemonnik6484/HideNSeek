package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.World;
import dev.lemonnik.hidenseek.utils.WorldManager;
import net.minestom.server.command.builder.Command;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

public class SetSpawnCommand extends Command {
    public SetSpawnCommand() {
        super("setspawn");

        setDefaultExecutor((sender, command) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command");
            }

            if (sender instanceof Player player) {
                World world = WorldManager.getWorld(player.getInstance());
                if (world != null) {
                    Pos pos = player.getPosition();
                    WorldManager.setWorldSpawn(world.id(), new Pos(
                            Math.floor(pos.x()) + 0.5,
                            Math.floor(pos.y()),
                            Math.floor(pos.z()) + 0.5
                    ));
                }
            }
        });
    }
}
