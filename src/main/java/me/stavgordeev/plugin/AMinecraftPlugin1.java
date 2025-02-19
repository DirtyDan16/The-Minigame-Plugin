package me.stavgordeev.plugin;

import me.stavgordeev.plugin.commands.MinigameCommand;
import me.stavgordeev.plugin.commands.PushBlockCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class AMinecraftPlugin1 extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this; // Initialize the plugin reference

        //getServer().getPluginManager().registerEvents(new PlayerMovementListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);

        Objects.requireNonNull(getCommand("push_block")).setExecutor(new PushBlockCommand(this));
        Objects.requireNonNull(getCommand("minigame")).setExecutor(new MinigameCommand(new Minigame(this)));

//        Objects.requireNonNull(this.getCommand("give_diamonds")).setExecutor(new CommandExecutor() {
//            @Override
//            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
//                return giveDiamondsCommand(sender, command, label, args);
//            }
//        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

//    public boolean giveDiamondsCommand(CommandSender sender, Command command, String label, String[] args) {
//        if (sender instanceof Player) {
//            Player player = (Player) sender;
//            ItemStack diamonds = new ItemStack(Material.DIAMOND, 3);
//            player.getInventory().addItem(diamonds);
//            player.sendMessage("You have been given 3 diamonds!");
//            return true;
//        } else {
//            sender.sendMessage("This command can only be run by a player.");
//            return false;
//        }
//    }
}
