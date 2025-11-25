package de.marsg.miniperm.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.helper.CustomChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;

public class ChatListener implements Listener {

    private final MiniPerm plugin;

    public ChatListener(MiniPerm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerSendMessage(AsyncChatEvent event) {
        Player player = event.getPlayer();

        /*
         *
         * Attache the custom text renderer to the message to display the group prefix
         * before the message
         * 
         */

        event.renderer(new CustomChatRenderer(plugin.getPermissionsMgr().getPlayersGroup(player).getPrefix()));
    }

}
