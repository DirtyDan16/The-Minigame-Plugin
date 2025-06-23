package me.stavgordeev.plugin.Minigames.BlueprintBazaar;

import me.stavgordeev.plugin.BuildLoader;
import me.stavgordeev.plugin.MinigamePlugin;
import me.stavgordeev.plugin.Minigames.MinigameSkeleton;
import me.stavgordeev.plugin.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

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
            File schematicsFolder = ((MinigamePlugin) plugin).getSchematicsFolder("blueprintbazaar");// The folder where the builds are stored
            this.allSchematics = schematicsFolder.listFiles(); // Gets the builds from the folder
        } else {
        throw new IllegalArgumentException("Plugin must be an instance of MinigamePlugin");
        }
    }

    @Override
    public void start(Player player) throws InterruptedException {
        super.start(player);

        initSchematics();// Initializes the availableSchematics list
    }

    //fixme: doesn't get rid of the top of given schematics when we remove an old schematic. didn't manage to solve it.
    private void cycleThroughSchematics() {
        new BukkitRunnable() {
            int index = 0;
            int[] currentBorders = null;
            File schematic = null;

            @Override
            public void run() {
                // Delete previous build if it exists
                if (schematic != null) {
                    deleteBuild(currentBorders);
                }

                // Check if we've gone through all schematics
                if (index >= allSchematics.length) {
                    cancel();
                    return;
                }

                // Display new schematic
                schematic = chooseNewBuild();
                if (schematic != null && schematic.exists()) {
                    BuildLoader.loadSchematic(schematic,BlueprintBazaarConst.WORLD, BlueprintBazaarConst.CENTER_BUILD_PLOT);
                    currentBorders = BuildLoader.getBuildBorders(schematic, BlueprintBazaarConst.CENTER_BUILD_PLOT);
                }

                // Move to next schematic
                index++;
            }
        }.runTaskTimer(plugin, 0L, 100L);
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

    /**
     * Chooses a new build from the availableSchematics list. The build is taken out of the available list
     * @return The chosen build
     */
    private File chooseNewBuild() {
        if (availableSchematics.isEmpty()) {
            // Handle the case where there are no available schematics
            plugin.getLogger().severe("No available schematics to choose from.");
            return null;
        }
        // Choose a random build from the schematics folder and delete it from the list
        Random getARandomBuild = new Random();
        File chosenBuild = availableSchematics.remove(getARandomBuild.nextInt(availableSchematics.size()));

        return chosenBuild;
    }

    public void prepareNewBuild() {
        File chosenBuild = chooseNewBuild();
        // Create the new build
        createNewBuild(chosenBuild, BlueprintBazaarConst.CENTER_BUILD_PLOT);
    }

    private void deleteBuild(int @NotNull [] buildBorders) {

        int minX = buildBorders[0], maxX = buildBorders[1],
            minY = buildBorders[2], maxY = buildBorders[3],
            minZ = buildBorders[4], maxZ = buildBorders[5];

        // Set all blocks within the boundaries to air
            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    for (int z = minZ; z < maxZ; z++) {
                        Block block = BlueprintBazaarConst.WORLD.getBlockAt(x, y, z);
                        block.setType(Material.AIR);
                    }
                }
            }
    }


    private void createNewBuild(File chosenBuild, Location location) {
        Bukkit.getServer().broadcast(Component.text("New building created!"));
        BuildLoader.loadSchematic(chosenBuild, BlueprintBazaarConst.WORLD, (int) location.x(), (int) location.y(), (int) location.z());
    }
}
