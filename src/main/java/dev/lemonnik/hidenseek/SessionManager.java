package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.utils.World;
import dev.lemonnik.hidenseek.utils.WorldManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.scoreboard.Team;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SessionManager {
    private static final Scheduler scheduler = MinecraftServer.getSchedulerManager();
    private static final BossBar BOSS_BAR = BossBar.bossBar(Component.text("Waiting for players: 2+"), 1, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

    private static final Team globalTeam = MinecraftServer.getTeamManager()
            .createBuilder("hiddenNametags")
            .nameTagVisibility(TeamsPacket.NameTagVisibility.NEVER)
            .build();

    private static final int intermissionDuration = 20 * 20; // 1.5min
    private static final int hidingDuration = 15 * 20; // 1min
    private static final int gameDuration = 1 * 60 * 20; // 7min
    private static int ticksPassed = 0;

    private static State state = State.IDLE;

    private static final ArrayList<Player> hiders = new ArrayList<>();
    private static final ArrayList<Player> seekers = new ArrayList<>();
    private static final ArrayList<Player> spectators = new ArrayList<>();

    private static World currentWorld;

    private static Task task;

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
        MinecraftServer.getGlobalEventHandler().addListener(EntityAttackEvent .class, event -> {
            if (event.getEntity() instanceof Player player && event.getTarget() instanceof Player playerTarget) {
                if (SessionManager.is(SessionManager.PlayerState.SEEKER, player) && SessionManager.is(SessionManager.PlayerState.HIDER, playerTarget)) {
                    hiders.remove(playerTarget);
                    addToSpectator(playerTarget);
                    if (hiders.isEmpty()) {
                        checkWin();
                    }
                }
            }
        });
    }

    public static void onPlayerJoin(Player player) {
        player.showBossBar(BOSS_BAR);
        if (state != State.INTERMISSION && state != State.IDLE) {
            addToSpectator(player);
            teleportToWorld(player, currentWorld);
        }

        if (MinecraftServer.getConnectionManager().getOnlinePlayerCount() > 1 && state == State.IDLE) {
            state = State.INTERMISSION;
            task = scheduler.submitTask(() -> {
                int secondsLeft;
                switch (state) {
                    case INTERMISSION:
                        ticksPassed++;
                        secondsLeft = (intermissionDuration - ticksPassed) / 20;
                        BOSS_BAR.name(Component.text("Intermission: " + secondsLeft + "s left"));
                        setBossBarProgress((float) ticksPassed / intermissionDuration);
                        BOSS_BAR.color(BossBar.Color.GREEN);
                        if (ticksPassed >= intermissionDuration) {
                            state = State.HIDING;
                            ticksPassed = 0;
                            initGame();
                        }
                        break;
                    case HIDING:
                        ticksPassed++;
                        secondsLeft = (hidingDuration - ticksPassed) / 20;
                        BOSS_BAR.name(Component.text("Hiding: " + secondsLeft + "s left"));
                        setBossBarProgress((float) ticksPassed / hidingDuration);
                        BOSS_BAR.color(BossBar.Color.YELLOW);
                        if (ticksPassed >= hidingDuration) {
                            state = State.GAME;
                            ticksPassed = 0;
                            teleportToWorld(seekers, currentWorld);
                        }
                        break;
                    case GAME:
                        ticksPassed++;
                        secondsLeft = (gameDuration - ticksPassed) / 20;
                        BOSS_BAR.name(Component.text("Game: " + secondsLeft+ "s left"));
                        setBossBarProgress((float) ticksPassed / gameDuration);
                        BOSS_BAR.color(BossBar.Color.RED);
                        if (ticksPassed >= gameDuration) {
                            checkWin();
                            state = State.INTERMISSION;
                        }
                        break;
                }

                return TaskSchedule.tick(1);
            });
        }
    }

    private static void checkWin() {
        if (!hiders.isEmpty()) {
            showTitle(Title.title(Component.text("Hiders win!"), Component.empty()));
        } else {
            showTitle(Title.title(Component.text("Seekers win!"), Component.empty()));
        }
        stopGame();
        state = State.INTERMISSION;
    }

    public static void skipIntermission() {
        if (state == State.INTERMISSION) {
            ticksPassed = intermissionDuration - 1;
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
        if (MinecraftServer.getConnectionManager().getOnlinePlayerCount() < 2 || hiders.isEmpty() || seekers.isEmpty()) {
            if (task != null) {
                task.cancel();
            }
            if (MinecraftServer.getConnectionManager().getOnlinePlayerCount() < 2) {
                state = State.IDLE;
            } else {
                state = State.INTERMISSION;
            }
            BOSS_BAR.color(BossBar.Color.BLUE);
            BOSS_BAR.progress(1);
            BOSS_BAR.name(Component.text("Waiting for players: 2+"));
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

        List<Player> players = new ArrayList<>(MinecraftServer.getConnectionManager().getOnlinePlayers());
        Collections.shuffle(players);

        int hiderCount = Math.min(players.size() - 1, (int) Math.ceil(players.size() * 0.9));

        for (int i = 0; i < players.size(); i++) {
            if (i < hiderCount) {
                hiders.add(players.get(i));
            } else {
                seekers.add(players.get(i));
            }
        }

        Main.info("Hiders: ");
        for (Player player : hiders) {
            Main.info("  - " + player.getUsername());
        }

        Main.info("Seekers: ");
        for (Player player : seekers) {
            Main.info("  - " + player.getUsername());
            player.setGlowing(true);
        }

        teleportToWorld(hiders, currentWorld);
        for (Player player : players) {
            player.setTeam(globalTeam);
        }
        Main.info("======================");
    }

    private static void teleportToWorld(List<Player> group, World world) {
        for (Player player : group) {
            teleportToWorld(player, world);
        }
    }

    private static void teleportToWorld(Player player, World world) {
        player.setInstance(world.world()).thenRun(() -> {
            Pos pos = WorldManager.getWorldSpawn(world.id());
            if (pos != null) player.teleport(pos);
        });
    }

    private static void stopGame() {
        World spawnWorld = WorldManager.getSpawnWorld();
        teleportToWorld(MinecraftServer.getConnectionManager().getOnlinePlayers().stream().toList(), spawnWorld);
        hiders.clear();
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setGlowing(false);
            player.setInvisible(false);
        }
        ticksPassed = 0;
        seekers.clear();
        spectators.clear();
    }

    private static boolean is(PlayerState state, Player player) {
        return switch (state) {
            case HIDER -> hiders.contains(player);
            case SEEKER -> seekers.contains(player);
            case SPECTATOR -> spectators.contains(player);
        };
    }
}
