package hoosk.minefurs.commands;

import hoosk.minefurs.handlers.PlayerJoinHandler;
import hoosk.minefurs.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.bukkit.Bukkit.getLogger;

public class GeoLookup implements Listener, CommandExecutor, TabCompleter {
    public static final String PERMISSION_NODE_TOGGLE = "geolookup.active";
    public static final String PERMISSION_NODE_USE = "geolookup.use";
    public static final String PERMISSION_NODE_LOGGING = "geolookup.logger";
    private boolean fileLogging;
    public Set<UUID> playersWithGeoLookupEnabled = new HashSet<>();
    private Main plugin;

    public GeoLookup(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new PlayerJoinHandler(this), plugin);
    }

    /**
     * Main command handler.
     * Checks if command is being executed by a player, then checks perms, and arguments.
     * @param sender - (Server/Player) command caller
     * @param command - The command called
     * @param label - Command label
     * @param args - (Optional) arguments passed with the command
     * @return true {executed correctly}
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Check if player and not server
        if(!(sender instanceof Player player)) {
            sender.sendMessage("Sorry! This command must be executed on a minecraft client!");
            return false;
        }

        // Check permissions
        if(!Objects.requireNonNull(player.getPlayer()).hasPermission(PERMISSION_NODE_USE)) {
            sender.sendMessage("Sorry! You are lacking appropriate permissions to execute this command!");
            return false;
        }

        // Make sure the API key has been set to avoid a null pointer exception
        if(plugin.getAPIKey() == null) {
            Bukkit.getLogger().warning("[GeoLookup] It looks like you haven't set an API key yet, or it failed to load correctly. Please check the config file, or use /geolookup-key <key>");
            sender.sendMessage("[GeoLookup] It looks like you haven't set an API key yet, or it failed to load correctly. Please check the config file, or use /geolookup-key <key>");
            return false;
        }

        String geoCommand = args[0].toLowerCase();

        switch (geoCommand) {
            case "active" -> {
                if (args.length != 2 || !sender.hasPermission(PERMISSION_NODE_TOGGLE)) {
                    sender.sendMessage("Sorry! You are lacking appropriate permissions to execute this command, or have executed it incorrectly!");
                    break;
                }
                if (args[1].equalsIgnoreCase("true")) {
                    playersWithGeoLookupEnabled.add(((Player) sender).getUniqueId());
                    sender.sendMessage("Geolocation auto-lookup enabled!");
                } else if (args[1].equalsIgnoreCase("false")) {
                    playersWithGeoLookupEnabled.remove(((Player) sender).getUniqueId());
                    sender.sendMessage("Geolocation auto-lookup disabled.");
                } else {
                    sender.sendMessage("Invalid argument. Usage: /geolookup active [true|false]");
                }
                return true;
            }
            case "logging" -> {
                if (args.length != 2 || !sender.hasPermission(PERMISSION_NODE_LOGGING)) {
                    sender.sendMessage("Sorry! You are lacking appropriate permissions to execute this command, or have executed it incorrectly!");
                    break;
                }
                if (args[1].equalsIgnoreCase("true")) {
                    fileLogging = true;
                    sender.sendMessage("Geolocation file logging has been enabled!");
                } else if (args[1].equalsIgnoreCase("false")) {
                    fileLogging = false;
                    sender.sendMessage("Geolocation file logging has been disabled!");
                } else {
                    sender.sendMessage("Invalid argument. Usage: /geolookup logging [true|false]");
                }
                return true;
            }
            case "*" -> {
                for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                    String addr = Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress();
                    getIPInfo(addr, sender, p);
                }
                return true;
            }
        }

        Player plr = Bukkit.getPlayer(args[0]);
        if (plr != null && plr.isOnline() && plr.getAddress() != null) {
            String addr = plr.getAddress().getAddress().getHostAddress(); // Grab IP
            getIPInfo(addr, sender, plr);
            return true;
        }

        sender.sendMessage("Please enter a valid player name or option(s)");
        return false;
    }

    /**
     * Queries the ipinfo.io API for the player's IP address. Be sure to add your token!
     * @param addr - Player IP address
     * @param sender - Caller of the /geolookup command
     * @param plr - The target player
     */
    public void getIPInfo(String addr, @NotNull CommandSender sender, @NotNull Player plr) {
        if(plugin.getAPIKey() == null) {
            Bukkit.getLogger().warning("[GeoLookup] It looks like you haven't set an API key yet, or it failed to load correctly. Please check the config file, or use /geolookup-key <key>");
            sender.sendMessage("[GeoLookup] It looks like you haven't set an API key yet, or it failed to load correctly. Please check the config file, or use /geolookup-key <key>");
            return;
        }

        String apiKey = plugin.getAPIKey();


        IPinfo ipInfo = new IPinfo.Builder()
                .setToken(apiKey)
                .build();
        try {
            IPResponse response = ipInfo.lookupIP(addr);
            /* Sometimes the API struggles to associate a country name to an address, but it is almost always
             * able to assign a region name. As a failsafe, this code tries country name first and if that fails
             * it falls back to using the region instead. (Still applies region in either case)
             */
            String country = response.getCountryName() != null ? response.getCountryName() + " [" + response.getRegion() + "]" : response.getRegion();
            String ipinfo = String.format(
                    "[GeoLookup] [%s] Player %s has connected from: [%s] %s with address {%s}",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    plr.getName(),
                    response.getCountryCode(),
                    country,
                    addr
            );
            sender.sendMessage(ipinfo);
            if(fileLogging) {
                logToFile(ipinfo);
            }
        } catch (RateLimitedException e) {
            getLogger().warning("ipinfo.io rate limit has been reached!.. How did you use 50k requests in a month??");
            e.printStackTrace();
        }
    }

    /**
     * Takes a string value to write, and creates a logfile (if it doesn't exist already) called geolookup.log which it writes to.
     * This method can be fairly inefficient if it is called many times in quick succession, as it has to re-create a new writer
     * each time and re-open the file just to close it again.
     * If performance issues are experienced, a more suitable logging library such as Log4J or SLF4J should be used for high frequency logging.
     * @param connectionInfo The string you want to write to the log file.
     */
    private void logToFile(String connectionInfo) {
        File logDir = new File("plugin logs");
        if(!logDir.exists()) {
            logDir.mkdir();
        }
        String logFileName = "GeoLookup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".log";
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(new File(logDir, logFileName), true))) {
            writer.write(connectionInfo);
            writer.newLine();
        } catch (IOException e) {
            getLogger().warning("Failed to write to ./plugin logs/*");
            e.printStackTrace();
        }
    }

    /**
     *
     * @param sender The sender of the command
     * @param command The command being called
     * @param alias Command alias
     * @param args The command arguments for tab completion
     * @return The possible options for command auto-completion
     */
    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("geolookup")) {
            if (args.length == 1) {
                // Return the sub-commands for /geolookup
                return Arrays.asList("*", "active", "logging");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("active") || args[0].equalsIgnoreCase("logging")) {
                    // Return true/false for /geolookup active and /geolookup logging
                    return Arrays.asList("true", "false");
                }
            }
        }

        // If no completions are available, return an empty list
        return Collections.emptyList();
    }
}
