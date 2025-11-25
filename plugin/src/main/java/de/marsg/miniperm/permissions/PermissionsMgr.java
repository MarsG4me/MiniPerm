package de.marsg.miniperm.permissions;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;

public class PermissionsMgr {

    private MiniPerm plugin;

    private Map<String, PermissionGroup> groups = HashMap.newHashMap(2);
    private PermissionGroup defaultGroup;

    private Map<Player, PlayerData> playersData = HashMap.newHashMap(8);

    public PermissionsMgr(MiniPerm plugin) {
        this.plugin = plugin;
    }

    /*
    *
    *
     * All group related parts
    *
    *
     */

    /**
     * @param name
     * @return TRUE if the group exists; FALSE else
     */
    public boolean doesGroupExist(String name) {
        return defaultGroup.getName().equals(name.toLowerCase()) || groups.containsKey(name.toLowerCase());
    }

    public PermissionGroup getGroup(String name) {
        if (defaultGroup.getName().equals(name.toLowerCase())) {
            return defaultGroup;
        } else {
            return groups.get(name.toLowerCase());
        }
    }

    /**
     * @return all group names (except default group)
     */
    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public String getDefaultGroupName() {
        return defaultGroup.getName();
    }

    public void addGroup(PermissionGroup group) {
        addGroup(group, true);
    }

    private void addGroup(PermissionGroup group, boolean doDBEntry) {

        if (doDBEntry) {

            CompletableFuture.runAsync(() -> {
                int key = DBMgr.addGroup(group.getName(), group.getPrefix(), group.getWeight(), group.isDefaultGroup());
                group.setId(key);
            });
        }

        if (group.isDefaultGroup()) {
            if (defaultGroup != null) {
                defaultGroup.removeDefaultFlag();
                addGroup(defaultGroup);
            }
            defaultGroup = group;

        } else {
            groups.putIfAbsent(group.getName(), group);
        }
    }

    public boolean deleteGroup(String name) {

        if (groups.containsKey(name)) {

            CompletableFuture.runAsync(() -> DBMgr.deleteGroup(name));

            PermissionGroup group = groups.get(name);

            for (Entry<Player, PlayerData> entry : playersData.entrySet()) {
                if (entry.getValue().getGroup().equals(group)) {
                    setPlayersGroup(entry.getKey(), name, null, true);
                }
            }

            groups.remove(name);
        }

        return false;
    }


    /*
    *
    *
     * All permissions related parts
    *
    *
     */

