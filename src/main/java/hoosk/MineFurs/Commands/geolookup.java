package hoosk.MineFurs.Commands;

import hoosk.MineFurs.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.errors.RateLimitedException;
import io.ipinfo.api.model.IPResponse;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class geolookup implements Listener, CommandExecutor {
    private static final String PERMISSION_NODE_TOGGLE = "geolookup.active";
    private static final String PERMISSION_NODE_USE = "geolookup.use";
    private Set<UUID> playersWithGeoLookupEnabled = new HashSet<>();

    public geolookup(Main plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
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
            return false;
        }

        if(args.length == 0) {
            sender.sendMessage("Command usage: /geolookup {<player> | * | active} <options>");
            return true;
        } else if (args.length == 2 && args[0].equalsIgnoreCase("active") && sender.hasPermission(PERMISSION_NODE_TOGGLE)){
            if(args[1].equalsIgnoreCase("true")){
                playersWithGeoLookupEnabled.add(((Player) sender).getUniqueId());
                sender.sendMessage("Geolocation auto-lookup enabled!");
            } else if (args[1].equalsIgnoreCase("false")) {
                playersWithGeoLookupEnabled.remove(((Player) sender).getUniqueId());
                sender.sendMessage("Geolocation auto-lookup disabled.");
            } else {
                sender.sendMessage("Invalid argument. Usage: /geolookup active [true|false]");
            }
        } else {
            System.out.println(Arrays.toString(args));
            if (args[0].equals("*")) {
                var allPlayers = Bukkit.getServer().getOnlinePlayers();
                String addr;
                for (Player p : allPlayers) {
                    addr = Objects.requireNonNull(p.getAddress()).getAddress().getHostAddress();
                    getIPInfo(addr, sender, p);
                }
            }

            Player plr = Bukkit.getPlayer(args[0]);
            if (plr != null && plr.getAddress() != null) {
                String addr = plr.getAddress().getAddress().getHostAddress(); // Grab IP
                getIPInfo(addr, sender, plr);
            } else {
                sender.sendMessage("Please enter a valid player name or option(s)");
                return false;
            }
        }
        return true;
    }

    /**
     * Queries the ipinfo.io API for the player's IP address. Be sure to add your token!
     * @param addr - Player IP address
     * @param sender - Caller of the /geolookup command
     * @param plr - The target player
     */
    public void getIPInfo(String addr, CommandSender sender, Player plr) {
        IPinfo ipInfo = new IPinfo.Builder()
                .setToken("---PUT TOKEN HERE---") // TODO
                .build();
        try {
            IPResponse response = ipInfo.lookupIP(addr);
            if (response.getCountryName() != null) {
                String ipinfo = String.format("[%s]", plr.getName())
                        + " has connected from: "
                        + String.format("[%s] ", response.getCountryCode())
                        + response.getCountryName()
                        + "-"
                        + response.getRegion();
                sender.sendMessage(ipinfo);
            } else {
                String ipinfo = String.format("[%s]", plr.getName())
                        + " has connected from: "
                        + String.format("[%s] ", response.getCountryCode())
                        + response.getRegion();
                sender.sendMessage(ipinfo);
            }
        } catch (RateLimitedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * When a player joins, check if logging is globally enabled.
     * If enabled, players that have the permission and have logging enabled will start seeing player connection information.
     * @param event Player connection
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(),"say Welcome back " + event.getPlayer().getName() + "!");
        String addr = Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress();
        for(Player receiver : Bukkit.getOnlinePlayers()) {
            if (receiver.hasPermission(PERMISSION_NODE_TOGGLE) && playersWithGeoLookupEnabled.contains(receiver.getUniqueId())) {
                getIPInfo(addr, receiver, event.getPlayer());
            }
        }
    }
}
