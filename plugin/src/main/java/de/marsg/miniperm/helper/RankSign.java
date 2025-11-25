package de.marsg.miniperm.helper;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
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

    private Location loc;
    private UUID ownersUUID;
    private MiniPerm plugin;

    public RankSign(MiniPerm plugin, String world, int x, int y, int z, UUID ownerUUID){
        this.loc = new Location(Bukkit.getWorld(world), x, y, z);
        this.ownersUUID = ownerUUID;
        this.plugin = plugin;
        setDefaultText();
    }

    private void setDefaultText(){
        setText("", "--", false);
    }



    public void setText(String line1, String line2, boolean online){
        
            Block signBlock = loc.getBlock();
            
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
            }else{
                line4 = Component.text("OFFLINE").color(NamedTextColor.RED);
            }

            SignSide frontSide = sign.getSide(Side.FRONT);
            frontSide.line(0, Component.text(line1));
            frontSide.line(1, Component.text(line2));
            frontSide.line(2, Component.text(""));
            frontSide.line(3, line4);
            frontSide.setGlowingText(true);
            frontSide.setColor(DyeColor.MAGENTA);

            sign.setWaxed(true);
            sign.update(true, false); // force update
    }

    public boolean isOwner(Player player){
        return ownersUUID.equals(player.getUniqueId());
    }

    public void updateSign(Player player){
        updateSign(player, player.isOnline());
    }

    public void updateSign(Player player, boolean setOnline){
        if (isOwner(player)) {
            PlayerData data = plugin.getPermissionsMgr().getPlayersData(player);

            setText(player.getName(), data.getGroup().getPrefix(), setOnline);
        }
    }

    public Location getLocation(){
        return loc;
    }

}
