package de.marsg.miniperm.helper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.data.DBMgr;

public class SignMgr {

    private MiniPerm plugin;

    private Set<RankSign> signs = HashSet.newHashSet(2);

    public SignMgr(MiniPerm plugin){
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

    public void setPlayerOffline(Player player){
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

    public void checkDestroyedSign(Location loc){
        // Save to use (unlike a for-each) just like removePlayersSigns
        signs.removeIf(rankSign -> rankSign.getLocation().equals(loc));
    }

    
    public void initSigns() {

        CompletableFuture<Set<RankSign>> fetchedSigns = CompletableFuture.supplyAsync(DBMgr::getAllRankSign);

        // process after data was fetched
        fetchedSigns.thenCompose(allSigns -> {

            // List to later async delete the invalid rank signs
            List<CompletableFuture<Void>> invalidSigns = new ArrayList<>();

            for (RankSign rankSign : allSigns) {

                Block signBlock = rankSign.getLocation().getBlock();

                if (signBlock.getState() instanceof Sign) {
                    signs.add(rankSign);
                } else {
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

            // Wait with return until everything is done
            //return is only needed because of the thenCompose!
            return CompletableFuture.allOf(invalidSigns.toArray(new CompletableFuture[0]));
        });
    }

}
