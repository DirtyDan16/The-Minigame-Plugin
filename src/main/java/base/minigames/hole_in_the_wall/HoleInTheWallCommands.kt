package base.minigames.hole_in_the_wall

import base.commands.MinigameCommandsSkeleton
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class HoleInTheWallCommands(private val holeInTheWall: HoleInTheWall) : MinigameCommandsSkeleton() {
    override fun handleCommand(sender: Player, command: Command, label: String, args: Array<String>): Boolean {
        when (args[0].lowercase(Locale.ENGLISH)) {
            "start","start_hard_mode" -> {
                when (args.size) {
                    1 -> return  error(sender, "Please specify a map name to start the game.")
                    2 -> {
                        try {
                            if (args[0] == "start_hard_mode") {
                                holeInTheWall.startFastMode(sender, args[1])
                            } else {
                                holeInTheWall.start(sender, args[1])
                            }
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        }
                    }
                    3 -> {
                        try {
                            if (args[0] == "start_hard_mode") {
                                holeInTheWall.startFastMode(sender, args[1], args[2])
                            } else {
                                holeInTheWall.start(sender, args[1], args[2])
                            }
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        }
                    }
                    else -> return error(sender, "Too many arguments")
                }
            }

            "set" -> {
                if (args.size == 1) return error(sender, "Please specify a setting to change.")
                when (args[1].lowercase(Locale.getDefault())) {
                    "wall_spawning_mode" -> {
                        if (args.size < 3) return error(sender, "Please specify the wall spawning mode.")

                        holeInTheWall.changeWallSpawningMode(args[2])
                    }
                    "wall_speed" -> {
                        if (args.size < 3) return error(sender, "Please specify the wall speed.")

                        try {
                            val speed = args[2].toInt()
                            holeInTheWall.wallSpeed = speed
                        } catch (_: NumberFormatException) {
                            return error(sender, "Invalid wall speed value")
                        }
                    }
                    else -> return error(sender, "Unknown setting: ${args[1]}.")
                }
            }

            "stop" -> holeInTheWall.pauseGame()
            "resume" -> holeInTheWall.resumeGame()
            "end" -> holeInTheWall.endGame()
            "spawn_wall" -> holeInTheWall.createNewWall()
            "clear_walls" -> holeInTheWall.clearWalls()
        }

        return true
    }

    override fun handleTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): List<String> {
        when (args.size) {
            1 -> {
                return listOf(
                    "start", "stop", "start_hard_mode", "resume", "end", "set",
                    "spawn_wall", "clear_walls"
                )
            }
            2 -> {
                return when (args[0]) {
                    "start" -> availableMaps
                    "set" -> listOf(
                        "wall_spawning_mode", "wall_speed"
                    )
                    else -> listOf()
                }
            }
            3 -> {
                return when (args[0]) {
                    "set" -> when (args[1]) {
                        "wall_spawning_mode" -> HITWConst.WallSpawnerMode.getModesAsAStringList()
                        "wall_speed" -> HITWConst.Timers.WALL_SPEED.map { it.toString() }
                        else -> listOf()
                    }
                    "start" -> HITWConst.WallSpawnerMode.getModesAsAStringList()
                    else -> listOf()
                }
            }
            else -> return listOf()
        }
    }



    companion object {
        private val availableMaps: List<String> = listOf<String>("Map1", "Map2", "Map3")
    }
}
