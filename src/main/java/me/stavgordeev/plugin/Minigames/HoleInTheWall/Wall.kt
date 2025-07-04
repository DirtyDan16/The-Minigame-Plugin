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
        fun powerOnAndOffButton(block: Block) {
            val state: BlockState = block.state

            val powerableState: Powerable = state.blockData as Powerable

            // Change the state of the button to powered if it is not already powered
            if (!powerableState.isPowered) {
                powerableState.isPowered = true // Power the button
                state.blockData = powerableState
                state.update(true, true) // Update the block state

                // Now we turn off the button after a short delay of X ticks in order to simulate the button being pressed which activates the piston.

                object : BukkitRunnable() {
                    override fun run() {
                        powerableState.isPowered = false // Unpower the button
                        state.blockData = powerableState
                        state.update(true, true) // Update the block state
                    }
                }.runTaskLater(plugin, 5L)

            }
        }

        // -------------------------------------------------------------------------------------------- //

        if (lifespan <= 0) {
            this.shouldBeStopped = true // If the wall has reached its lifespan, it should be stopped (it'll be determined by the game logic if it should be removed or continue living on for later).

            //FOR NOW - all walls that have a lifespan of 0 will be removed from the game.
            //TODO: Implement logic to determine if the wall should be removed or not.
            this.shouldBeRemoved = true
        }

        //region ----Moving Wall Logic - Press Buttons on Pistons---------------------------------------------------

        val minX = bottomCorner.x; val maxX = topCorner.x
        val minZ = bottomCorner.z; val maxZ = topCorner.z
        val minY = bottomCorner.y; val maxY = topCorner.y

        // get the locations of all piston blocks within the bounding box of the wall
        val pistonLocations: Sequence<Location> = sequence {
            for (x in minX.toInt()..maxX.toInt()) {
                for (y in minY.toInt()..maxY.toInt()) {
                    for (z in minZ.toInt()..maxZ.toInt()) {
                        val block = bottomCorner.world.getBlockAt(x, y, z)
                        // Only check blocks that are pistons
                        if (block.type == Material.PISTON) yield(Location(bottomCorner.world, x.toDouble(), y.toDouble(), z.toDouble()))
                    }
                }
            }
        }

        // The faces of the pistons that we want to activate buttons on
        // For a given block, we need to check it's faces in the direction of the wall movement - only this way we can actually detect the buttons that are attached to the pistons.
        val faces = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

        // Now that we have the locations of all pistons, we can iterate through them and activate the buttons on their faces. we need to check each piston block's faces for buttons.
        pistonLocations.forEach { loc ->
            faces.forEach { face ->
                // Get the face of the block
                val side = loc.block.getRelative(face)
                // Activate the button at this location (if it exists)
                if (side.type == Material.STONE_BUTTON) {
                    powerOnAndOffButton(side)
                }
            }
        }
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