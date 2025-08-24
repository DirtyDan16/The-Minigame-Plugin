package base.minigames.maze_hunt

import org.bukkit.Location
import org.bukkit.World

object MHConst {
    object Locations {
        val WORLD: World = org.bukkit.Bukkit.getWorld("world") ?: throw IllegalStateException("World 'world' not found")
        val GAME_START_LOCATION = Location(WORLD, 0.0, 100.0, 0.0) // Replace null with your world object
        const val GAME_AREA_RADIUS = 50
    }
}
