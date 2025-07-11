package me.stavgordeev.plugin.Minigames.HoleInTheWall

import org.bukkit.Bukkit.getWorld
import org.bukkit.Location
import org.bukkit.World


object HoleInTheWallConst {
    const val isInDevelopment: Boolean = false // If the plugin is in development mode, some features may be disabled or behave differently. this is so that the plugin can be tested easily without constantly tempering with the code.


    const val PLATFORMS_FOLDER: String = "platforms"
    const val WALLPACK_FOLDER: String = "wallpack"
    const val MAP_FOLDER: String = "map"
    const val GAME_FOLDER: String = "holeinthewall"

    const val HARD_CAP_MAX_POSSIBLE_AMOUNT_OF_WALLS: Int = 4

    const val DEFAULT_WALL_TRAVEL_LIFESPAN: Int = 20 // How many blocks the wall travels before it disappears. This is the default value, but can be overridden by the wall file itself.

    object Locations {
        val WORLD: World = getWorld("world") ?: throw IllegalStateException("World 'world' not found. Please ensure the world is loaded.")

        // The pivot point - everything is centered around this point. The idea is that this is the center of the map
        val PIVOT: Location = Location(WORLD,0.0, 130.0, 0.0)

        // The offset needed to center the arena (deco) relative to walls and the floor of the game. SHOULD NOT BE REFERENCED FOR LOCATIONS OTHER THAN THE PLAYER SPAWN.
        val CENTER_OF_MAP: Location = PIVOT.clone().add(1.0, 0.0, -1.0)

        val SPAWN: Location = PIVOT.clone().add(0.0, 3.0, 0.0) // The spawn point of the player in the game.

        val PLATFORM: Location = PIVOT.clone()

        const val DISTANCE_OF_WALL_FROM_CENTER_OF_PLATFORM: Double = 12.0

        val SOUTH_WALL_SPAWN: Location = PIVOT.clone().add(1.0, 1.0, DISTANCE_OF_WALL_FROM_CENTER_OF_PLATFORM)
        val NORTH_WALL_SPAWN: Location = PIVOT.clone().add(0.0, 1.0, -DISTANCE_OF_WALL_FROM_CENTER_OF_PLATFORM - 1.0)
        val WEST_WALL_SPAWN: Location = PIVOT.clone().add(-DISTANCE_OF_WALL_FROM_CENTER_OF_PLATFORM, 1.0, 0.0)
        val EAST_WALL_SPAWN: Location = PIVOT.clone().add(DISTANCE_OF_WALL_FROM_CENTER_OF_PLATFORM + 1.0, 1.0, -1.0)
    }
    
    object WallDifficulty {
        const val EASY: Int = 0
        const val MEDIUM: Int = 1
        const val HARD: Int = 2
        const val VERY_HARD: Int = 3
    }

    enum class WallSpawnerState {
        IDLE, // The spawner is not doing anything
        SPAWNING, // The spawner is currently spawning a wall
        INTENDING_TO_CREATE_WALL_IN_THE_SAME_DIRECTION, // The spawner is intending to create a wall in the same direction as the last wall
        INTENDING_TO_CREATE_WALL_IN_A_DIFFERENT_DIRECTION, // The spawner is intending to create a wall in a different direction than the last wall
        WAITING_FOR_NEXT_WALL, // The spawner is waiting for the next wall to spawn
    }

    object Timers {
        const val DELAY_BEFORE_STARTING_GAME: Long = 2*20 // in ticks
        const val GAME_DURATION: Int = 300 // in seconds

        val WALL_SPEED_UP_LANDMARKS: IntArray = intArrayOf(30, 60, 90, 120, 155, 200) // in seconds
        val INCREASE_WALL_DIFFICULTY_LANDMARKS: IntArray = intArrayOf(45, 90, 155) // in seconds
        val PLATFORM_SHRINKAGE_LANDMARKS: IntArray = intArrayOf(70, 155)

//        val WALL_SPEED: IntArray = intArrayOf(15, 12, 10, 7, 5, 4, 3, 2) //in ticks
        val WALL_SPEED: IntArray = intArrayOf(4) //in ticks


        val DELAY_BEFORE_SPAWNING_A_WALL_FROM_THE_SAME_DIRECTION: LongRange = 25L..30L // in ticks
        val DELAY_BEFORE_SPAWNING_A_WALL_FROM_A_DIFFERENT_DIRECTION: LongRange = 70L..80L // in ticks
    }
}