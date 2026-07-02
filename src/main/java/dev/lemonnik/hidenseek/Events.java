package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.utils.PermsList;
import dev.lemonnik.hidenseek.utils.World;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.player.*;

public class Events {
    private static final GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
    private static final World spawnWorld = WorldManager.getSpawnWorld();
    private static final Pos spawnPos = WorldManager.getWorldSpawn(spawnWorld.id()) != null ? WorldManager.getWorldSpawn(spawnWorld.id()) : Pos.ZERO ;

    public static void register() {
        globalEventHandler.addListener(PlayerGameModeRequestEvent.class, event -> {
            Player player = event.getPlayer();
            if (player.getPermissionLevel() >= 3) {
                player.setGameMode(event.getRequestedGameMode());
            }
        });

        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            event.setSpawningInstance(spawnWorld.world());
            player.setRespawnPoint(spawnPos);
            player.setGameMode(GameMode.ADVENTURE);
            player.setPermissionLevel(PermsList.getLevel(player.getUuid()));
        });

        globalEventHandler.addListener(PlayerLoadedEvent.class, event -> {
            SessionManager.onPlayerJoin(event.getPlayer());

            Main.announce(Component.text("§e" + event.getPlayer().getUsername() + " joined"));
        });
        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> {
            SessionManager.onPlayerLeave(event.getPlayer());

            Main.announce(Component.text("§e" + event.getPlayer().getUsername() + " left"));
        });
        globalEventHandler.addListener(PlayerUseItemEvent.class, SessionManager::onItemUse);
        globalEventHandler.addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true));
        globalEventHandler.addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));
    }
}
