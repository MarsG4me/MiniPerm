package de.marsg.miniperm.commands;

import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.LanguageMgr;
import de.marsg.miniperm.permissions.PermissionGroup;
import de.marsg.miniperm.permissions.PlayerData;

public class WhoAmICommand implements CommandExecutor {

    private MiniPerm plugin;
    private LanguageMgr langMgr;

    public WhoAmICommand(MiniPerm plugin) {
        this.plugin = plugin;
        this.langMgr = plugin.getLanguageMgr();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (sender instanceof Player player) {

            PermissionGroup group = plugin.getPermissionsMgr().getPlayersGroup(player);

            PlayerData playerData = plugin.getPermissionsMgr().getPlayersData(player);

            if (playerData == null || playerData.getExpirationTimestamp() == null) {
                /*
                 *
                 * format and stuff for permanent groups
                 * 
                 */
                Map<String, String> placeholders = Map.of(
                        "%player%", player.getName(),
                        "%prefix%", group.getPrefix(),
                        "%group%", group.getName());
                langMgr.sendMessage(player, "whoami.forever", placeholders.entrySet());

            } else {
                /*
                 *
                 * format and stuff for temporary groups
                 * 
                 */
                Map<String, String> placeholders = Map.of(
                        "%player%", player.getName(),
                        "%prefix%", group.getPrefix(),
                        "%group%", group.getName(),
                        "%date%", playerData.getExpirationTimestamp().toString());
                langMgr.sendMessage(player, "whoami.limited", placeholders.entrySet());
            }

            return true;
        }

        sender.sendMessage("Only a player can use this command!");

        return false;
    }
}
