package dev.lemonnik.hidenseek.commands;

import dev.lemonnik.hidenseek.utils.AdminsList;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentEntity;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;

import java.util.ArrayList;
import java.util.List;

public class UnAdminCommand extends Command {
    public UnAdminCommand() {
        super("unadmin");

        ArgumentEntity playerArgument = (ArgumentEntity) ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true);

        addSyntax((sender, context) -> {
            if (!AdminsList.isAdmin(sender.identity().uuid())) return;

            EntityFinder finder = context.get(playerArgument);
            List<Entity> entities = finder.find(sender);

            if (entities.isEmpty()) {
                sender.sendMessage("Player not found");
                return;
            }

            Player target = (Player) entities.getFirst();
            AdminsList.addAdmin(target.getUuid());
            sender.sendMessage("Added " + target.getUsername() + " to admin list");
        }, playerArgument);
    }
}