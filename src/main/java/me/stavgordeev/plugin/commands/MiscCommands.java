package me.stavgordeev.plugin.commands;

import com.sk89q.worldedit.entity.Player;
import me.stavgordeev.plugin.Utils;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MiscCommands implements CommandExecutor, TabExecutor {
    private final Plugin plugin;

    public MiscCommands(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Please provide a valid command.");
            return false;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can execute this command.");
            return false;
        }

        //--------Going Through all the valid misc commands----------------
        switch (args[0].toLowerCase()) {
            case "nuke":
                if (args.length < 2) {
                    sender.sendMessage("Please provide a radius for the nuke.");
                    return false;
                }
                int radius = Integer.parseInt(args[1]);
                Utils.nukeGameArea((Location) sender, radius);
                break;
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
