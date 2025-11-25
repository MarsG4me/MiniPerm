package de.marsg.miniperm.helper;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import de.marsg.miniperm.permissions.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RankSign {

    private String worldName;
    private int x;
    private int y;
    private int z;
    private UUID ownersUUID;
    private MiniPerm plugin;

    public RankSign(MiniPerm plugin, String world, int x, int y, int z, UUID ownerUUID) {
        this.worldName = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ownersUUID = ownerUUID;
        this.plugin = plugin;
    }

    public void initDefaultText() {

        /*
         * Set Default sign text after server boot; reduces requests as we only update
         * the players group when they join
         */
        Block signBlock = getLocation().getBlock();

        Sign sign = null;
        if (signBlock.getState() instanceof Sign normalSign) {
            sign = normalSign;
        } else if (signBlock.getState() instanceof WallSign wallSign) {
            sign = (Sign) wallSign;
        }

        if (sign == null) {
            return;
        }

        /*
         * Keep the first line unchanged (that is the owners name)
         * If the sign was edited somehow, this "error" will be fixed when the player
         * joins the server
         */
        SignSide frontSide = sign.getSide(Side.FRONT);
        frontSide.line(1, Component.text("--"));
        frontSide.line(2, Component.text(""));
        frontSide.line(3, Component.text("OFFLINE").color(NamedTextColor.RED));

        sign.setWaxed(true);
        sign.update(true, false);
    }

    public void setText(String line1, String line2, boolean online) {

        Block signBlock = getLocation().getBlock();

        Sign sign = null;
        if (signBlock.getState() instanceof Sign normalSign) {
            sign = normalSign;
        } else if (signBlock.getState() instanceof WallSign wallSign) {
            sign = (Sign) wallSign;
        }

        if (sign == null) {
            return;
        }

        Component line4;
        if (online) {
            line4 = Component.text("ONLINE").color(NamedTextColor.GREEN);
        } else {
            line4 = Component.text("OFFLINE").color(NamedTextColor.RED);
        }

        /*
         * Fully update the sign text to display and changes to the user (should also
         * reflect name changes as the UUID stays fixed)
         */
        SignSide frontSide = sign.getSide(Side.FRONT);
        frontSide.line(0, Component.text(line1));
        frontSide.line(1, Component.text(line2));
        frontSide.line(2, Component.text(""));
        frontSide.line(3, line4);

        sign.setWaxed(true);
        sign.update(true, false); // force update
    }

    public boolean isOwner(Player player) {
        return ownersUUID.equals(player.getUniqueId());
    }

    public void updateSign(Player player) {
        updateSign(player, player.isOnline());
    }

    /**
     * @param player
     * @param setOnline OVERRIDE THE ONLINE STATUS
     *                  <p>
     *                  Mainly used for the quit event as the #isOnline check is
     *                  unreliable here
     */
    public void updateSign(Player player, boolean setOnline) {
        /*
         * Mainly used when players quit as the Player object at the time we take it
         * from te event is still considered online
         */
        if (isOwner(player)) {
            PlayerData data = plugin.getPermissionsMgr().getPlayersData(player);

            setText(player.getName(), data.getGroup().getPrefix(), setOnline);
        }
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z);
    }

}
