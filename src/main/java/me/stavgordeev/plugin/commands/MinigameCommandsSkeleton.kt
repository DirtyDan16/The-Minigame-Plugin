package me.stavgordeev.plugin.commands

import com.google.errorprone.annotations.CheckReturnValue
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player

abstract class MinigameCommandsSkeleton : CommandExecutor, TabExecutor {
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

        if (args.size == 0) {
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
     * @param args The arguments passed to the command, including final
     * partial argument to be completed
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

    @CheckReturnValue
    protected fun error(player: Player, msg: String): Boolean {
        player.sendMessage(Component.text(msg).color(NamedTextColor.RED))
        return false
    }

    /**
     * Handles the command
     * @param player The player who executed the command
     * @param command The command that was executed
     * @param label The alias of the command that was used
     * @param args The arguments passed to the command
     * @return true if the command was handled, otherwise false
     */
    protected abstract fun handleCommand(
        player: Player,
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
}
