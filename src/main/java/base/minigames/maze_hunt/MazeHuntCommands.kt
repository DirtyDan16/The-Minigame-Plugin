package base.minigames.maze_hunt

import base.commands.MinigameCommandsSkeleton
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MazeHuntCommands(val mazeHunt: MazeHunt) : MinigameCommandsSkeleton() {

    /**
     * All sub-commands for this minigame
     */
    enum class SubCommands {
        START,
        START_HARD_MODE,
        PAUSE,
        RESUME,
        END,
        NUKE_ARENA;

        companion object {
            /**
             * Converts a string to a SubCommand enum value. Case-insensitive.
             * @param str The string to convert
             * @return The SubCommand enum value, or null if the string does not match any enum value
             */
            fun fromString(str: String): SubCommands? {
                return entries.find { it.name.equals(str, ignoreCase = true) }
            }
        }
    }


    override fun handleCommand(
        sender: Player,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        when (SubCommands.fromString(args[0])) {
            SubCommands.START -> {
                if (mazeHunt.stopIfGameIsRunning()) return false
                mazeHunt.start(sender)
            }
            SubCommands.START_HARD_MODE -> {
                if (mazeHunt.stopIfGameIsRunning()) return false
                mazeHunt.startFastMode(sender)
            }
            SubCommands.PAUSE -> {
                if (mazeHunt.stopIfGameIsPaused()) return false
                mazeHunt.pauseGame()
            }
            SubCommands.RESUME -> {
                if (mazeHunt.stopIfGameIsNotPaused()) return false
                mazeHunt.resumeGame()
            }
            SubCommands.END -> {
                if (mazeHunt.stopIfGameIsNotRunning()) return false
                mazeHunt.endGame()
            }
            SubCommands.NUKE_ARENA -> mazeHunt.nukeArea()
            else -> return error(sender, "Unknown command.")
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        return when (args.size) {
            1 -> SubCommands.entries.map { it.name.lowercase()}
            else -> {listOf()}
        }
    }
}