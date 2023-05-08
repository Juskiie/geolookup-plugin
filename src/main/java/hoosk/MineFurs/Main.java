package hoosk.MineFurs;

import hoosk.MineFurs.Commands.geolookup;

// import java.net.URL;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    public static void main(String[] args) {

    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getLogger().info("Starting up GeoLookup plugin (by Juskie)!");

        // Commands
        Objects.requireNonNull(this.getCommand("geolookup")).setExecutor(new geolookup(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getLogger().info("Shutting down GeoLookup plugin (by Juskie)!");
    }
}
