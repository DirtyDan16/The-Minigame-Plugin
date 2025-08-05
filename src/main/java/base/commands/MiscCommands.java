package base.commands;

import base.MinigamePlugin;
import base.Other.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MiscCommands implements CommandExecutor, TabExecutor {
    private final MinigamePlugin plugin;

    public MiscCommands(MinigamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can execute this command.");
            return false;
        } else if (args.length == 0) {
            return false;
        }

        //--------Going Through all the valid misc commands----------------
        switch (args[0].toLowerCase()) {
            case "nuke":
                if (args.length < 2) {
                    player.sendMessage("Please provide a radius for the nuke.");
                    return false;
                } else if (!args[1].matches("\\d+") || Integer.parseInt(args[1]) <= 0) {
                    player.sendMessage("Invalid radius.");
                    return false;
                }
                sender.sendMessage("Nuking the game area...");
                int radius = Integer.parseInt(args[1]);
                Utils.nukeGameArea(player.getLocation(), radius);
                break;
            default:
                sender.sendMessage("Invalid command.");
                return false;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("nuke");
        }
        return List.of();
    }
}
