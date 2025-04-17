package me.stavgordeev.plugin.Minigames;

import me.stavgordeev.plugin.MinigamePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

public class HoleInTheWall extends MinigameSkeleton{
    private File allSchematicsForAGivenMap; // The schematics for a given hitw map -  which come with their wall pack, and platforms.
    private String mapName = ""; //the map name that is being played. gets a value on the start() method.
    private File[] platformSchematics; //the platform stages for a given map
    private ArrayList<File> wallPackSchematics; //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.

    public HoleInTheWall(Plugin plugin) {
        super(plugin);
    }

    public void start(Player player,String mapName) throws InterruptedException {
        this.mapName = mapName;
        super.start(player);
    }

    @Override
    public void nukeArea(Location center, int radius) {

    }

    @Override
    public void prepareArea() {
        // Get the base folder "holeinthewall"
        File baseFolder = null;
        if (plugin instanceof MinigamePlugin) {
            baseFolder = ((MinigamePlugin) plugin).getSchematicsFolder("holeinthewall");
        }
        if (baseFolder == null) {
            throw new IllegalStateException("Base schematics folder not found");
        }
        // gets the schematics from a folder of the selected map.
        for (File file : Objects.requireNonNull(baseFolder.listFiles())) {
            if (file.isFile() && file.getName().equals(mapName)) {
                allSchematicsForAGivenMap = file;

                // get the platform schematics folder, the wall pack folder, and (if there is) the background folder.
                for (File mapPiece : Objects.requireNonNull(allSchematicsForAGivenMap.listFiles())) {
                    if (mapPiece.getName().equals("platforms")) {
                        platformSchematics = mapPiece.listFiles();
                    } else if (mapPiece.getName().equals("wallpack")) {
                        File wallPack = mapPiece;
                        wallPackSchematics = new ArrayList<>();
                        // add to the wall pack all the wall groups that are found from the wall pack folder.
                        Collections.addAll(wallPackSchematics, Objects.requireNonNull(wallPack.listFiles()));
                    } else if (mapPiece.getName().equals("map")) {
                        // implementation way later lol
                    }
                }
            }
        }
    }

    @Override
    public void prepareGameSetting(Player player) {

    }
}