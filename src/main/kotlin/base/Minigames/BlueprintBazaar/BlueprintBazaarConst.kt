package base.Minigames.BlueprintBazaar

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World

object BlueprintBazaarConst {
    val WORLD: World? = Bukkit.getWorld("world")

    object Locations {
        val GAME_START_LOCATION: Location = Location(WORLD, 0.0, 150.0, 0.0)
        val LEFT_BUILD_PLOT: Location = Location(WORLD, GAME_START_LOCATION.x - 10, GAME_START_LOCATION.y + 2, GAME_START_LOCATION.z + 10)
        val CENTER_BUILD_PLOT: Location = Location(WORLD, GAME_START_LOCATION.x, GAME_START_LOCATION.y + 2, GAME_START_LOCATION.z + 10)
        val RIGHT_BUILD_PLOT: Location = Location(WORLD, GAME_START_LOCATION.x + 10, GAME_START_LOCATION.y + 2, GAME_START_LOCATION.z + 10)
    }

    //public static final Location CENTER_OF_A_BUILD_PLOT = new Location(WORLD, -3, 3, 0); // The center of a build plot. needs to be added to the build plot location to get the center of the build plot.
    const val GAME_AREA_RADIUS: Int = 30
}
