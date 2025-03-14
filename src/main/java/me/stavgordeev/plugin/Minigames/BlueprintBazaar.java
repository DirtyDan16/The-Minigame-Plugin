package me.stavgordeev.plugin.Minigames;

import me.stavgordeev.plugin.BuildLoader;
import me.stavgordeev.plugin.Constants.BlueprintBazaarConst;
import me.stavgordeev.plugin.MinigamePlugin;
import me.stavgordeev.plugin.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class BlueprintBazaar extends MinigameSkeleton {

    private final File[] allSchematics; // The builds.
    private ArrayList<File> availableSchematics; // The builds. when a build is chosen, it is removed from this list


    public BlueprintBazaar (Plugin plugin) {
        super(plugin);
        // Gets the schematics folder from the MinigamePlugin.java. This is where the builds are stored.
        if (plugin instanceof MinigamePlugin) {
            File schematicsFolder = ((MinigamePlugin) plugin).getSchematicsFolder();// The folder where the builds are stored
            this.allSchematics = schematicsFolder.listFiles(); // Gets the builds from the folder
        } else {
        throw new IllegalArgumentException("Plugin must be an instance of MinigamePlugin");
        }
    }

    @Override
    public void start(Player player) throws InterruptedException {
        super.start(player);

        initSchematics();// Initializes the availableSchematics list

        new BukkitRunnable() {
            int index = 0;

            // This method is called every 2 seconds
            public void run() {
                if (index >= allSchematics.length) {
                    cancel();
                    return;
                }
                prepareNewBuild();
                index++;
            }
        }.runTaskTimer(plugin, 0L, 40L); // 40L = 2 seconds (20 ticks per second)
    }

    @Override
    public void startFastMode(Player player) throws InterruptedException {
        super.startFastMode(player);
    }

    @Override
    public void pauseGame(Player player) {
        super.pauseGame(player);
    }

    @Override
    public void resumeGame(Player player) {
        super.resumeGame(player);
    }

    @Override
    public void endGame(Player player) {
        super.endGame(player);
    }

    @Override
    public boolean isPlayerInGame(Player player) {
        return super.isPlayerInGame(player);
    }

    @Override
    public void nukeArea(Location center, int radius) {
        Utils.nukeGameArea(center, radius);
    }


    @Override
    public void prepareArea() {
        nukeArea(BlueprintBazaarConst.GAME_START_LOCATION, BlueprintBazaarConst.GAME_AREA_RADIUS);
        Utils.initFloor(20,20, Material.RED_WOOL, BlueprintBazaarConst.GAME_START_LOCATION,BlueprintBazaarConst.WORLD);
    }

    @Override
    public void prepareGameSetting(Player player) {
        player.teleport(BlueprintBazaarConst.GAME_START_LOCATION.clone().add(0, 8, 0)); // Teleport the player to the start location
    }

    /**
     * Initializes the availableSchematics list with all the builds in the schematics folder.
     */
    public void initSchematics() {
        // Adds the builds to the availableSchematics list
        assert allSchematics != null;
        availableSchematics = new ArrayList<>(Arrays.asList(allSchematics));
    }

    /**
     * Loads all the builds in the schematics folder. The builds are loaded in a grid pattern.
     */
    public void loadAllSchematics() {
        int index = 0;
        // Load all the builds in the schematics folder
        // Load the schematic relative to the center build plot. The x and z coordinates are Modified in a way that makes the builds appear in a grid.
        for (File schematic : allSchematics) {
            // Calculate the x, y, and z coordinates for the build
            int curX = (int) BlueprintBazaarConst.CENTER_BUILD_PLOT.x()+(10*(index%6)),
                curY = (int) (BlueprintBazaarConst.CENTER_BUILD_PLOT.y()),
                curZ = (int) (BlueprintBazaarConst.CENTER_BUILD_PLOT.z() + 10 * (index/6));

            // Initialize the floor for this build
            Utils.initFloor(6,6, Material.RED_WOOL,
                    new Location(BlueprintBazaarConst.WORLD, curX-3, curY-2, curZ),BlueprintBazaarConst.WORLD);
            // Load the schematic
            BuildLoader.loadSchematic(schematic, BlueprintBazaarConst.WORLD, curX, curY, curZ);

            // Increment the index for the position of the next build
            index++;
        }
    }

    public void prepareNewBuild() {
        if (availableSchematics.isEmpty()) {
            // Handle the case where there are no available schematics
            plugin.getLogger().severe("No available schematics to choose from.");
            return;
        }


        // Choose a random build from the schematics folder and delete it from the list
        Random getARandomBuild = new Random();
        File chosenBuild = availableSchematics.remove(getARandomBuild.nextInt(availableSchematics.size()));

        // Create the new build
        createNewBuild(chosenBuild, BlueprintBazaarConst.CENTER_BUILD_PLOT);
    }

    private void createNewBuild(File chosenBuild, Location location) {
        Bukkit.getServer().broadcast(Component.text("New building created!"));
        BuildLoader.loadSchematic(chosenBuild, BlueprintBazaarConst.WORLD, (int) location.x(), (int) location.y(), (int) location.z());
    }
}
