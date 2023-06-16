package hoosk.minefurs;

import hoosk.minefurs.commands.GeoLookup;

// import java.net.URL;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import hoosk.minefurs.commands.SetAPIKey;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin implements Listener {
    FileConfiguration config = getConfig();
    boolean firstTimeLaunch = false;
    final Properties properties = new Properties();

    public static void main(String[] args) {
    }

    @Override
    public void onEnable() {
        // Config initialisation
        Bukkit.getPluginManager().registerEvents(this, this);
        config.options().copyDefaults(true);
        saveConfig();

        if(!config.contains("api-key")) {
            firstTimeLaunch=true;
            Bukkit.getLogger().info("[GeoLookup] Attempting to load configuration file...");
            Bukkit.getLogger().warning("[GeoLookup] First time launch detected, please enter your API key using /geolookup-key <key>");
            Bukkit.getLogger().warning("[GeoLookup] Please visit: https://ipinfo.io/ for your API key, if you don't have one already.");
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                if (player.isOp()) {
                    player.sendMessage("[GeoLookup] First time launch detected, please enter your API key using /geolookup-key <key>");
                    player.sendMessage("[GeoLookup] Please visit: https://ipinfo.io/ for your API key, if you don't have one already.");
                }
            }
        } else {
            try {
                properties.load(this.getClassLoader().getResourceAsStream("project.properties"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Bukkit.getLogger().info("[GeoLookup] Config file found and loaded successfully!");
            Bukkit.getLogger().info("[GeoLookup] Running version: " + properties.getProperty("version"));
            Bukkit.getLogger().info("[GeoLookup] Using API key: " + this.getAPIKey());
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                if (player.isOp()) {
                    player.sendMessage("[GeoLookup] Config file found and loaded successfully!");
                    player.sendMessage("[GeoLookup] Running version: " + properties.getProperty("version"));
                }
            }
        }

        // Plugin startup logic
        Bukkit.getLogger().info("[GeoLookup] Starting up GeoLookup plugin (by Juskie)!");
        Bukkit.getLogger().info("[GeoLookup] Registering commands!");

        // Commands
        GeoLookup geoLookup = new GeoLookup(this);
        Objects.requireNonNull(getCommand("geolookup")).setExecutor(geoLookup);
        Objects.requireNonNull(getCommand("geolookup")).setTabCompleter(geoLookup);
        Objects.requireNonNull(getCommand("geolookup-key")).setExecutor(new SetAPIKey(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("Shutting down GeoLookup plugin!");
    }

    public String getAPIKey() {
        return config.getString("api-key");
    }

    public void setAPIKey(String key) {
        config.set("api-key", key);
        saveConfig();
        firstTimeLaunch=false;
    }

    public boolean isFirstTimeLaunch() {
        return firstTimeLaunch;
    }
}
