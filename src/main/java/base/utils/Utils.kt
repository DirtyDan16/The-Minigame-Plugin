package base.utils

import base.MinigamePlugin.Companion.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

/** A simple wrapper class to hold an integer value by reference. Useful for passing integers to functions that need to modify them. */
class IntRef(var value: Int)

enum class Direction {
    NORTH, SOUTH, EAST, WEST;

    fun getClockwise(): Direction {
        return when (this) {
            NORTH -> EAST
            SOUTH -> WEST
            EAST -> SOUTH
            WEST -> NORTH
        }
    }

    fun getCounterClockwise(): Direction {
        return when (this) {
            NORTH -> WEST
            SOUTH -> EAST
            EAST -> NORTH
            WEST -> SOUTH
        }
    }

    fun getOpposite(): Direction {
        return when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
        }
    }
}

object Utils {

    /**
     * Activates a task after a condition is met, with an optional delay after the condition is met.
     * The task will be checked at regular intervals defined by `checkInterval`.
     * If `conditionToCancel` is provided and returns true, the task will be cancelled.
     *
     * @param checkInterval The interval in ticks to check the condition.
     * @param delayAfterConditionMet The delay in ticks after the condition is met before executing the action.
     * @param condition The condition to check.
     * @param conditionToCancel An optional condition to cancel the task.
     * @param action The action to execute when the condition is met.
     * @param actionToDoIfCanceled An optional action to execute if the task is cancelled.
     * @param actionToDoWhileWaitingForAConditionToOccur An optional action to execute while waiting for the condition to occur.
     * @param listOfRunnablesToAddTo An optional list to add the runnable to, for later cancellation.
     * @return A BukkitRunnable that can be cancelled if needed.
     */
    fun activateTaskAfterConditionIsMet(
        checkInterval: Long = 1L,
        delayAfterConditionMet: Long = 0L,
        condition: () -> Boolean,
        conditionToCancel: (() -> Boolean)? = null,
        action: Runnable,
        actionToDoIfCanceled: (() -> Unit)? = null,
        actionToDoWhileWaitingForAConditionToOccur: Runnable? = null,
        listOfRunnablesToAddTo: MutableList<BukkitRunnable>? = null
    ): BukkitRunnable {
        val runnable: BukkitRunnable = object : BukkitRunnable() {
            override fun run() {
                when {
                    conditionToCancel?.invoke() == true -> cancel()
                    condition.invoke() -> {
                        if (delayAfterConditionMet > 0L) {
                            Bukkit.getScheduler().runTaskLater(plugin, action, delayAfterConditionMet)
                        } else {
                            action.run()
                        }
                        cancel(false)
                    }
                    else -> actionToDoWhileWaitingForAConditionToOccur?.run()
                }
                return
            }

            override fun cancel() {
                cancel(true)
            }

            fun cancel(doActionWhenCanceled: Boolean){
                if (doActionWhenCanceled) actionToDoIfCanceled?.invoke()
                super.cancel()

                listOfRunnablesToAddTo?.remove(this)
            }
        }

        runnable.runTaskTimer(plugin, 0L, checkInterval)

        listOfRunnablesToAddTo?.add(runnable)

        return runnable
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

/**
 * Returns true with the given probability (chance).
 * @param chance A double between 0.0 and 1.0 representing the probability of returning true.
 * @return true with the given probability, false otherwise.
 */
fun successChance(chance: Double): Boolean {
    require(chance in 0.0..1.0) { "Chance must be between 0.0 and 1.0" }
    return Random.Default.nextDouble() < chance
}

inline fun doActionByChance(probability: Double, action: () -> Unit) {
    if (successChance(probability)) action()
}