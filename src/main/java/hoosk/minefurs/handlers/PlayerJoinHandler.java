package hoosk.minefurs.handlers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

import hoosk.minefurs.commands.GeoLookup;

public class PlayerJoinHandler implements Listener {
    private final GeoLookup geoLookup;

    public PlayerJoinHandler(GeoLookup geoLookup) {
        this.geoLookup = geoLookup;
    }

    /**
     * When a player joins, check if logging is globally enabled.
     * If enabled, players that have the permission and have logging enabled will start seeing player connection information.
     * @param event Player connection
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String addr = Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress();
        for(Player receiver : Bukkit.getOnlinePlayers()) {
            if (receiver.hasPermission(GeoLookup.PERMISSION_NODE_TOGGLE) && geoLookup.playersWithGeoLookupEnabled.contains(receiver.getUniqueId())) {
                geoLookup.getIPInfo(addr, receiver, event.getPlayer());
            }
        }
    }
}