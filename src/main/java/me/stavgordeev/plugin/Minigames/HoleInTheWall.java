package me.stavgordeev.plugin.Minigames;

import me.stavgordeev.plugin.MinigamePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static me.stavgordeev.plugin.Constants.HoleInTheWallConst.*;


public class HoleInTheWall extends MinigameSkeleton{


    private File allSchematicsForAGivenMap;
    private File[] platformSchematics;    //the platform stages for a given map
    private List<File> wallPackSchematics;     //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.
    private String mapName = ""; //the map name that is being played. gets a value on the start() method.

    //region ----Game Modifiers that change as the game progresses

    private int gameDuration = Timers.GAME_DURATION; //in seconds
    private int wallSpeed = Timers.WALL_SPEED[0]; //in ticks
    private int wallSpeedIndex = 0; //index of the wall speed in the array

    //the current wall difficulty in the pack. starts from EASY and increases as the game progresses.
    // note that previous wall difficulties are also used in the game, but less frequently.
    private WallDifficulty curWallDifficultyInPack = WallDifficulty.EASY;

    //endregion -----------------------------------------------------------------------------------


    public HoleInTheWall(Plugin plugin) {
        super(plugin);
    }

    public void start(Player player,String mapName) throws InterruptedException {
        this.mapName = mapName;
        super.start(player);

        //--------------
        startGameEvents();
    }

    private void startGameEvents() {
        // Start the game duration timer

    }

    @Override
    public void nukeArea(Location center, int radius) {

    }

    @Override
    public void prepareArea() {
        try {
            AreaPreparationHelper helper = new AreaPreparationHelper();
            File baseFolder = helper.getGameBaseFolder();
            helper.loadMapSchematics(baseFolder);
            if (allSchematicsForAGivenMap != null) {
                helper.processMapComponents();
            }
        } catch (SecurityException e) {
            handleGameError("Failed to access game files", e);
        } catch (NullPointerException e) {
            handleGameError("Required game files are missing", e);
        } catch (Exception e) {
            handleGameError("Unexpected error during area preparation", e);
        }
    }
    // Private inner class to encapsulate helper methods
    private class AreaPreparationHelper {
        private File getGameBaseFolder() {
            if (!(plugin instanceof MinigamePlugin minigamePlugin)) {
                throw new IllegalStateException("Invalid plugin type");
            }
            File baseFolder = minigamePlugin.getSchematicsFolder(GAME_FOLDER);
            Objects.requireNonNull(baseFolder, "Game base folder not found");
            return baseFolder;
        }

        private void loadMapSchematics(File baseFolder) {
            File[] files = Objects.requireNonNull(baseFolder.listFiles(), "No files found in base folder named " + baseFolder.getName());
            allSchematicsForAGivenMap = Arrays.stream(files)
                    .filter(file -> file.isFile() && file.getName().equals(mapName))
                    .findFirst()
                    .orElse(null);
        }

        private void processMapComponents() {
            File[] mapComponents = Objects.requireNonNull(allSchematicsForAGivenMap.listFiles(),
                    "No components found in map folder named " + allSchematicsForAGivenMap.getName());
            
            for (File component : mapComponents) {
                switch (component.getName()) {
                    case PLATFORMS_FOLDER -> platformSchematics = component.listFiles();
                    case WALLPACK_FOLDER -> loadWallPackSchematics(component);
                    case MAP_FOLDER -> { /* Reserved for future implementation */ }
                }
            }
        }

        private void loadWallPackSchematics(File wallPack) {
            wallPackSchematics = new ArrayList<>();
            File[] wallFiles = Objects.requireNonNull(wallPack.listFiles(), "No wall schematics found");
            Collections.addAll(wallPackSchematics, wallFiles);
        }
    }

    private void handleGameError(String message, Exception e) {
        endGame(thePlayer);
        throw new RuntimeException(message, e);
    }

    @Override
    public void prepareGameSetting(Player player) {

    }
}