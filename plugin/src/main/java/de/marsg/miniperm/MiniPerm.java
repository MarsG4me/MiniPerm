package de.marsg.miniperm;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import de.marsg.miniperm.commands.MinipermCommand;
import de.marsg.miniperm.commands.WhoAmICommand;
import de.marsg.miniperm.data.DBMgr;
import de.marsg.miniperm.data.LanguageMgr;
import de.marsg.miniperm.events.ChatListener;
import de.marsg.miniperm.events.PlayerJoinLeaveListener;
import de.marsg.miniperm.permissions.PermissionsMgr;

public class MiniPerm extends JavaPlugin {

    private PermissionsMgr groupMgr;
    private LanguageMgr langMgr;
    
    @Override
    public void onEnable() {
        try {
            // Save default config if it doesn't exist
            saveDefaultConfig();

            /*
            * Ensure DB connection is setup
            */
            if (!DBMgr.setup(this)) {
                getLogger().warning("DB setup failed! Disabeling plugin...");
                Bukkit.getPluginManager().disablePlugin(this);
            }

            //Load group Manager
            groupMgr = new PermissionsMgr(this);
            groupMgr.loadGroups();

            //Load language Manager
            langMgr = new LanguageMgr(this);
            langMgr.loadAllLanguages();


            // Register commands

            getCommand("miniperm").setExecutor(new MinipermCommand(this));
            getCommand("miniperm").setTabCompleter(new MinipermCommand(this));

            getCommand("whoami").setExecutor(new WhoAmICommand(this));

            // Register event listeners
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);



            getLogger().info("MiniPerm plugin enabled!");
        } catch (Exception e) {
            getLogger().severe("Failed to enable MiniPerm plugin!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    
    @Override
    public void onDisable() {
        try {
            ScoreboardManager manager = Bukkit.getScoreboardManager();
            Scoreboard board = manager.getMainScoreboard();
            board.getTeams().forEach(team -> {
                if (team.getName().startsWith("miniperm_")) {
                    team.unregister();
                }
            });

            getLogger().info("MiniPerm plugin disabled!");
        } catch (Exception e) {
            getLogger().severe("Error while disabling MiniPerm plugin!");
            e.printStackTrace();
        }
    }

    public PermissionsMgr getPermissionsMgr(){
        return groupMgr;
    }

    public LanguageMgr getLanguageMgr(){
        return langMgr;
    }

}