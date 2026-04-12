package io.github.homeforge.utils;

import io.github.homeforge.HomeForge;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Thread-safe scheduler utility compatible with both Paper and Folia.
 *
 * <p>Paper 1.19.4+ ships the same scheduler API that Folia introduced, so every
 * method here works without any runtime branching — Paper just executes region/entity
 * tasks on its single main thread, while Folia executes them on the correct region
 * thread.</p>
 *
 * <p>Scheduler selection guide:
 * <ul>
 *   <li>{@link #runAsync}       — I/O, database, anything off-tick.</li>
 *   <li>{@link #runGlobal}      — Weather, game-rules, chat messages to players,
 *                                 anything not tied to a specific region.</li>
 *   <li>{@link #runAtLocation}  — Block/chunk mutations at a known location.</li>
 *   <li>{@link #runOnPlayer}    — Inventory opens, teleports, anything that touches
 *                                 a player entity (follows the player across regions).</li>
 * </ul>
 * </p>
 */
public final class SchedulerUtil {

    private SchedulerUtil() {}

    // Async — runs off any tick thread

    /**
     * Run {@code task} asynchronously as soon as possible.
     * Safe for blocking I/O (database queries, file reads, etc.).
     */
    public static void runAsync(HomeForge plugin, Runnable task) {
        plugin.getServer().getAsyncScheduler()
                .runNow(plugin, t -> task.run());
    }

    // Global region — one-shot and repeating

    /**
     * Execute {@code task} on the next tick of the global region.
     * Use for server-wide state (messages, game rules, console commands).
     */
    public static void runGlobal(HomeForge plugin, Runnable task) {
        plugin.getServer().getGlobalRegionScheduler()
                .execute(plugin, task);
    }

    /**
     * Schedule {@code task} to run on the global region after {@code delayTicks}.
     */
    public static void runGlobalLater(HomeForge plugin, Runnable task, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler()
                .runDelayed(plugin, t -> task.run(), delayTicks);
    }

    // Region — tied to a world location

    /**
     * Execute {@code task} on the region that owns {@code location}.
     * Use for block/chunk mutations at a specific location.
     */
    public static void runAtLocation(HomeForge plugin, Location location, Runnable task) {
        plugin.getServer().getRegionScheduler()
                .execute(plugin, location, task);
    }

    // Entity / Player — follows the entity across regions

    /**
     * Run {@code task} on the next tick of the region owning {@code player}.
     * Required for: opening inventories, reading player location, teleporting.
     *
     * @param retired optional callback if the player has already left the server; pass {@code null} to ignore.
     */
    public static void runOnPlayer(HomeForge plugin, Player player,
                                   Runnable task, Runnable retired) {
        player.getScheduler().run(plugin, t -> task.run(), retired);
    }

    /** Convenience overload — retired callback is a no-op. */
    public static void runOnPlayer(HomeForge plugin, Player player, Runnable task) {
        runOnPlayer(plugin, player, task, null);
    }

    /**
     * Run {@code task} on the player's region after {@code delayTicks} ticks.
     */
    public static void runOnPlayerLater(HomeForge plugin, Player player,
                                        Runnable task, long delayTicks) {
        player.getScheduler().runDelayed(plugin, t -> task.run(), null, delayTicks);
    }

    /**
     * Schedule a repeating task on the player's region.
     *
     * @param task             receives the {@link ScheduledTask} so it can cancel itself.
     * @param initialDelayTicks ticks to wait before first execution (minimum 1).
     * @param periodTicks       ticks between subsequent executions.
     * @return the {@link ScheduledTask} — keep a reference to cancel it later.
     */
    public static ScheduledTask runTimerOnPlayer(HomeForge plugin, Player player,
                                                 Consumer<ScheduledTask> task,
                                                 long initialDelayTicks, long periodTicks) {
        return player.getScheduler()
                .runAtFixedRate(plugin, task, null, initialDelayTicks, periodTicks);
    }
}