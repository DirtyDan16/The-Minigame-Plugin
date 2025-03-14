package me.stavgordeev.plugin.Constants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class BlueprintBazaarConst {
    public static final World WORLD = Bukkit.getWorld("world");
    public static final Location GAME_START_LOCATION = new Location(WORLD, 0, 150, 0);
    public static final Location LEFT_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x()-10, GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);
    public static final Location CENTER_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x(), GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);
    public static final Location RIGHT_BUILD_PLOT = new Location(WORLD, GAME_START_LOCATION.x()+10, GAME_START_LOCATION.y()+2, GAME_START_LOCATION.z()+10);


    public static final int GAME_AREA_RADIUS = 50;
}
