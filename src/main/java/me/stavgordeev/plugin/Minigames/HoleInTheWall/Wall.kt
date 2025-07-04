package me.stavgordeev.plugin.Minigames.HoleInTheWall

import me.stavgordeev.plugin.BuildLoader
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.BlockState
import org.bukkit.block.data.Powerable
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import me.stavgordeev.plugin.MinigamePlugin.plugin

class Wall(
    val wallFile: File,
    val wallDirection: HoleInTheWallConst.WallDirection,
) {
    var bottomCorner: Location
    var topCorner: Location
    var lifespan: Int = HoleInTheWallConst.DEFAULT_WALL_TRAVEL_LIFESPAN //How many blocks the wall travels before it disappears.
    val spawnLocation: Location = when (wallDirection) {
            HoleInTheWallConst.WallDirection.SOUTH -> HoleInTheWallConst.Locations.SOUTH_WALL_SPAWN
            HoleInTheWallConst.WallDirection.NORTH -> HoleInTheWallConst.Locations.NORTH_WALL_SPAWN
            HoleInTheWallConst.WallDirection.WEST -> HoleInTheWallConst.Locations.WEST_WALL_SPAWN
            HoleInTheWallConst.WallDirection.EAST -> HoleInTheWallConst.Locations.EAST_WALL_SPAWN
    }

    var shouldBeRemoved: Boolean = false // If the wall should be removed from the game.
    var shouldBeStopped: Boolean = false // If the wall should be stopped from moving. It doesn't mean it should be removed from the game, but it has the possibility (for example - Psych walls)

    init {
        // Load the wall file and validate its contents if necessary
        if (wallFile.isDirectory) {
            throw IllegalArgumentException("Wall file cannot be a directory: ${wallFile.path}")
        }
        if (!wallFile.exists()) {
            throw IllegalArgumentException("Wall file does not exist: ${wallFile.path}")
        }

        // Rotate the wall schematic based on the direction the wall comes from.
        // ie if a wall comes from the south, we need to load it facing north.
        when (wallDirection) {
            HoleInTheWallConst.WallDirection.SOUTH -> {
                BuildLoader.loadSchematicByDirection(wallFile, spawnLocation, "north")
            }
            HoleInTheWallConst.WallDirection.NORTH -> {
                BuildLoader.loadSchematicByDirection(wallFile, spawnLocation, "south")
            }
            HoleInTheWallConst.WallDirection.WEST -> {
                BuildLoader.loadSchematicByDirection(wallFile, spawnLocation, "east")
            }
            HoleInTheWallConst.WallDirection.EAST -> {
                BuildLoader.loadSchematicByDirection(wallFile, spawnLocation, "west")
            }
        }
        // Set the top and bottom corners based on the wall file and spawn location
        bottomCorner = BuildLoader.getBottomCornerOfBuild(wallFile, spawnLocation)
        topCorner = BuildLoader.getTopCornerOfBuild(wallFile, spawnLocation)
    }

    /**
     * Moves the wall in the specified direction by a singular block via activating the pistons.
     * Each wall has a lifespan, which is the number of blocks it can travel before it stops moving.
     * Each time the wall moves, its lifespan is decremented by 1.
     * If the wall has a lifespan of 0, it will be stopped, but not necessarily removed from the game. The game logic handles the logic for removing the wall.
     */
    fun move() {
        if (lifespan <= 0) {
            this.shouldBeStopped = true // If the wall has reached its lifespan, it should be stopped (it'll be determined by the game logic if it should be removed or continue living on for later).

            //FOR NOW - all walls that have a lifespan of 0 will be removed from the game.
            //TODO: Implement logic to determine if the wall should be removed or not.
            this.shouldBeRemoved = true
        }

        //region ----Moving Wall Logic - Press Buttons on Pistons---------------------------------------------------

        //endregion
        // region ---Update the bottom and top corners based on the wall direction, since in the physical world, the slime wall has moved.
        //endregion
        //region --- Update the Pistons' location so that they match the new wall location and aren't left behind.
        //endregion
        //region ---- After successfully moving the wall, we need to re-add the buttons that are attached to the pistons, since they will be removed when the wall is moved.
        //endregion
        lifespan--
    }
}