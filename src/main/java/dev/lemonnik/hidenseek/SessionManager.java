package dev.lemonnik.hidenseek;

import dev.lemonnik.hidenseek.utils.WorldManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SessionManager {
    private static final Scheduler scheduler = MinecraftServer.getSchedulerManager();
    private static final BossBar BOSS_BAR = BossBar.bossBar(Component.text("Timer"), 1, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

    private static final int intermissionDuration = 90 * 20; // 1.5min
    private static final int hidingDuration = 60 * 20; // 1min
    private static final int gameDuration = 7 * 60 * 20; // 7min

    private static int ticksPassed = 0;

    private static State state = State.IDLE;

    private static final ArrayList<Player> hiders = new ArrayList<>();
    private static final ArrayList<Player> seekers = new ArrayList<>();

    private static InstanceContainer currentInstance;

    private static Task task;

    private enum State {
        IDLE,
        INTERMISSION,
        HIDING,
        GAME
    }

    public static void onPlayerJoin(Player player) {
        player.showBossBar(BOSS_BAR);
        if (MinecraftServer.getConnectionManager().getOnlinePlayerCount() > 1 && state == State.IDLE) {
            state = State.INTERMISSION;
            task = scheduler.submitTask(() -> {
                int secondsLeft;
                switch (state) {
                    case INTERMISSION:
                        ticksPassed++;
                        secondsLeft = (intermissionDuration - ticksPassed) / 20;
                        BOSS_BAR.name(Component.text("Intermission: " + secondsLeft + "s left"));
                        BOSS_BAR.progress((float) intermissionDuration / ticksPassed);
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
                        BOSS_BAR.progress((float) hidingDuration / ticksPassed);
                        BOSS_BAR.color(BossBar.Color.YELLOW);
                        if (ticksPassed >= hidingDuration) {
                            state = State.GAME;
                            ticksPassed = 0;
                            releaseSeekers();
                        }
                        break;
                    case GAME:
                        ticksPassed++;
                        secondsLeft = (gameDuration - ticksPassed) / 20;
                        BOSS_BAR.name(Component.text("Game: " + secondsLeft+ "s left"));
                        BOSS_BAR.progress((float) gameDuration / ticksPassed);
                        BOSS_BAR.color(BossBar.Color.RED);
                        if (ticksPassed >= gameDuration) {
                            state = State.INTERMISSION;
                            ticksPassed = 0;
                            stopGame();
                        }
                        break;
                }

                return TaskSchedule.tick(1);
            });
        }
    }

    public static void onPlayerLeave(Player player) {
        if (MinecraftServer.getConnectionManager().getOnlinePlayerCount() < 2 || hiders.isEmpty() || seekers.isEmpty()) {
            task.cancel();
            state = State.IDLE;
            ticksPassed = 0;
            hiders.clear();
            seekers.clear();
        }
    }

    private static void initGame() {
        currentInstance = WorldManager.getRandomWorld();

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

        for (Player player : hiders) {
            player.setInstance(currentInstance);
            // TODO: set coords too
        }
    }

    private static void releaseSeekers() {
        for (Player player : seekers) {
            player.setInstance(currentInstance);
            // TODO: coords
        }
    }

    private static void stopGame() {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            InstanceContainer spawn = WorldManager.getSpawnWorld();
            player.setInstance(spawn);
            hiders.clear();
            seekers.clear();
            state = State.IDLE;
        }
    }
}
