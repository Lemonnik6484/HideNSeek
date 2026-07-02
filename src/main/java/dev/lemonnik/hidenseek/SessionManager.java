package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.utils.World;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class SessionManager {
    private static final Scheduler SCHEDULER = MinecraftServer.getSchedulerManager();
    private static final BossBar BOSS_BAR = BossBar.bossBar(Component.text("Waiting for players: 2+"), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
    private static final Team GLOBAL_TEAM = MinecraftServer.getTeamManager()
            .createBuilder("hiddenNametags")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build();

    private static final float SEEKER_RATIO = 0.10F;
    private static final int INTERMISSION_DURATION = 20 * 20; // 20sec
    private static final int HIDING_DURATION = 60 * 20; // 1min
    private static final int GAME_DURATION = 7 * 60 * 20; // 7min
    private static final int MIN_PLAYERS = 2;
    private static final int TICKS_PER_SECOND = 20;

    private static State state = State.IDLE;
    private static int ticksPassed = 0;

    private static final Set<Player> hiders = new HashSet<>();
    private static final Set<Player> seekers = new HashSet<>();
    private static final Set<Player> spectators = new HashSet<>();
    private static final Map<Player, Integer> seekerWeights = new HashMap<>();

    private static World currentWorld;

    private static Task gameTickTask;

    private enum State {
        IDLE,
        INTERMISSION,
        HIDING,
        GAME
    }

    public enum PlayerState {
        HIDER,
        SEEKER,
        SPECTATOR
    }

    public static void init() {
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player player && event.getTarget() instanceof Player playerTarget) {
                if (is(PlayerState.SEEKER, player) && is(PlayerState.HIDER, playerTarget)) {
                    hiders.remove(playerTarget);
                    addToSpectator(playerTarget);
                    if (hiders.isEmpty()) {
                        showWin();
                    }
                }
            }
        });
    }

    public static void onPlayerJoin(Player player) {
        player.showBossBar(BOSS_BAR);
        if (isRoundActive()) {
            addToSpectator(player);
            teleportToWorld(player, currentWorld);
        }

        if (shouldStartRound()) {
            startRoundCycle();
        }
    }

    private static void showWin() {
        showTitle(Title.title(Component.text(hiders.isEmpty() ? "Seekers win!" : "Hiders win!"), Component.empty()));
        stopGame();
        state = State.INTERMISSION;
    }

    public static void skipIntermission() {
        if (state == State.INTERMISSION) {
            ticksPassed = INTERMISSION_DURATION - 1;
        }
    }

    private static void showTitle(Title title) {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.showTitle(title);
        }
    }

    private static void addToSpectator(Player player) {
        spectators.add(player);
        player.setGameMode(GameMode.SPECTATOR);
        player.setInvisible(true);
    }

    public static void onPlayerLeave(Player player) {
        hiders.remove(player);
        spectators.remove(player);
        seekers.remove(player);

        if (shouldStopRound()) {
            cancelTickTask();

            state = MinecraftServer.getConnectionManager().getOnlinePlayerCount() < MIN_PLAYERS ? State.IDLE : State.INTERMISSION;

            resetBossBar();
            stopGame();
        }
    }

    private static void setBossBarProgress(float progress) {
        BOSS_BAR.progress(Math.clamp(0, progress, 1));
    }

    private static void initGame() {
        Main.info("======================");
        currentWorld = WorldManager.getRandomWorld();
        Main.info("Selected map: " + currentWorld.id());

        hiders.clear();
        seekers.clear();

        List<Player> onlinePlayers = new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        for (Player player : onlinePlayers) {
            seekerWeights.putIfAbsent(player, 1);
        }

        seekerWeights.keySet().retainAll(onlinePlayers);
        List<Player> weightedPool = new ArrayList<>();
        for (Player player : onlinePlayers) {
            int weight = seekerWeights.get(player);
            for (int i = 0; i < weight; i++) {
                weightedPool.add(player);
            }
        }

        int seekerCount = getSeekerCount(onlinePlayers.size());
        for (int i = 0; i < seekerCount; i++) {
            Player seeker = weightedPool.get(ThreadLocalRandom.current().nextInt(weightedPool.size()));
            weightedPool.remove(seeker);

            seekers.add(seeker);
            seekerWeights.put(seeker, Math.max(seekerWeights.get(seeker) - 1, 1));
        }

        for (Player player : onlinePlayers) {
            if (!seekers.contains(player)) {
                hiders.add(player);
                seekerWeights.put(player, seekerWeights.get(player) + 1);
            }
        }

        Main.info("Hiders: ");
        for (Player player : hiders) {
            Main.info("  - " + player.getUsername());
            player.getInventory().setItemStack(8, createItem(Material.BELL, "§eSounditem"));
            player.showTitle(Title.title(Component.text("You're a hider!"), Component.empty()));
        }

        Main.info("Seekers: ");
        for (Player player : seekers) {
            Main.info("  - " + player.getUsername());
            player.setGlowing(true);
            player.getInventory().setItemStack(8, createItem(Material.CONDUIT, "§eTeleport to spawn"));
            player.showTitle(Title.title(Component.text("You're a seeker!"), Component.empty()));
        }

        teleportToWorld(hiders, currentWorld);
        for (Player player : onlinePlayers) {
            player.setTeam(GLOBAL_TEAM);
        }
        Main.info("======================");
    }

    public static void onItemUse(PlayerUseItemEvent event) {
        Player player = event.getPlayer();
        ItemStack stack = event.getItemStack();

        if (stack.material() == Material.BELL) {
            if (stack.amount() > 1) return;

            player.getInstance().playSound(
                    Sound.sound(
                            Key.key("block.bell.use"),
                            Sound.Source.PLAYER,
                            1f,
                            1f
                    ),
                    player.getPosition().x(),
                    player.getPosition().y(),
                    player.getPosition().z()
            );

            player.getInventory().setItemStack(8, stack.withAmount(30));

            MinecraftServer.getSchedulerManager().submitTask(() -> {
                ItemStack stack2 = player.getInventory().getItemStack(8);
                if (stack2.material() != Material.BELL) {
                    return TaskSchedule.stop();
                }

                player.getInventory().setItemStack(8, stack.withAmount(stack2.amount() - 1));

                return stack2.amount() >= 1 ? TaskSchedule.seconds(1) : TaskSchedule.stop();
            });

            event.setCancelled(true);
        }

        if (stack.material() == Material.CONDUIT) {
            World world = WorldManager.getWorld(player.getInstance());
            if (world != null) {
                Pos pos = WorldManager.getWorldSpawn(world.id());
                if (pos != null) {
                    player.teleport(pos);
                }
            }

            player.playSound(Sound.sound(Key.key("entity.enderman.teleport"), Sound.Source.PLAYER, 1f, 1f));
            event.setCancelled(true);
        }
    }

    private static void teleportToWorld(Collection<? extends Player> group, World world) {
        for (Player player : group) {
            teleportToWorld(player, world);
        }
    }

    public static void teleportToWorld(Player player, World world) {
        player.setInstance(world.world()).thenRun(() -> {
            Pos pos = WorldManager.getWorldSpawn(world.id());
            if (pos != null) player.teleport(pos);
        });
    }

    private static void stopGame() {
        World spawnWorld = WorldManager.getSpawnWorld();
        teleportToWorld(MinecraftServer.getConnectionManager().getOnlinePlayers(), spawnWorld);
        hiders.clear();
        resetPlayers();
        ticksPassed = 0;
        seekers.clear();
        spectators.clear();
    }

    private static void resetPlayers() {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setGlowing(false);
            player.setInvisible(false);
            player.getInventory().clear();
        }
    }

    private static boolean is(PlayerState state, Player player) {
        return switch (state) {
            case HIDER -> hiders.contains(player);
            case SEEKER -> seekers.contains(player);
            case SPECTATOR -> spectators.contains(player);
        };
    }

    private static boolean isRoundActive() {
        return state != State.INTERMISSION && state != State.IDLE;
    }

    private static boolean shouldStartRound() {
        return state == State.IDLE && MinecraftServer.getConnectionManager().getOnlinePlayerCount() >= MIN_PLAYERS;
    }

    private static boolean shouldStopRound() {
        return MinecraftServer.getConnectionManager().getOnlinePlayerCount() < MIN_PLAYERS || hiders.isEmpty() || seekers.isEmpty();
    }

    private static void startRoundCycle() {
        state = State.INTERMISSION;
        gameTickTask = SCHEDULER.submitTask(SessionManager::tickRound);
    }

    private static TaskSchedule tickRound() {
        switch (state) {
            case INTERMISSION -> tickPhase(
                    "Intermission",
                    INTERMISSION_DURATION,
                    BossBar.Color.GREEN,
                    SessionManager::beginHiding
            );
            case HIDING -> tickPhase(
                    "Hiding",
                    HIDING_DURATION,
                    BossBar.Color.YELLOW,
                    SessionManager::beginGame
            );
            case GAME -> tickPhase(
                    "Game",
                    GAME_DURATION,
                    BossBar.Color.RED,
                    SessionManager::finishGame
            );
            case IDLE -> {
                return TaskSchedule.stop();
            }
        }

        return TaskSchedule.tick(1);
    }

    private static void tickPhase(String label, int duration, BossBar.Color color, Runnable onComplete) {
        ticksPassed++;
        int secondsLeft = Math.max(0, (duration - ticksPassed) / TICKS_PER_SECOND);

        BOSS_BAR.name(Component.text(label + ": " + secondsLeft + "s left"));
        BOSS_BAR.color(color);
        setBossBarProgress((float) ticksPassed / duration);

        if (ticksPassed >= duration) {
            ticksPassed = 0;
            onComplete.run();
        }
    }

    private static void beginHiding() {
        state = State.HIDING;
        initGame();
    }

    private static void beginGame() {
        state = State.GAME;
        teleportToWorld(seekers, currentWorld);
    }

    private static void finishGame() {
        showWin();
        state = State.INTERMISSION;
    }

    private static void cancelTickTask() {
        if (gameTickTask != null) {
            gameTickTask.cancel();
            gameTickTask = null;
        }
    }

    private static void resetBossBar() {
        BOSS_BAR.color(BossBar.Color.BLUE);
        BOSS_BAR.progress(1);
        BOSS_BAR.name(Component.text("Waiting for players: " + MIN_PLAYERS + "+"));
    }

    private static ItemStack createItem(Material material, String name) {
        return ItemStack.builder(material)
                .customName(Component.text(name))
                .build();
    }

    private static int getSeekerCount(int playerCount) {
        return Math.max(1, (int) Math.ceil(playerCount * SEEKER_RATIO));
    }
}
