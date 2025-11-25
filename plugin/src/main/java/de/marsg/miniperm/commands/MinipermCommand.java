package de.marsg.miniperm.commands;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;
import de.marsg.miniperm.data.LanguageMgr;
import de.marsg.miniperm.helper.RankSign;
import de.marsg.miniperm.permissions.PermissionGroup;
import de.marsg.miniperm.permissions.PermissionsMgr;
import de.marsg.miniperm.permissions.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class MinipermCommand implements CommandExecutor {

    private MiniPerm plugin;
    private LanguageMgr langMgr;

    public MinipermCommand(MiniPerm plugin) {
        this.plugin = plugin;
        this.langMgr = plugin.getLanguageMgr();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        switch (args[0].toLowerCase()) {
            case "groups":
                return manageGroups(sender, args);

            case "permissions":
                return managePermissions(sender, args);

            case "user":
                return manageUsers(sender, args);

            case "create_sign":
                return manageSigns(sender);

            case "test":
                return testPermission(sender, args[1]);

            case "language":
                if (args.length < 2) {
                    return false;
                }
                return manageLanguage(sender, args[1]);

            default:
                if (sender instanceof Player player) {
                    langMgr.sendMessage(player, "cmd.invalid");
                } else {
                    sender.sendMessage(Component.text("That is not a valid option!")
                            .color(NamedTextColor.RED));
                }
                break;
        }

        return true;
    }

    /*
     *
     *
     * Sign Management
     * 
     * 
     */

    private boolean manageSigns(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("miniperm.admin") && !player.hasPermission("miniperm.signs")) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.no_permission");
                return true;
            }

            Block target = player.getTargetBlockExact(10);
            if (target.getState() instanceof Sign) {
                plugin.getSignMgr().addSign(new RankSign(plugin, target.getWorld().getName(), target.getX(),
                        target.getY(), target.getZ(), player.getUniqueId()));
                plugin.getSignMgr().updatePlayersSigns(player);

                CompletableFuture.runAsync(() -> DBMgr.createRankSign(player.getUniqueId(), target.getX(),
                        target.getY(), target.getZ(), target.getWorld().getName()));

            } else {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_a_sign");
            }

        } else {
            sender.sendMessage("Only players can use this command!");
        }
        return true;
    }

    /*
     *
     *
     * Language Management
     * 
     * 
     */

    private boolean manageLanguage(CommandSender sender, String language) {
        if (sender instanceof Player player) {

            if (plugin.getLanguageMgr().getLanguages().contains(language)) {
                plugin.getPermissionsMgr().updateLanguage(player, language);
                plugin.getLanguageMgr().sendMessage(player, "language.set");
            } else {
                plugin.getLanguageMgr().sendMessage(player, "language.failed");
            }

        } else {
            sender.sendMessage("Only players can use this command!");
        }
        return true;
    }

    /*
     *
     *
     * User Management
     * 
     * 
     */

    private boolean manageUsers(CommandSender sender, String... args) {
        if (args.length < 3) {
            return false;
        }

        if (sender instanceof Player player
                && (!player.hasPermission("miniperm.admin") && !player.hasPermission("miniperm.user"))) {
            plugin.getLanguageMgr().sendMessage(player, "cmd.no_permission");
            return true;
        }
        if (args.length < 3) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_all_args_used");
            } else {
                sender.sendMessage("This command needs more arguments to work.");
            }
            return false;
        }

        // miniperm user <info/add_group/remove_group> <player_name> [group_name] [time]
        switch (args[1].toLowerCase()) {
            case "info":
                return manageUsersInfo(sender, args);

            case "set_group":
                return manageUsersSetGroup(sender, args);

            case "remove_group":
                return manageUsersRemoveGroup(sender, args);

            default:
                if (sender instanceof Player player) {
                    langMgr.sendMessage(player, "cmd.invalid");
                } else {
                    sender.sendMessage("That is not a valid option!");
                }
                break;
        }
        return false;
    }

    private boolean manageUsersRemoveGroup(CommandSender sender, String... args) {
        // miniperm user remove_group <player_name>
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "user.not_online");
            } else {
                sender.sendMessage("That player is not online!");
            }
            return true;
        }

        String oldGroup = plugin.getPermissionsMgr().getPlayersData(target).getGroup().getName();

        if (plugin.getPermissionsMgr().removePlayersGroup(target)) {
            if (sender instanceof Player player) {
                Map<String, String> placeholders = Map.of(
                        "%player%", target.getName(),
                        "%group%", oldGroup);
                langMgr.sendMessage(player, "user.removed", placeholders.entrySet());
            } else {
                sender.sendMessage(String.format("Player '%s' was removed from '%s'.", target.getName(),
                        oldGroup));
            }
        }

        return true;
    }

    private boolean manageUsersSetGroup(CommandSender sender, String... args) {
        if (args.length < 4) {
            return false;
        }
        // miniperm user set_group <player_name> <group_name> [time]
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "user.not_online");
            } else {
                sender.sendMessage("That player is not online!");
            }
            return true;
        }

        PermissionsMgr perm = plugin.getPermissionsMgr();

        if (!perm.doesGroupExist(args[3])) {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "group.doesnt_exists");
            } else {
                sender.sendMessage("This group doesn't exist!");
            }
            return false;
        }

        if (args.length < 5) {
            plugin.getPermissionsMgr().setPlayersGroup(target, args[3], null);

            if (sender instanceof Player player) {
                Map<String, String> placeholders = Map.of(
                        "%player%", target.getName(),
                        "%group%", args[3].toLowerCase(),
                        "%date%", "-");
                plugin.getLanguageMgr().sendMessage(player, "group.user_set_group", placeholders.entrySet());
            } else {
                sender.sendMessage(String.format("You set %s group to %s", target.getName(), args[3].toLowerCase()));
            }
            return true;
        }

        long expiration = temporalHelper(args);

        if (expiration == -1) {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "user.invalid_time");
            } else {
                sender.sendMessage(
                        "The time part can only contain the suffixes d, h, m and s for day, hour, minute and second!");
            }
        } else {
            plugin.getPermissionsMgr().setPlayersGroup(target, args[3], Instant.ofEpochSecond(expiration));

            if (sender instanceof Player player) {
                Map<String, String> placeholders = Map.of(
                        "%player%", target.getName(),
                        "%group%", args[3].toLowerCase(),
                        "%date%", Instant.ofEpochSecond(expiration).toString());
                plugin.getLanguageMgr().sendMessage(player, "group.user_set_group_limited", placeholders.entrySet());
            } else {
                sender.sendMessage(String.format("You set %s group to %s", target.getName(), args[3].toLowerCase()));
            }
        }

        return true;
    }

    private boolean manageUsersInfo(CommandSender sender, String... args) {
        Player target = Bukkit.getPlayer(args[2]);

        if (target == null) {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "user.not_online");
            } else {
                sender.sendMessage("That player is not online!");
            }
            return true;
        }

        PlayerData data = plugin.getPermissionsMgr().getPlayersData(target);

        if (data.getExpirationTimestamp() == null) {
            if (sender instanceof Player player) {
                Map<String, String> placeholders = Map.of(
                        "%player%", target.getName(),
                        "%group%", data.getGroup().getName());
                langMgr.sendMessage(player, "user.info", placeholders.entrySet());
            } else {
                sender.sendMessage(String.format("Player '%s' is part in the '%s' group.", target.getName(),
                        data.getGroup().getName()));
            }
        } else {
            if (sender instanceof Player player) {
                Map<String, String> placeholders = Map.of(
                        "%player%", target.getName(),
                        "%group%", data.getGroup().getName(),
                        "%date%", data.getExpirationTimestamp().toString());
                langMgr.sendMessage(player, "user.info", placeholders.entrySet());
            } else {
                sender.sendMessage(String.format("Player '%s' is part in the '%s' group until %s.", target.getName(),
                        data.getGroup().getName(), data.getExpirationTimestamp().toString()));
            }
        }

        return true;
    }

    /*
     *
     *
     * Permissions Management
     * 
     * 
     */

    private boolean managePermissions(CommandSender sender, String... args) {
        if (args.length < 3) {
            return false;
        }

        if (sender instanceof Player player
                && (!player.hasPermission("miniperm.admin") && !player.hasPermission("miniperm.permissions"))) {
            plugin.getLanguageMgr().sendMessage(player, "cmd.no_permission");
            return true;
        }
        // miniperm permissions <group_name> <list/add/remove> [permission]

        switch (args[2].toLowerCase()) {
            case "list":
                managePermissionsListAll(sender, args);
                return true;

            case "add":
                return managePermissionsAdd(sender, args);

            case "remove":
                return managePermissionsRemove(sender, args);

            default:
                if (sender instanceof Player player) {
                    langMgr.sendMessage(player, "cmd.invalid");
                } else {
                    sender.sendMessage("That is not a valid option!");
                }
                break;
        }
        return true;
    }

    private boolean managePermissionsRemove(CommandSender sender, String... args) {
        if (args.length < 3) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_all_args_used");
            } else {
                sender.sendMessage("This command needs more arguments to work.");
            }
            return false;
        } else if (!plugin.getPermissionsMgr().doesGroupExist(args[1])) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "group.doesnt_exists");
            } else {
                sender.sendMessage("This group doesn't exists.");
            }
        } else {
            if (plugin.getPermissionsMgr().removePermission(args[1], args[3])) {
                if (sender instanceof Player player) {
                    Map<String, String> placeholders = Map.of(
                            "%permission%", args[3]);
                    plugin.getLanguageMgr().sendMessage(player, "group.permission_removed", placeholders.entrySet());
                } else {
                    sender.sendMessage("This command needs more arguments to work.");
                }
            } else {
                if (sender instanceof Player player) {
                    plugin.getLanguageMgr().sendMessage(player, "group.permission_added_failed");
                } else {
                    sender.sendMessage("This command needs more arguments to work.");
                }
            }
        }
        return true;
    }

    private boolean managePermissionsAdd(CommandSender sender, String... args) {
        if (args.length < 3) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_all_args_used");
            } else {
                sender.sendMessage("This command needs more arguments to work.");
            }
            return false;
        } else if (!plugin.getPermissionsMgr().doesGroupExist(args[1])) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "group.doesnt_exists");
            } else {
                sender.sendMessage("This group doesn't exists.");
            }
        } else {
            if (plugin.getPermissionsMgr().addPermission(args[1], args[3])) {
                if (sender instanceof Player player) {
                    Map<String, String> placeholders = Map.of(
                            "%permission%", args[3]);
                    plugin.getLanguageMgr().sendMessage(player, "group.permission_added", placeholders.entrySet());
                } else {
                    sender.sendMessage("This command needs more arguments to work.");
                }
            } else {
                if (sender instanceof Player player) {
                    plugin.getLanguageMgr().sendMessage(player, "group.permission_removed_failed");
                } else {
                    sender.sendMessage("This command needs more arguments to work.");
                }
            }
        }
        return true;
    }

    private void managePermissionsListAll(CommandSender sender, String... args) {
        if (plugin.getPermissionsMgr().doesGroupExist(args[1])) {

            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "group.permissions_title");
            } else {
                sender.sendMessage("The group has the following permissions:");
            }
            for (String permission : plugin.getPermissionsMgr().getGroup(args[1]).getPermissions()) {
                sender.sendMessage("->" + permission);
            }
        } else {
            if (sender instanceof Player player) {
                langMgr.sendMessage(player, "group.doesnt_exists");
            } else {
                sender.sendMessage("That group doesn't exist!");
            }
        }
    }

    /*
     *
     *
     * Groups/Ranks Management
     * 
     * 
     */

    private boolean manageGroups(CommandSender sender, String... args) {
        if (args.length < 2) {
            return false;
        }

        if (sender instanceof Player player
                && (!player.hasPermission("miniperm.admin") && !player.hasPermission("miniperm.groups"))) {
            plugin.getLanguageMgr().sendMessage(player, "cmd.no_permission");
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "list":
                manageGroupsListAll(sender);
                return true;

            case "create":
                return manageGroupsCreateGroup(sender, args);

            case "delete":
                return manageGroupsDeleteGroup(sender, args);

            default:
                if (sender instanceof Player player) {
                    langMgr.sendMessage(player, "cmd.invalid");
                } else {
                    sender.sendMessage("That is not a valid option!");
                }
                break;
        }
        return false;
    }

    private boolean manageGroupsCreateGroup(CommandSender sender, String... args) {
        // miniperm groups create [group_name] [weight] [is_default] [prefix]
        if (args.length < 5) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_all_args_used");
            } else {
                sender.sendMessage("This command needs more arguments to work.");
            }
            return false;

        } else if (plugin.getPermissionsMgr().doesGroupExist(args[2])) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "group.already_exists");
            } else {
                sender.sendMessage("This group already exists.");
            }

        } else {
            String groupName = args[2].toLowerCase();
            int weight = 0;

            try {
                weight = Integer.parseInt(args[3]);
            } catch (Exception e) {
                if (sender instanceof Player player) {
                    Map<String, String> placeholders = Map.of(
                            "%arg%", "weight",
                            "%type%", "number");
                    langMgr.sendMessage(player, "cmd.invalid_type", placeholders.entrySet());
                } else {
                    sender.sendMessage("Weight must be a number!");
                }
                return false;
            }

            boolean isDefault = false;
            if (args[4].equalsIgnoreCase("t") || args[4].equalsIgnoreCase("true") || args[4].equals("1")) {
                isDefault = true;
            }
            String prefix = String.join(" ", Arrays.copyOfRange(args, 5, args.length));

            PermissionGroup group = new PermissionGroup(-1, groupName, prefix, weight, isDefault);
            plugin.getPermissionsMgr().addGroup(group);
            return true;
        }

        return false;
    }

    private boolean manageGroupsDeleteGroup(CommandSender sender, String... args) {
        // miniperm groups delete [group_name]
        if (args.length < 3) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "cmd.not_all_args_used");
            } else {
                sender.sendMessage("This command needs more arguments to work.");
            }
            return false;

        } else if (!plugin.getPermissionsMgr().doesGroupExist(args[2])) {
            if (sender instanceof Player player) {
                plugin.getLanguageMgr().sendMessage(player, "group.doesnt_exists");
            } else {
                sender.sendMessage("This group doesn't exists.");
            }

        } else {
            return plugin.getPermissionsMgr().deleteGroup(args[2].toLowerCase());
        }
        return false;
    }

    private void manageGroupsListAll(CommandSender sender) {
        if (sender instanceof Player player) {
            plugin.getLanguageMgr().sendMessage(player, "group.list");
        } else {
            sender.sendMessage("List of groups:");
        }
        sender.sendMessage("-> " + plugin.getPermissionsMgr().getDefaultGroupName());
        for (String groupName : plugin.getPermissionsMgr().getGroupNames()) {
            sender.sendMessage("-> " + groupName);
        }
    }

    /*
     *
     *
     * Test Permission Management
     * 
     * 
     */

    private boolean testPermission(CommandSender sender, String permission) {

        if (sender instanceof Player player) {

            boolean hasPermission = player.hasPermission(permission);

            if (hasPermission) {
                Map<String, String> placeholders = Map.of(
                        "%permission%", permission);
                langMgr.sendMessage(player, "miniperm.test.has_permission", placeholders.entrySet());
            } else {
                Map<String, String> placeholders = Map.of(
                        "%permission%", permission);
                langMgr.sendMessage(player, "miniperm.test.doesnt_have_permission", placeholders.entrySet());
            }
        } else {
            sender.sendMessage("Only players can use this command!");
        }
        return true;
    }

    /*
     *
     *
     * Helper Methodes
     * 
     * 
     */

    private long temporalHelper(String... args) {
        String[] temporalArgs = Arrays.copyOfRange(args, 4, args.length);
        Instant expiration = Instant.now();

        try {
            for (String timeArg : temporalArgs) {
                if (timeArg.endsWith("d")) {
                    String time = timeArg.replace("d", "");
                    expiration = expiration.plus(Long.parseLong(time), ChronoUnit.DAYS);
                } else if (timeArg.endsWith("h")) {
                    String time = timeArg.replace("h", "");
                    expiration = expiration.plus(Long.parseLong(time), ChronoUnit.HOURS);
                } else if (timeArg.endsWith("m")) {
                    String time = timeArg.replace("m", "");
                    expiration = expiration.plus(Long.parseLong(time), ChronoUnit.MINUTES);
                } else if (timeArg.endsWith("s")) {
                    String time = timeArg.replace("s", "");
                    expiration = expiration.plus(Long.parseLong(time), ChronoUnit.SECONDS);
                } else {
                    return -1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        return expiration.getEpochSecond();
    }
}
