package base.commands

import com.google.errorprone.annotations.CheckReturnValue
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

abstract class MinigameCommandsSkeleton : TabExecutor {
    /**
     * Executes the given command, returning its success
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be run by a player.")
            return false
        }

        if (args.isEmpty()) {
            return true
        }

        return handleCommand(sender, command, label, args)
    }

    /**
     * Requests a list of possible completions for a command argument.
     * @param sender Source of the command.  For players tab-completing a
     * command inside of a command block, this will be the player, not
     * the command block.
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args The arguments passed to the command
     * @return A List of possible completions for the final argument, or null
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String> {
        return handleTabComplete(sender, command, label, args)
    }

    /**
     * Sends an error message to the player and returns false
     * @param sender The player to send the message to
     * @param msg The message to send
     * @return false because it is used exclusively in the onTabComplete method - which requires a boolean return type. false indicates the command was not successful
     */
    @CheckReturnValue
    protected fun error(sender: Player, msg: String): Boolean {
        sender.sendMessage(Component.text(msg).color(NamedTextColor.RED))
        return false
    }

    /**
     * Handles the command
     * @param sender The player who executed the command
     * @param command The command that was executed
     * @param label The alias of the command that was used
     * @param args The arguments passed to the command
     * @return true if the command was handled, otherwise false
     */
    protected abstract fun handleCommand(
        sender: Player,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean

    /**
     * Handles the tab completion
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args The arguments passed to the command
     * @return A List of possible completions for the final argument, or null
     */
    protected abstract fun handleTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): List<String>


    // This is a template for sub-commands, copy and paste it into a new minigamecommand file
//
//    enum class SubCommands {
//        START,
//        START_HARD_MODE,
//        PAUSE,
//        RESUME,
//        END,
//        NUKE_ARENA;
//
//        companion object {
//            /**
//             * Converts a string to a SubCommand enum value. Case-insensitive.
//             * @param str The string to convert
//             * @return The SubCommand enum value, or null if the string does not match any enum value
//             */
//            fun fromString(str: String): SubCommands? {
//                return entries.find { it.name.equals(str, ignoreCase = true) }
//            }
//        }
//    }
}
