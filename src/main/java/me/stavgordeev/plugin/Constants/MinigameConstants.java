// src/main/java/me/stavgordeev/plugin/MinigameConstants.java
package me.stavgordeev.plugin.Constants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Locale;

public class MinigameConstants {
    public static final int UPPER_BOUND_CHANGING_FLOOR_INTERVAL = 25;
    public static final int LOWER_BOUND_CHANGING_FLOOR_INTERVAL = 15;
    public static final int UPPER_BOUND_STOP_CHANGING_FLOOR_INTERVAL = 10;
    public static final int LOWER_BOUND_STOP_CHANGING_FLOOR_INTERVAL = 1;
    public static final int UPPER_BOUND_CHANGING_FLOOR_X_RADIUS = 7;
    public static final int LOWER_BOUND_CHANGING_FLOOR_X_RADIUS = 2;
    public static final int UPPER_BOUND_CHANGING_FLOOR_Z_RADIUS = 7;
    public static final int LOWER_BOUND_CHANGING_FLOOR_Z_RADIUS = 2;


    public static final int INITIAL_DELAY_TO_SELECT_A_FLOOR_MATERIAL = 25;
    public static final int DURATION_OF_STAYING_IN_A_FLOOR_WITH_ONLY_CHOSEN_MATERIAL = 30;

    public static final Material[] DEFAULT_FLOOR_BLOCK_TYPES = new Material[]{Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL,Material.YELLOW_WOOL};


    public static final World WORLD = Bukkit.getWorld("world");
    public static final Location GAME_START_LOCATION = new Location(WORLD, 0, 150, 0);
//    public static final Location EDGE_ONE_FLOOR_LOCATION = new Location(null, 0, 0, 0);
//    public static final Location EDGE_TWO_FLOOR_LOCATION = new Location(null, 0, 0, 0);
    public static final int MIN_INTERVAL = 1;
}