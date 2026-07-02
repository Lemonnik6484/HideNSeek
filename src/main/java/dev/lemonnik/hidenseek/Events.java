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
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockFace;

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
        globalEventHandler.addListener(PlayerBlockInteractEvent.class, Events::onBlockInteract);
    }

    private static void onBlockInteract(PlayerBlockInteractEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) return;

        Block block = event.getBlock();
        if (!isDoorOrTrapdoor(block)) {
            return;
        }

        String open = block.getProperty("open");
        if (open == null) {
            return;
        }

        boolean nextOpen = !Boolean.parseBoolean(open);
        Block toggledBlock = block.withProperty("open", Boolean.toString(nextOpen));
        event.getInstance().setBlock(event.getBlockPosition(), toggledBlock, true);

        if (block.key().asString().endsWith("_door")) {
            String half = block.getProperty("half");
            BlockFace direction = "upper".equals(half) ? BlockFace.BOTTOM : BlockFace.TOP;
            Block partnerBlock = event.getInstance().getBlock(event.getBlockPosition().relative(direction));

            if (partnerBlock.key().equals(block.key())) {
                event.getInstance().setBlock(
                        event.getBlockPosition().relative(direction),
                        partnerBlock.withProperty("open", Boolean.toString(nextOpen)),
                        true
                );
            }
        }

        event.setBlockingItemUse(true);
        event.setCancelled(true);
    }

    private static boolean isDoorOrTrapdoor(Block block) {
        String key = block.key().asString();
        return key.endsWith("_door") || key.endsWith("_trapdoor");
    }
}
