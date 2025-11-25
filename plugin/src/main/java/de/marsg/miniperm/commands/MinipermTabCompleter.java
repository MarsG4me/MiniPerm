package de.marsg.miniperm.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import de.marsg.miniperm.MiniPerm;

public class MinipermTabCompleter implements TabCompleter {

    private MiniPerm plugin;

    private static final Map<String, String> SUBCOMMAND_PERMISSIONS = Map.of(
            "groups", "miniperm.groups",
            "permissions", "miniperm.permissions",
            "user", "miniperm.user",
            "create_sign", "miniperm.signs");

    public MinipermTabCompleter(MiniPerm plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player player && args.length > 1) {

            String subcommand = args[0].toLowerCase();

            // Check if the subcommand requires a specific permission
            if (SUBCOMMAND_PERMISSIONS.containsKey(subcommand)) {
                String requiredPermission = SUBCOMMAND_PERMISSIONS.get(subcommand);

                if (!player.hasPermission(requiredPermission) && !player.hasPermission("miniperm.admin")) {
                    return completions;
                }
            }
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList(
                    "groups", "permissions", "user", "create_sign", "test", "language");
            StringUtil.copyPartialMatches(args[0], subcommands, completions);

        } else if (args[0].equalsIgnoreCase("groups")) {
            if (args.length == 2) {
                List<String> subcommands = Arrays.asList(
                        "list", "create", "delete");
                StringUtil.copyPartialMatches(args[1], subcommands, completions);
            }
        } else if (args[0].equalsIgnoreCase("permissions")) {
            if (args.length == 2) {
                List<String> subcommands = Arrays
                        .asList(plugin.getPermissionsMgr().getGroupNames().toArray(new String[0]));
                subcommands.add(plugin.getPermissionsMgr().getDefaultGroupName());
                StringUtil.copyPartialMatches(args[1], subcommands, completions);
            } else if (args.length == 3) {
                List<String> subcommands = Arrays.asList(
                        "list", "add", "remove");
                StringUtil.copyPartialMatches(args[1], subcommands, completions);
            }
        } else if (args[0].equalsIgnoreCase("user")) {
            if (args.length == 2) {
                List<String> subcommands = Arrays.asList(
                        "info", "set_group", "remove_group");
                StringUtil.copyPartialMatches(args[1], subcommands, completions);
            } else if (args.length == 3) {
                Bukkit.getServer().getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (args.length == 4) {
                plugin.getPermissionsMgr().getGroupNames().forEach(completions::add);
                completions.add(plugin.getPermissionsMgr().getDefaultGroupName());
            }
        } else if (args[0].equalsIgnoreCase("language") && args.length == 2) {
            plugin.getLanguageMgr().getLanguages().forEach(completions::add);
        }

        return completions;

    }
}
