package dev.lemonnik.hidenseek.commands;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandExecutor;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.number.ArgumentDouble;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public class TeleportCommand extends Command {
    public TeleportCommand() {
        super("teleport", "tp");

        ArgumentDouble xArgument = ArgumentType.Double("x");
        ArgumentDouble yArgument = ArgumentType.Double("y");
        ArgumentDouble zArgument = ArgumentType.Double("z");

        addSyntax((sender, ctx) -> {
            if (!(sender instanceof Player player) || (player.getPermissionLevel() < 3)) return;

            double x = ctx.get(xArgument);
            double y = ctx.get(yArgument);
            double z = ctx.get(zArgument);

            player.teleport(player.getPosition().withCoord(x, y, z));
            player.sendMessage("Teleported to %s %s %s".formatted(x, y, z));
        }, xArgument, yArgument, zArgument);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            player.sendMessage("You are a MORON!!!!!!!!!");
        });
    }
}
