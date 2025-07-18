package me.stavgordeev.plugin.Minigames.HoleInTheWall

import me.stavgordeev.plugin.commands.MinigameCommandsSkeleton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class HoleInTheWallCommands(private val holeInTheWall: HoleInTheWall) : MinigameCommandsSkeleton() {
    override fun handleCommand(player: Player, command: Command?, label: String?, args: Array<String>): Boolean {
        when (args[0].lowercase(Locale.getDefault())) {
            "start" -> {
                if (args.size < 2) {
                    Bukkit.getServer().broadcast(Component.text("When starting this minigame, a given map name needs to be given.").color(NamedTextColor.RED))
                    return false
                }
                if (args.size < 3) {
                    Bukkit.getServer().broadcast(
                        Component.text("When starting this minigame, you need to specify which gamemode you want to play.I f you choose 'Alternating', the mode will be dynamically changed throughout the game").color(NamedTextColor.RED))
                    return false
                }

                try {
                    holeInTheWall.start(player, args[1], args[2])
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }

            "start_hard_mode" -> try {
                holeInTheWall.startFastMode(player)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            "stop" -> holeInTheWall.pauseGame(player)
            "resume" -> holeInTheWall.resumeGame(player)
            "end" -> holeInTheWall.endGame(player)
            "change_wall_spawning_mode_to" -> holeInTheWall.changeWallSpawningMode(args[1])
            "spawn_wall" -> holeInTheWall.createNewWall()
            "clear_walls" -> holeInTheWall.clearWalls()
        }

        return true
    }

    override fun handleTabComplete(sender: CommandSender?, command: Command?, label: String?, args: Array<String?>): List<String> {
        when (args.size) {
            1 -> {
                return listOf(
                    "start", "stop", "start_hard_mode", "resume", "end", "change_wall_spawning_mode_to",
                    "spawn_wall", "clear_walls"
                )
            }
            2 -> {
                return when (args[0]) {
                    "start" -> availableMaps
                    "change_wall_spawning_mode_to" -> HITWConst.WallSpawnerMode.getModesAsAStringList()
                    else -> listOf()
                }
            }
            3 -> return when (args[0]) {
                "start" -> HITWConst.WallSpawnerMode.getModesAsAStringList()
                else -> listOf()
            }
            else -> return listOf()
        }
    }

    companion object {
        private val availableMaps: List<String> = listOf<String>("Map1", "Map2", "Map3")
    }
}
