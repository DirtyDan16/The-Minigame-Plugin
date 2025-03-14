// src/main/java/me/stavgordeev/plugin/commands/MinigameCommand.java
package me.stavgordeev.plugin.commands;

import me.stavgordeev.plugin.Constants.DiscoMayhemConst;
import me.stavgordeev.plugin.Minigames.DiscoMayhem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DiscoMayhemCommands extends MinigameCommandsSkeleton {
    private final DiscoMayhem discoMayhem;

    public DiscoMayhemCommands(DiscoMayhem discoMayhem) {
        this.discoMayhem = discoMayhem;
    }

    @Override
    protected boolean handleCommand(Player player, Command command, String label, String[] args) {
        switch (args[0].toLowerCase()) {
            case "start":
                try {
                    discoMayhem.start(player);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "start_hard_mode":
                try {
                    discoMayhem.startFastMode(player);
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
                discoMayhem.nukeArea(DiscoMayhemConst.GAME_START_LOCATION, 50);
                break;
            default:
                Bukkit.getServer().broadcast(Component.text("Unknown command.").color(NamedTextColor.RED));
                break;
        }
        return true;
    }

    @Override
    protected @Nullable List<String> handleTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "start_hard_mode", "resume", "end", "nuke_area");
        }
        return List.of();
    }
}