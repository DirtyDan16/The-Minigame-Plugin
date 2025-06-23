package me.stavgordeev.plugin.Minigames.BlueprintBazaar;

import me.stavgordeev.plugin.commands.MinigameCommandsSkeleton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BlueprintBazaarCommands extends MinigameCommandsSkeleton {
    private final BlueprintBazaar blueprintbazaar;
    public BlueprintBazaarCommands(BlueprintBazaar blueprintbazaar) {
        this.blueprintbazaar = blueprintbazaar;
    }

    @Override
    protected boolean handleCommand(Player player, Command command, String label, String[] args) {
        switch (args[0].toLowerCase()) {
            case "start":
                try {
                    blueprintbazaar.start(player);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "start_hard_mode":
                try {
                    blueprintbazaar.startFastMode(player);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "stop":
                blueprintbazaar.pauseGame(player);
                break;
            case "resume":
                blueprintbazaar.resumeGame(player);
                break;
            case "end":
                blueprintbazaar.endGame(player);
                break;
            case "nuke_area":
                blueprintbazaar.nukeArea(BlueprintBazaarConst.GAME_START_LOCATION, 50);
                break;
            case "spawn_build":
                blueprintbazaar.prepareNewBuild();
                break;
            case "showcase_all_builds":
                blueprintbazaar.loadAllSchematics();
                break;
            case "init_schematics":
                blueprintbazaar.initSchematics();
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
            return List.of("start", "stop", "start_hard_mode", "resume", "end", "nuke_area", "spawn_build", "showcase_all_builds", "init_schematics");
        }
        return List.of();
    }
}
