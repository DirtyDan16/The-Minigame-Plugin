package me.stavgordeev.plugin.Minigames.HoleInTheWall;

import me.stavgordeev.plugin.commands.MinigameCommandsSkeleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HoleInTheWallCommands extends MinigameCommandsSkeleton {
    private final HoleInTheWall holeInTheWall;
    public HoleInTheWallCommands(HoleInTheWall holeInTheWall) {
        this.holeInTheWall = holeInTheWall;
    }

    @Override
    protected boolean handleCommand(Player player, Command command, String label, String[] args) {
        switch (args[0].toLowerCase()) {
            case "start":
                if (args.length < 2) {
                    Bukkit.getServer().broadcast(Component.text("When starting this minigame, a given map name needs to be given.").color(NamedTextColor.RED));
                    return false;
                }
                try {
                    holeInTheWall.start(player,args[1]);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "start_hard_mode":
                try {
                    holeInTheWall.startFastMode(player);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "stop":
                holeInTheWall.pauseGame(player);
                break;
            case "resume":
                holeInTheWall.resumeGame(player);
                break;
            case "end":
                holeInTheWall.endGame(player);
                break;
            case "nuke_area":
                //holeInTheWall.nukeArea(holeInTheWallConst.GAME_START_LOCATION, 50);
                break;
        }

        return true;
    }

    @Override
    protected @Nullable List<String> handleTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("start", "stop", "start_hard_mode", "resume", "end", "nuke_area");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            return getAvailableMaps();
        }
        return List.of();
    }

    private static List<String> getAvailableMaps() {
        // This method should return a list of available maps for the minigame.
        // For now, we will return a static list of map names.
        // In a real implementation, this could be replaced with a dynamic list from a configuration file or database.
        return List.of("Map1", "Map2", "Map3");
    }
}
