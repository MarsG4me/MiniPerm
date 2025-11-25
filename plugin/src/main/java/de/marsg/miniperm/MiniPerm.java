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
import de.marsg.miniperm.events.SignBreakingListener;
import de.marsg.miniperm.helper.SignMgr;
import de.marsg.miniperm.permissions.PermissionsMgr;
import de.marsg.miniperm.scheduler.GroupExpirationScheduler;

public class MiniPerm extends JavaPlugin {

    private PermissionsMgr groupMgr;
    private LanguageMgr langMgr;
    private GroupExpirationScheduler groupAutoRemover;
    private SignMgr signsMgr;

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

            // The Rank sing stuffs
            signsMgr = new SignMgr(this);
            signsMgr.initSigns();

            // Load group Manager
            groupMgr = new PermissionsMgr(this);
            groupMgr.loadGroups();

            // Load language Manager
            langMgr = new LanguageMgr(this);
            langMgr.loadAllLanguages();

            // Auto group expiration manager
            groupAutoRemover = new GroupExpirationScheduler(this);

            // Register commands

            getCommand("miniperm").setExecutor(new MinipermCommand(this));
            getCommand("miniperm").setTabCompleter(new MinipermCommand(this));

            getCommand("whoami").setExecutor(new WhoAmICommand(this));

            // Register event listeners
            getServer().getPluginManager().registerEvents(new PlayerJoinLeaveListener(this), this);
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getServer().getPluginManager().registerEvents(new SignBreakingListener(this), this);

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

    public PermissionsMgr getPermissionsMgr() {
        return groupMgr;
    }

    public LanguageMgr getLanguageMgr() {
        return langMgr;
    }

    public GroupExpirationScheduler getExpirationScheduler() {
        return groupAutoRemover;
    }

    public SignMgr getSignMgr() {
        return signsMgr;
    }
}