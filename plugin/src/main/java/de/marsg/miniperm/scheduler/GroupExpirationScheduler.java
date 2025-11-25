package de.marsg.miniperm.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import de.marsg.miniperm.MiniPerm;

public class GroupExpirationScheduler {

    private Map<UUID, BukkitTask> activeTimer = new ConcurrentHashMap<>(2);

    private MiniPerm plugin;

    public GroupExpirationScheduler(MiniPerm plugin) {
        this.plugin = plugin;
    }

    public void addTimer(Player player, Instant expiresAt) {
        removeTimer(player);

        // runTaskLaterAsynchronously needs ticks when to execute as "delay"
        long delayMillis = Duration.between(Instant.now(), expiresAt).toMillis();
        // Convert milliseconds to ticks (1000 milli per second and 20 ticks per second)
        // ! AS THIS USES TICKS; INCREASING TICK SPEED WILL BREAK THE CORRECT TIMING !
        long delayTicks = Math.max(1, delayMillis / 50); // ->Ensures to never have negative delay

        BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<String, String> placeholders = Map.of(
                        "%group%", plugin.getPermissionsMgr().getPlayersGroup(player).getName());
                plugin.getPermissionsMgr().removePlayersGroup(player);
                plugin.getLanguageMgr().sendMessage(player, "user.auto_group_removal", placeholders.entrySet());
            });
        }, delayTicks);

        activeTimer.put(player.getUniqueId(), task);
    }

    public void removeTimer(Player player) {
        BukkitTask timer = activeTimer.get(player.getUniqueId());
        if (timer != null) {
            timer.cancel();
            activeTimer.remove(player.getUniqueId());
        }
    }
}