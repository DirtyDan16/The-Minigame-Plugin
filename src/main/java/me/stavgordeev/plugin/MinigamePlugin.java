package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Listeners.PlayerDeathListener;
import me.stavgordeev.plugin.commands.MinigameCommand;
import me.stavgordeev.plugin.commands.PushBlockCommand;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MinigamePlugin extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this; // Initialize the plugin reference
        DiscoMayhem discoMayhem = new DiscoMayhem(this);

        // Register the event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(discoMayhem), this);

        // Register the commands
        //Objects.requireNonNull(getCommand("push_block")).setExecutor(new PushBlockCommand(this));
        Objects.requireNonNull(getCommand("minigame")).setExecutor(new MinigameCommand(discoMayhem)); // Register the command relating to the minigame DiscoMayhem.

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


}
