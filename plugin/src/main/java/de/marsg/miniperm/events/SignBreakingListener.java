package de.marsg.miniperm.events;

import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import de.marsg.miniperm.MiniPerm;

public class SignBreakingListener implements Listener {

    private MiniPerm plugin;

    public SignBreakingListener(MiniPerm plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event) {
        if (event.getBlock().getState() instanceof Sign) {

            /*
             * Listener to auto remove broken rank signs so the DB entry can be removed if
             * the sign is gone
             */
            plugin.getSignMgr().checkDestroyedSign(event.getBlock().getLocation());
            plugin.getLogger().info("A potential rank sign was broken.");
        }
    }
}
