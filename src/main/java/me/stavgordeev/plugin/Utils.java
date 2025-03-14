package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Constants.DiscoMayhemConst;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

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

    /**
     * Initializes the floor under the player to a specific material.
     * @param xLengthRad The x radius of the floor
     * @param zLengthRad The z radius of the floor
     * @param material The material to set the floor to
     * @param center The center of the floor
     * @param world The world to set the floor in
     */
    public static void initFloor(int xLengthRad, int zLengthRad, Material material, Location center, World world) {
        // Initialize the floor under the player to stone 1 block at a time. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1.
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                Location selectedLocation = new Location(world, center.getX() + x, center.getY(), center.getZ() + z);
                selectedLocation.getBlock().setType(material);
            }
        }

        Bukkit.broadcastMessage("floor initialized");
    }


}
