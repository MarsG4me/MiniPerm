package de.marsg.miniperm.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;

public class SignMgr {

    private MiniPerm plugin;

    private Set<RankSign> signs = HashSet.newHashSet(2);

    public SignMgr(MiniPerm plugin) {
        this.plugin = plugin;
    }

    public void addSign(RankSign sign) {
        signs.add(sign);
    }

    public void updatePlayersSigns(Player player) {
        for (RankSign rankSign : signs) {
            if (rankSign.isOwner(player)) {
                rankSign.updateSign(player);
            }
        }
    }

    public void setPlayerOffline(Player player) {
        for (RankSign rankSign : signs) {
            if (rankSign.isOwner(player)) {
                rankSign.updateSign(player, false);
            }
        }
    }

    public void removePlayersSigns(Player player) {
        // Save to use (unlike a for-each)
        signs.removeIf(rankSign -> rankSign.isOwner(player));
    }

    public void checkDestroyedSign(Location loc) {
        // Save to use (unlike a for-each) just like removePlayersSigns

        List<RankSign> invalidSigns = new ArrayList<>();

        for (RankSign rankSign : signs) {
            if (rankSign.getLocation().equals(loc)) {
                invalidSigns.add(rankSign);
            }
        }

        signs.removeIf(invalidSigns::contains);

        CompletableFuture.runAsync(() -> {
            invalidSigns.forEach(sign -> DBMgr.deleteRankSign(sign.getLocation().blockX(), sign.getLocation().blockY(),
                    sign.getLocation().blockZ(), sign.getLocation().getWorld().getName()));
        });
    }

    public void initSigns() {

        // Fetch rank signs off the main thread, but process world/block access on the
        // main thread
        CompletableFuture.supplyAsync(() -> {
            try {
                return DBMgr.getAllRankSign();
            } catch (Exception e) {
                plugin.getLogger().warning("[SignMgr] Exception while fetching rank signs: " + e.getMessage());
                return Set.<RankSign>of();
            }
        }).thenAccept(allSigns -> {
            // Process the fetched signs on the main server thread to safely access blocks
            Bukkit.getScheduler().runTask(plugin, () -> {
                List<CompletableFuture<Void>> invalidSigns = new ArrayList<>();

                for (RankSign rankSign : allSigns) {
                    Block signBlock = rankSign.getLocation().getBlock();

                    if (signBlock.getState() instanceof Sign) {
                        signs.add(rankSign);
                        // initialize sign text on the main thread
                        try {
                            rankSign.initDefaultText();
                        } catch (Exception e) {
                            plugin.getLogger().warning("[SignMgr] Failed to init sign text: " + e.getMessage());
                        }
                    } else {
                        // delete invalid sign async (DB operation)
                        CompletableFuture<Void> deleteFuture = CompletableFuture.runAsync(() -> {
                            DBMgr.deleteRankSign(
                                    signBlock.getX(),
                                    signBlock.getY(),
                                    signBlock.getZ(),
                                    signBlock.getWorld().getName());
                        });
                        invalidSigns.add(deleteFuture);
                    }
                }

                plugin.getLogger().info(String.format("Loaded %d rank signs.", signs.size()));

                // Optionally wait for delete operations to finish in background
                if (!invalidSigns.isEmpty()) {
                    CompletableFuture.allOf(invalidSigns.toArray(new CompletableFuture[0])).whenComplete((r, ex) -> {
                        if (ex != null) {
                            plugin.getLogger()
                                    .warning("[DB] Error while deleting invalid rank signs: " + ex.getMessage());
                        }
                    });
                }
            });
        });
    }

}
