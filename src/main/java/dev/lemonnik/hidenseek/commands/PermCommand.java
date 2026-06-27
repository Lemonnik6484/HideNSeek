package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.PermsList;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

public class PermCommand extends Command {
    public PermCommand() {
        super("permission", "perm");

        ArgumentEntity playerArgument = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);
        ArgumentInteger levelArgument = (ArgumentInteger) ArgumentType.Integer("level").between(0, 4);

        addSyntax((sender, context) -> {
            EntityFinder finder = context.get(playerArgument);
            List<Entity> entities = finder.find(sender);

            if (entities.isEmpty()) {
                sender.sendMessage("Player not found");
                return;
            }

            Player target = (Player) entities.getFirst();

            int level = context.get(levelArgument);

            if (!(sender instanceof Player player)) {
                target.setPermissionLevel(level);
                PermsList.set(target);
                sender.sendMessage("Set " + target.getUsername() + "'s permission level to " + level);
            } else {
                if (player.getPermissionLevel() > level || player.getPermissionLevel() == 4) {
                    target.setPermissionLevel(level);
                    PermsList.set(target);
                    sender.sendMessage("Set " + target.getUsername() + "'s permission level to " + level);
                } else {
                    sender.sendMessage("You do not have permission to do this");
                }
            }
        }, playerArgument, levelArgument);
    }
}