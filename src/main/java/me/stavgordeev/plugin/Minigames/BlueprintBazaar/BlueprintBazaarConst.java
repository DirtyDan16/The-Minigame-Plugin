package me.stavgordeev.plugin.Minigames.BlueprintBazaar;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class BlueprintBazaarConst {
    public static final World WORLD = Bukkit.getWorld("world");
    public static final Location GAME_START_LOCATION = new Location(WORLD, 0, 150, 0);
    public static final Location LEFT_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x()-10, GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);
    public static final Location CENTER_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x(), GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);
    public static final Location RIGHT_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x()+10, GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);

    //public static final Location CENTER_OF_A_BUILD_PLOT = new Location(WORLD, -3, 3, 0); // The center of a build plot. needs to be added to the build plot location to get the center of the build plot.

    public static final int GAME_AREA_RADIUS = 50;
}
