package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Listeners.PlayerDeathListener;
import me.stavgordeev.plugin.Minigames.BlueprintBazaar;
import me.stavgordeev.plugin.Minigames.DiscoMayhem;
import me.stavgordeev.plugin.commands.BlueprintBazaarCommands;
import me.stavgordeev.plugin.commands.DiscoMayhemCommands;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class MinigamePlugin extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this; // Initialize the plugin reference
        DiscoMayhem discoMayhem = new DiscoMayhem(this);
        BlueprintBazaar blueprintBazaar = new BlueprintBazaar(this);

        // Register the event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(discoMayhem), this);

        Objects.requireNonNull(getCommand("mg_disco_mayhem")).setExecutor(new DiscoMayhemCommands(discoMayhem)); // Register the command relating to the minigame DiscoMayhem.
        Objects.requireNonNull(getCommand("mg_blueprint_bazaar")).setExecutor(new BlueprintBazaarCommands(blueprintBazaar)); // Register the command relating to the minigame BlueprintBazaar.
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }


}
