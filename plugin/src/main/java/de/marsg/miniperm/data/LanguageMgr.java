package de.marsg.miniperm.data;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import de.marsg.miniperm.MiniPerm;
import net.kyori.adventure.text.Component;

public class LanguageMgr {

    private MiniPerm plugin;

    private Map<String, YamlConfiguration> languageCache = HashMap.newHashMap(2);

    public LanguageMgr(MiniPerm plugin) {
        this.plugin = plugin;
    }

    public void loadAllLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists())
            langFolder.mkdirs();

        copyDefaultLanguage("de");
        copyDefaultLanguage("en");

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null)
            return;

        for (File file : files) {
            String langCode = file.getName().replace(".yml", "");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            languageCache.put(langCode, config);
            plugin.getLogger().info("Loaded language: " + langCode);
        }
    }

    private void copyDefaultLanguage(String langCode) {
        File file = new File(plugin.getDataFolder(), "lang/" + langCode + ".yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("lang/" + langCode + ".yml")) {
                if (in != null)
                    Files.copy(in, file.toPath());
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy default language " + langCode + ": " + e.getMessage());
            }
        }
    }

    public Set<String> getLanguages() {
        return languageCache.keySet();
    }

    private String getMessage(Player player, String key) {
        String lang = plugin.getPermissionsMgr().getPlayersData(player).getLanguage();
        YamlConfiguration config = languageCache.get(lang);
        if (config == null)
            return "§cMissing language: " + lang;
        return config.getString(key, "§cMissing translation: " + key);
    }

    public void broadcast(String messageKey, Set<Entry<String, String>> placeholders) {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            String msg = getMessage(player, messageKey);

            for (Entry<String, String> entry : placeholders) {
                msg = msg.replace(entry.getKey(), entry.getValue());
            }

            player.sendMessage(Component.text(msg));
        }
    }

    public void sendMessage(Player player, String messageKey, Set<Entry<String, String>> placeholders) {
        String msg = getMessage(player, messageKey);

        for (Entry<String, String> entry : placeholders) {
            msg = msg.replace(entry.getKey(), entry.getValue());
        }

        player.sendMessage(Component.text(msg));
    }

    public void sendMessage(Player player, String messageKey) {
        player.sendMessage(Component.text(getMessage(player, messageKey)));
    }
}
