package me.stavgordeev.plugin;

import org.bukkit.Location;
import org.bukkit.Material;

public class Utils {
    public static void nukeGameArea(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location currentLocation = center.clone().add(x, y, z);
                    currentLocation.getBlock().setType(Material.AIR);
                }
            }
        }
    }

}
