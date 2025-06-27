package me.stavgordeev.plugin.Minigames.HoleInTheWall

import org.bukkit.Location


object HoleInTheWallConst {
    const val PLATFORMS_FOLDER: String = "platforms"
    const val WALLPACK_FOLDER: String = "wallpack"
    const val MAP_FOLDER: String = "map"
    const val GAME_FOLDER: String = "holeinthewall"


    const val DEFAULT_WALL_TRAVEL_LIFESPAN: Int = 20 // How many blocks the wall travels before it disappears. This is the default value, but can be overridden by the wall file itself.

    object Locations {
        val WORLD = org.bukkit.Bukkit.getWorld("world")
        val SPAWN: Location = Location(WORLD,0.0, 70.0, 0.0)
        
        val SOUTH_WALL_SPAWN: Location = Location(WORLD, 0.0, 70.0, -20.0)
        val NORTH_WALL_SPAWN: Location = Location(WORLD, 0.0, 70.0, 20.0)
        val WEST_WALL_SPAWN: Location = Location(WORLD, -20.0, 70.0, 0.0)
        val EAST_WALL_SPAWN: Location = Location(WORLD, 20.0, 70.0, 0.0)
    }
    
    object WallDifficulty {
        const val EASY: Int = 0
        const val MEDIUM: Int = 1
        const val HARD: Int = 2
        const val VERY_HARD: Int = 3
    }

    // Directions in which the wall can come from. i.e South means that the wall will come from the south side of the arena.
    // When a new Wall Object is created, it will be assigned a direction which it'll remember for various logic.
    enum class WallDirection {
        SOUTH, NORTH, WEST, EAST
    }

    object Timers {
        const val GAME_DURATION: Int = 300 // in seconds
        val WALL_SPEED_UP_LANDMARKS: IntArray = intArrayOf(30, 60, 90, 120, 155, 200) // in seconds
        val INCREASE_WALL_DIFFICULTY_LANDMARKS: IntArray = intArrayOf(45, 90, 155) // in seconds
        val PLATFORM_SHRINKAGE_LANDMARKS: IntArray = intArrayOf(70, 155)

        val WALL_SPEED: IntArray = intArrayOf(20, 15, 12, 10, 7, 5, 4, 3, 2) //in ticks
    }
}