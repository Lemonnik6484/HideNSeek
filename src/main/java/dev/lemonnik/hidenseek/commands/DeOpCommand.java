package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.OpsList;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.List;

public class DeOpCommand extends Command {
    public DeOpCommand() {
        super("deop");

        ArgumentEntity playerArgument = ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        addSyntax((sender, context) -> {
            if (!OpsList.is(sender.identity().uuid())) return;

            EntityFinder finder = context.get(playerArgument);
            List<Entity> entities = finder.find(sender);

            if (entities.isEmpty()) {
                sender.sendMessage("Player not found");
                return;
            }

            Player target = (Player) entities.getFirst();
            OpsList.remove(target.getUuid());
            sender.sendMessage("Removed " + target.getUsername() + " from op list");
        }, playerArgument);
    }
}