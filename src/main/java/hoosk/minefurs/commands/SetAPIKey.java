package hoosk.minefurs.commands;

import hoosk.minefurs.Main;
// import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Class for setting API key for ipinfo service.
 */
public class SetAPIKey implements Listener, CommandExecutor {
    private Main plugin;

    public SetAPIKey(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!(commandSender instanceof Player) || !commandSender.isOp()) {
            commandSender.sendMessage("This command can only be run by an operator.");
            return true;
        }

        if (plugin.isFirstTimeLaunch()) {
            if (args.length > 0) {
                plugin.setAPIKey(args[0]);
                commandSender.sendMessage("API key saved. You can now use all commands.");
                return true;
            } else {
                commandSender.sendMessage("Please enter a key. Usage: /geolookup-key <key>");
                return false;
            }
        }

        return false;
    }
}
