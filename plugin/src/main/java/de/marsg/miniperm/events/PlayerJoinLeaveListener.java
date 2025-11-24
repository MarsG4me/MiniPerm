package de.marsg.miniperm.events;

import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.permissions.PermissionGroup;
import de.marsg.miniperm.permissions.PermissionsMgr;

public class PlayerJoinLeaveListener implements Listener {

    private final MiniPerm plugin;
    private final PermissionsMgr permMgr;

    public PlayerJoinLeaveListener(MiniPerm plugin) {
        this.plugin = plugin;
        permMgr = plugin.getPermissionsMgr();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        Player player = event.getPlayer();

        plugin.getPermissionsMgr().loadPlayerData(player).thenRun(() -> {

            PermissionGroup group = plugin.getPermissionsMgr().getPlayersGroup(player);
            Map<String, String> placeholders = Map.of(
                    "%player%", player.getName(),
                    "%prefix%", group.getPrefix(),
                    "%group%", group.getName());

            plugin.getLanguageMgr().broadcast("server.join", placeholders.entrySet());
        });
    }

    @EventHandler
    public void onPlayerLe(PlayerQuitEvent event) {
        event.quitMessage(null);

        Player player = event.getPlayer();

        PermissionGroup group = plugin.getPermissionsMgr().getPlayersGroup(player);
        Map<String, String> placeholders = Map.of(
                "%player%", player.getName(),
                "%prefix%", group.getPrefix(),
                "%group%", group.getName());

        plugin.getLanguageMgr().broadcast("server.leave", placeholders.entrySet());

        permMgr.playerLeft(player);
    }

}
