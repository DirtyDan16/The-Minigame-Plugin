package me.stavgordeev.plugin

import me.stavgordeev.plugin.MinigamePlugin.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable

enum class Direction {
    NORTH, SOUTH, EAST, WEST;
}

object Utils {

    fun runTaskWhen(
        condition: () -> Boolean,
        checkInterval: Long = 1L, //in ticks
        action: Runnable
    ) {
        object : BukkitRunnable() {
            override fun run() {
                if (condition()) {
                    action.run()
                    cancel() // Stop checking once condition is met
                }
            }
        }.runTaskTimer(plugin, 0L, checkInterval)
    }



    @JvmStatic
    fun nukeGameArea(center: Location, radius: Int) {
        for (x in -radius..radius) {
            for (y in -radius..radius) {
                for (z in -radius..radius) {
                    val currentLocation = center.clone().add(x.toDouble(), y.toDouble(), z.toDouble())
                    currentLocation.block.type = Material.AIR
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
                val selectedLocation = Location(world, center.x + x, center.y, center.z + z)
                selectedLocation.block.type = material
            }
        }

        Bukkit.broadcastMessage("floor initialized")
    }
}
