package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Listeners.PlayerDeathListener;
import me.stavgordeev.plugin.Minigames.BlueprintBazaar.BlueprintBazaar;
import me.stavgordeev.plugin.Minigames.DiscoMayhem.DiscoMayhem;
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HoleInTheWall;
import me.stavgordeev.plugin.Minigames.BlueprintBazaar.BlueprintBazaarCommands;
import me.stavgordeev.plugin.Minigames.DiscoMayhem.DiscoMayhemCommands;
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HoleInTheWallCommands;
import me.stavgordeev.plugin.commands.MiscCommands;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class MinigamePlugin extends JavaPlugin {

    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this; // Initialize the plugin reference

        initSchematicsFolders();

        DiscoMayhem discoMayhem = new DiscoMayhem(this);
        BlueprintBazaar blueprintBazaar = new BlueprintBazaar(this);
        HoleInTheWall holeInTheWall = new HoleInTheWall(this);

        // Register the event listeners
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(discoMayhem), this);

        Objects.requireNonNull(getCommand("mg_disco_mayhem")).setExecutor(new DiscoMayhemCommands(discoMayhem)); // Register the command relating to the minigame DiscoMayhem.
        Objects.requireNonNull(getCommand("mg_blueprint_bazaar")).setExecutor(new BlueprintBazaarCommands(blueprintBazaar)); // Register the command relating to the minigame BlueprintBazaar.
        Objects.requireNonNull(getCommand("mg_hole_in_the_wall")).setExecutor(new HoleInTheWallCommands(holeInTheWall));
        Objects.requireNonNull(getCommand("misc")).setExecutor(new MiscCommands(this));
    }

    private void initSchematicsFolders() {
        // Create the BlueprintBazaarBuilds folder if it doesn't exist
        blueprintBazaarSchematicsFolder = new File(getDataFolder(), "BlueprintBazaarBuilds");
        if (!blueprintBazaarSchematicsFolder.exists()) {
            blueprintBazaarSchematicsFolder.mkdirs();  // Creates the folder if it doesn't exist
            getLogger().info("Created BlueprintBazaarBuilds folder.");
        } else {
            getLogger().info("BlueprintBazaarBuilds folder already exists.");
        }

        holeInTheWallSchematicsFolder = new File(getDataFolder(), "HoleInTheWall");
        if (!blueprintBazaarSchematicsFolder.exists()) {
            blueprintBazaarSchematicsFolder.mkdirs();  // Creates the folder if it doesn't exist
            getLogger().info("Created HoleInTheWall folder.");
        } else {
            getLogger().info("HoleInTheWall folder already exists.");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private File blueprintBazaarSchematicsFolder,holeInTheWallSchematicsFolder;
    public File getSchematicsFolder(String minigame) {
        return switch (minigame) {
            case "blueprintbazaar" -> blueprintBazaarSchematicsFolder;
            case "holeinthewall" -> holeInTheWallSchematicsFolder;
            default -> throw new IllegalStateException("Unexpected value: " + minigame);
        };
    }


}
