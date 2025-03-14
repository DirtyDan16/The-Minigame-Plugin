package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Listeners.PlayerDeathListener;
import me.stavgordeev.plugin.Minigames.BlueprintBazaar;
import me.stavgordeev.plugin.Minigames.DiscoMayhem;
import me.stavgordeev.plugin.commands.BlueprintBazaarCommands;
import me.stavgordeev.plugin.commands.DiscoMayhemCommands;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class MinigamePlugin extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this; // Initialize the plugin reference

        // Create the BlueprintBazaarBuilds folder if it doesn't exist
        schematicsFolder = new File(getDataFolder(), "BlueprintBazaarBuilds");
        if (!schematicsFolder.exists()) {
            schematicsFolder.mkdirs();  // Creates the folder if it doesn't exist
            getLogger().info("Created BlueprintBazaarBuilds folder.");
        } else {
            getLogger().info("BlueprintBazaarBuilds folder already exists.");
        }

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

    private File schematicsFolder;
    public File getSchematicsFolder() {
        return schematicsFolder;
    }


}
