@file:Suppress("DEPRECATION")

package base.utils

import base.MinigamePlugin.Companion.plugin
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** A simple wrapper class to hold an integer value by reference. Useful for passing integers to functions that need to modify them. */
class IntRef(var value: Int)

/**
 * Used as a method return type to indicate whether the method exited early for one reason or another or not.
 */
enum class ExitStatus {
    EARLY_EXIT,
    COMPLETED
}

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
     * If `conditionToCancel` is provided and returns true, the task will be canceled.
     *
     * @param checkInterval The interval in ticks to check the condition.
     * @param delayAfterConditionMet The delay in ticks after the condition is met before executing the action.
     * @param condition The condition to check.
     * @param conditionToCancel An optional condition to cancel the task.
     * @param action The action to execute when the condition is met.
     * @param actionToDoIfCanceled An optional action to execute if the task is cancelled.
     * @param actionToDoWhileWaitingForAConditionToOccur An optional action to execute while waiting for the condition to occur.
     * @param listOfRunnablesToAddTo An optional list to add the runnable to, for later cancellation.
     * @return A BukkitRunnable that can be canceled if needed.
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

    /**
     * A [BukkitRunnable] that can be paused and resumed. When paused, it keeps track of the remaining time until the next execution. When resumed, it continues from where it left off.
     * @param plugin The JavaPlugin instance.
     * @param periodTicks The period in ticks between each execution of the action. If null, the action will be executed only once after the initial delay.
     * @param remainingTicks The remaining ticks until the next execution when paused. Default is 0.
     * @param action The action to execute.
     */
    class PausableBukkitRunnable(
        private val plugin: JavaPlugin,
        private var remainingTicks: Long = 0L,
        private val periodTicks: Long? = null,
        private val action: () -> Unit
    ) {
        private var task: BukkitTask? = null
        private var lastStartTime: Long = 0L // in system ms

        private var isPaused: Boolean = true

        /**
         *  Used to dictate if this runnable shouldn't be used and needs to be removed from lists.
         *  This is flagged as true for run once runnables that have been executed.
         *
         *  Can also be manually flagged; however, it'll be removed only when calling [base.minigames.MinigameSkeleton.pauseGame].
         * */
        var shouldBeRemoved: Boolean = false

        /**
         * Starts/resumes the task. If the task is already running, this method does nothing.
         */
        fun start() {
            if (!isPaused) return
            isPaused = false

            lastStartTime = System.currentTimeMillis()

            task = if (periodTicks != null) {

                object : BukkitRunnable() {
                    override fun run() {
                        action()
                    }
                }.runTaskTimer(plugin, remainingTicks, periodTicks)
            } else {

                object : BukkitRunnable() {
                    override fun run() {
                        action()

                        // disable reusing this instance of Pausable BukkitRunnable
                        shouldBeRemoved = true
                    }
                }.runTaskLater(plugin, remainingTicks)
            }


            if (periodTicks != null) {
                remainingTicks = periodTicks
            }
        }

        /**
         * Pauses the task. If the task is already paused, this method does nothing.
         */
        fun pause() {
            if (isPaused) return
            isPaused = true

            task?.cancel()
            task = null


            val elapsedTicks = ((System.currentTimeMillis() - lastStartTime) * 20 / 1000)

            // If the task is a repeating timer, we'll need to have a module operation since the task repeats itself every 'periodTicks' ticks.
            // Otherwise, if the task is only a delayed run once task, we'll just calculate the difference in the 2 times.
            val elapsedTicksInCycle =
                if (periodTicks != null)
                    elapsedTicks % periodTicks
                else
                    elapsedTicks

            remainingTicks =  max(0,remainingTicks - elapsedTicksInCycle)
        }
    }

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

    fun <T> Collection<Pair<T, Int>>.getWeightedRandom(): T {
        val totalWeight = this.sumOf { it.second }
        var randomValue = Random.nextInt(totalWeight)
        for ((item, weight) in this) {
            randomValue -= weight
            if (randomValue < 0) {
                return item
            }
        }
        throw IllegalStateException("Should never reach here if weights are positive")
    }
}

