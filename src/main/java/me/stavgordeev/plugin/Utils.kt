package me.stavgordeev.plugin

import me.stavgordeev.plugin.MinigamePlugin.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable

enum class Direction {
    NORTH, SOUTH, EAST, WEST;

    fun getClockwise(): Direction {
        return when (this) {
            Direction.NORTH -> EAST
            Direction.SOUTH -> WEST
            Direction.EAST -> SOUTH
            Direction.WEST -> NORTH
        }
    }

    fun getCounterClockwise(): Direction {
        return when (this) {
            Direction.NORTH -> WEST
            Direction.SOUTH -> EAST
            Direction.EAST -> NORTH
            Direction.WEST -> SOUTH
        }
    }

    fun getOpposite(): Direction {
        return when (this) {
            Direction.NORTH -> SOUTH
            Direction.SOUTH -> NORTH
            Direction.EAST -> WEST
            Direction.WEST -> EAST
        }
    }
}

object Utils {

    fun activateTaskAfterConditionIsMet( checkInterval: Long = 1L, condition: () -> Boolean, action: Runnable) {
        object : BukkitRunnable() {
            override fun run() {
                if (condition()) {
                    action.run()
                    // Stop checking once the condition is met
                    cancel()
                }
            }
        }.runTaskTimer(plugin, 0L, checkInterval)
    }

    fun activateTaskAfterConditionIsMet(delayToWaitAfterConditionIsMet: Long, checkInterval: Long = 1L,condition: () -> Boolean,action: Runnable) {
        object : BukkitRunnable() {
            override fun run() {
                if (condition()) {
                    // Delay the action to actually happen after the specified delay from delayToWaitAfterConditionIsMet
                    Bukkit.getScheduler().runTaskLater(plugin, action,delayToWaitAfterConditionIsMet)
                    // Stop checking once the condition is met
                    cancel()
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