    public boolean addPermission(String group, String permission) {
        if (!doesGroupExist(group)) {
            return false;
        }
        if (group.equals(getDefaultGroupName()) && defaultGroup.addPermission(permission)) {

            CompletableFuture.runAsync(() -> {
                if (DBMgr.addPermission(defaultGroup.getId(), permission)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        for (Entry<Player, PlayerData> entry : playersData.entrySet()) {
                            if (entry.getValue().getGroup().getName().equals(group)) {
                                addPermissionToPlayer(entry.getKey(), permission);
                            }
                        }
                    });
                }
            });

        } else if (groups.get(group).addPermission(permission)) {

            CompletableFuture.runAsync(() -> {
                if (DBMgr.addPermission(groups.get(group).getId(), permission)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        for (Entry<Player, PlayerData> entry : playersData.entrySet()) {
                            if (entry.getValue().getGroup().getName().equals(group)) {
                                addPermissionToPlayer(entry.getKey(), permission);
                            }
                        }
                    });
                }
            });

        }
        return true;
    }

    public boolean removePermission(String group, String permission) {
        if (!doesGroupExist(group)) {
            return false;
        }

        if (group.equals(getDefaultGroupName()) && defaultGroup.removePermission(permission)) {

            CompletableFuture.runAsync(() -> {
                if (DBMgr.removePermission(defaultGroup.getId(), permission)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        for (Entry<Player, PlayerData> entry : playersData.entrySet()) {
                            if (entry.getValue().getGroup().getName().equals(group)) {
                                removePermissionFromPlayer(entry.getKey(), permission);
                            }
                        }
                    });
                }
            });

        } else if (groups.get(group).removePermission(permission)) {

            CompletableFuture.runAsync(() -> {
                if (DBMgr.removePermission(groups.get(group).getId(), permission)) {
                    Bukkit.getScheduler().runTask(plugin, () -> {

                        for (Entry<Player, PlayerData> entry : playersData.entrySet()) {
                            if (entry.getValue().getGroup().getName().equals(group)) {
                                removePermissionFromPlayer(entry.getKey(), permission);
                            }
                        }
                    });
                }
            });

        }
        return true;
    }


    /*
    *
    *
     * All user related parts
    *
    *
     */

    public void updateLanguage(Player player, String language) {
        PlayerData data = playersData.get(player);
        data.updateLanguage(language);
        CompletableFuture.runAsync(
                () -> DBMgr.updateUsersLanguage(player.getUniqueId(), language));

    }

    public boolean removePlayersGroup(Player player) {
        return setPlayersGroup(player, defaultGroup.getName(), null);
    }

    /**
     * @param player
     * @param group
     * @return TRUE if the player was given the group; FALSE else
     */
    public boolean setPlayersGroup(Player player, String group) {
        return setPlayersGroup(player, group, null);
    }

    /**
     * @param player
     * @param group
     * @param expirationDate
     * @return TRUE if the player was given the group; FALSE else
     */
    public boolean setPlayersGroup(Player player, String group, Instant expirationDate) {
        return setPlayersGroup(player, group, expirationDate, true);
    }

    private boolean setPlayersGroup(Player player, String group, Instant expirationDate, boolean updateDb) {
        plugin.getExpirationScheduler().removeTimer(player);
        plugin.getLogger().info(String.format("Setting %s's group to %s....", player.getName(), group));
        PermissionAttachment attachment;
        String language = "en";
        if (playersData.containsKey(player)) {
            attachment = playersData.get(player).getAttachment();
            language = playersData.get(player).getLanguage();
        } else {
            attachment = player.addAttachment(plugin);
        }

        if (groups.containsKey(group)) {

            if (updateDb) {
                plugin.getLogger().info("Starting DB sync...");
                CompletableFuture.runAsync(
                        () -> DBMgr.setUsersGroup(player.getUniqueId(), groups.get(group).getId(), expirationDate));
            }

            PlayerData data = new PlayerData(groups.get(group), attachment, expirationDate, language);
            playersData.put(player, data);
            resetPlayersPermissions(player);
            syncPermissionsWithServer(player);
            if (expirationDate != null) {
                plugin.getExpirationScheduler().addTimer(player, expirationDate);
            }
            plugin.getLogger().info(String.format("Set players group to %s.", group));

            // Using Mockito this getter is not correctly setup. but as it is working in
            // production we skipp it here in testing
            if (plugin.getSignMgr() != null) {
                plugin.getSignMgr().updatePlayersSigns(player);
            }
            return true;

        } else if (group.equals(defaultGroup.getName())) {

            if (updateDb) {
                plugin.getLogger().info("Starting DB sync...");
                CompletableFuture.runAsync(
                        () -> DBMgr.setUsersGroup(player.getUniqueId(), defaultGroup.getId(), null));
            }
            PlayerData data = new PlayerData(defaultGroup, attachment, null, language);
            playersData.put(player, data);
            resetPlayersPermissions(player);
            syncPermissionsWithServer(player);
            plugin.getLogger().info("Set players group to default group.");

            // Using Mockito this getter is not correctly setup. but as it is working in
            // production we skipp it here in testing
            if (plugin.getSignMgr() != null) {
                plugin.getSignMgr().updatePlayersSigns(player);
            }
            return true;
        }
        return false;
    }

    public PermissionGroup getPlayersGroup(Player player) {
        if (playersData.containsKey(player)) {
            return playersData.get(player).getGroup();
        } else {
            return defaultGroup;
        }
    }

    public PlayerData getPlayersData(Player player) {
        if (playersData.containsKey(player)) {
            return playersData.get(player);
        }
        return null;
    }

    private void addPermissionToPlayer(Player player, String permission) {
        if (playersData.containsKey(player)) {
            playersData.get(player).getAttachment().setPermission(permission, true);
        }
    }

    private void removePermissionFromPlayer(Player player, String permission) {
        if (playersData.containsKey(player)) {
            playersData.get(player).getAttachment().unsetPermission(permission);
        }
    }

    public void playerLeft(Player player) {
        playersData.remove(player);
    }


    /*
    *
    *
     * Load userdata on login
    *
    *
     */
    public CompletableFuture<Void> loadPlayerData(Player player) {
        plugin.getLogger().info("Begin PlayerData creation");

        // Logic complicated but needed to ensure the return happens only AFTER the
        // player data is created
        return CompletableFuture.supplyAsync(() -> {

            return DBMgr.getUsersGroup(player.getUniqueId());
        }).thenCompose(result -> {

            CompletableFuture<Void> completionFuture = new CompletableFuture<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    if (result.length > 1) {
                        setPlayersGroup(player, (String) result[0], (Instant) result[1], false);
                        playersData.get(player).updateLanguage((String) result[2]);
                        if ((Instant) result[1] != null) {
                            plugin.getExpirationScheduler().addTimer(player, (Instant) result[1]);
                        }
                    } else {
                        setPlayersGroup(player, defaultGroup.getName(), null, true);
                    }
                    completionFuture.complete(null); // Signal successful completion
                } catch (Exception e) {
                    completionFuture.completeExceptionally(e); // Signal an error
                }
            });

            return completionFuture;
        });
    }


    /*
    *
    *
     * Fetch groups on server startup
    *
    *
     */
    public void loadGroups() {
        CompletableFuture.runAsync(() -> {

            for (Entry<String, Object[]> groupData : DBMgr.getAllGroups().entrySet()) {

                Set<String> permissions = DBMgr.getGroupsPermission((int) groupData.getValue()[0]);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    PermissionGroup group = new PermissionGroup(
                            (int) groupData.getValue()[0], groupData.getKey(), (String) groupData.getValue()[1],
                            (int) groupData.getValue()[2], (boolean) groupData.getValue()[3], permissions);

                    addGroup(group, false);
                });
            }

        });
    }

    private void syncPermissionsWithServer(Player player) {
        PermissionAttachment atta = playersData.get(player).getAttachment();
        for (String permission : playersData.get(player).getGroup().getPermissions()) {
            atta.setPermission(permission, true);
        }
    }

    private void resetPlayersPermissions(Player player) {
        PermissionAttachment atta = playersData.get(player).getAttachment();
        for (String permission : atta.getPermissions().keySet()) {
            atta.unsetPermission(permission);
        }
    }

}
