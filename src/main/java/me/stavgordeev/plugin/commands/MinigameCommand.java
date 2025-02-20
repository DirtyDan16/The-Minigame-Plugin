// src/main/java/me/stavgordeev/plugin/commands/MinigameCommand.java
package me.stavgordeev.plugin.commands;

import me.stavgordeev.plugin.DiscoMayhem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.command.TabExecutor;

import java.util.List;

public class MinigameCommand implements CommandExecutor, TabExecutor {
    private final DiscoMayhem discoMayhem;

    public MinigameCommand(DiscoMayhem discoMayhem) {
        this.discoMayhem = discoMayhem;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        if (args.length == 0) {
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                try {
                    discoMayhem.start(player);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "stop":
                discoMayhem.pauseGame(player);
                break;
            case "resume":
                discoMayhem.resumeGame(player);
                break;
            case "end":
                discoMayhem.endGame(player);
                break;
            case "nuke_area":
                discoMayhem.nukeArea(player.getLocation(),50);
                break;
            default:
                player.sendMessage("Unknown command.");
                break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length == 1) {
        return List.of("start", "stop","resume", "end", "nuke_area");
    }
    return List.of();
    }
}