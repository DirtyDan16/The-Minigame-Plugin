package me.stavgordeev.plugin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

enum class Direction {
    NORTH, SOUTH, EAST, WEST;
}

object Utils {
    @JvmStatic
    fun nukeGameArea(center: Location, radius: Int) {
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val currentLocation = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    currentLocation.getBlock().setType(Material.AIR)
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
    @JvmStatic
    fun initFloor(xLengthRad: Int, zLengthRad: Int, material: Material, center: Location, world: World?) {
        // Initialize the floor under the player to stone 1 block at a time. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1.
        for (x in -xLengthRad..xLengthRad) {
            for (z in -zLengthRad..zLengthRad) {
                val selectedLocation = Location(world, center.getX() + x, center.getY(), center.getZ() + z)
                selectedLocation.getBlock().setType(material)
            }
        }

        Bukkit.broadcastMessage("floor initialized")
    }
}
