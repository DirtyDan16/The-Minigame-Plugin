package me.stavgordeev.plugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class MinigameCommandsSkeleton implements CommandExecutor, TabExecutor {

    /**
     * Executes the given command, returning its success
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be run by a player.");
            return false;
        }

        if (args.length == 0) {
            return true;
        }

        return handleCommand(player, command, label, args);
    }

    /**
     * Requests a list of possible completions for a command argument.
     * @param sender Source of the command.  For players tab-completing a
     *     command inside of a command block, this will be the player, not
     *     the command block.
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args The arguments passed to the command, including final
     *     partial argument to be completed
     * @return A List of possible completions for the final argument, or null
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return handleTabComplete(sender, command, label, args);
    }

    /**
     * Handles the command
     * @param player The player who executed the command
     * @param command The command that was executed
     * @param label The alias of the command that was used
     * @param args The arguments passed to the command
     * @return true if the command was handled, otherwise false
     */
    protected abstract boolean handleCommand(Player player, Command command, String label, String[] args);

    /**
     * Handles the tab completion
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args The arguments passed to the command
     * @return A List of possible completions for the final argument, or null
     */
    protected abstract @Nullable List<String> handleTabComplete(CommandSender sender, Command command, String label, String[] args);
}
