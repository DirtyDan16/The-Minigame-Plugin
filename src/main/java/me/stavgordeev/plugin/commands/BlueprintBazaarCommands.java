package me.stavgordeev.plugin.commands;

import me.stavgordeev.plugin.Minigames.BlueprintBazaar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlueprintBazaarCommands extends MinigameCommandsSkeleton {

    public BlueprintBazaarCommands(BlueprintBazaar discoMayhem) {
    }

    @Override
    protected boolean handleCommand(Player player, Command command, String label, String[] args) {
        return false;
    }

    @Override
    protected @Nullable List<String> handleTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
