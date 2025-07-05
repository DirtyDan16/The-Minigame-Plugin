package me.stavgordeev.plugin.Minigames.HoleInTheWall

import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
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
    val directionWallComesFrom: HoleInTheWallConst.WallDirection,
) {

    //region -- Properties --
    var wallRegion: CuboidRegion
    val locationOfPistons: MutableList<Location>

    var lifespan: Int =
        HoleInTheWallConst.DEFAULT_WALL_TRAVEL_LIFESPAN //How many blocks the wall travels before it disappears.

    val spawnLocation: Location = when (directionWallComesFrom) {
        HoleInTheWallConst.WallDirection.SOUTH -> HoleInTheWallConst.Locations.SOUTH_WALL_SPAWN
        HoleInTheWallConst.WallDirection.NORTH -> HoleInTheWallConst.Locations.NORTH_WALL_SPAWN
        HoleInTheWallConst.WallDirection.WEST -> HoleInTheWallConst.Locations.WEST_WALL_SPAWN
        HoleInTheWallConst.WallDirection.EAST -> HoleInTheWallConst.Locations.EAST_WALL_SPAWN
    }
    val directionWallIsFacing: String = when (directionWallComesFrom) {
        HoleInTheWallConst.WallDirection.SOUTH -> "north"
        HoleInTheWallConst.WallDirection.NORTH -> "south"
        HoleInTheWallConst.WallDirection.WEST -> "east"
        HoleInTheWallConst.WallDirection.EAST -> "west"
    }

    var shouldBeRemoved: Boolean = false // If the wall should be removed from the game.
    var shouldBeStopped: Boolean =
        false // If the wall should be stopped from moving. It doesn't mean it should be removed from the game, but it has the possibility (for example - Psych walls)

    //endregion

    init {
        // Load the wall file and validate its contents if necessary
        if (wallFile.isDirectory) {
            throw IllegalArgumentException("Wall file cannot be a directory: ${wallFile.path}")
        }
        if (!wallFile.exists()) {
            throw IllegalArgumentException("Wall file does not exist: ${wallFile.path}")
        }

        // Rotate the wall schematic based on the direction the wall comes from.
        // i.e., if a wall comes from the south, we need to load it facing north.
        BuildLoader.loadSchematicByDirection(wallFile, spawnLocation, directionWallIsFacing)

        // we also set the volume of the wall based on the spawn location and the wall direction.
        wallRegion = BuildLoader.getRotatedRegion(wallFile, spawnLocation, directionWallIsFacing)

        // Get the locations of all pistons in the wall region. important that this is done after the wall region is set, since the method relies on the wall region to get the piston locations.
        locationOfPistons = getPistonLocations()
    }

    private fun getPistonLocations(): MutableList<Location> {
        // Get the locations of all piston blocks within the bounding box of the wall
        val locations = mutableListOf<Location>()

        for (x in wallRegion.minimumPoint.x..wallRegion.maximumPoint.x) {
            for (y in wallRegion.minimumPoint.y..wallRegion.maximumPoint.y) {
                for (z in wallRegion.minimumPoint.z..wallRegion.maximumPoint.z) {
                    val block = HoleInTheWallConst.Locations.WORLD.getBlockAt(x, y, z)
                    // Only check blocks that are pistons
                    if (block.type == Material.PISTON) {
                        locations.add(
                            Location(
                                HoleInTheWallConst.Locations.WORLD,
                                x.toDouble(),
                                y.toDouble(),
                                z.toDouble()
                            )
                        )
                    }
                }
            }
        }
        return locations;
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


            }
        }

        // -------------------------------------------------------------------------------------------- //

        if (lifespan <= 0) {
            this.shouldBeStopped =
                true // If the wall has reached its lifespan, it should be stopped (it'll be determined by the game logic if it should be removed or continue living on for later).

            //FOR NOW - all walls that have a lifespan of 0 will be removed from the game.
            //TODO: Implement logic to determine if the wall should be removed or not.
            this.shouldBeRemoved = true
        }

        //region ----Moving Wall Logic - add and Press Buttons on Pistons---------------------------------------------------

        // We'll iterate through the locations of all pistons. we'll add behind them a stone button and activate the buttons on their faces.
        locationOfPistons.forEach { loc ->
            // the direction the wall is facing is the same as the direction the piston is facing. calculate the button location based on the direction the wall is facing.
            val buttonLocation: Location = when (directionWallIsFacing) {
                "south" -> loc.clone().add(0.0, 0.0, -1.0)
                "north" -> loc.clone().add(0.0, 0.0, 1.0)
                "west" -> loc.clone().add(1.0, 0.0, 0.0)
                "east" -> loc.clone().add(-1.0, 0.0, 0.0)
                else -> {
                    throw IllegalArgumentException("HITW: Invalid wall direction: $directionWallIsFacing")
                }
            }


            // Check if the block behind the piston is air, if it is not, then we can't place a button there.
            if (buttonLocation.block.type != Material.AIR) {
                throw IllegalStateException("HITW: expected air behind the piston, but found ${buttonLocation.block.type} at ${buttonLocation.block.location}")
            }

            // Get the block behind the piston where we will place the button.
            val buttonBlock: Block = buttonLocation.block
            buttonBlock.type = Material.STONE_BUTTON

            // now we need the button to lay flat against the piston, so we need to set the block data of the button to face *against* the piston.
            val data = buttonBlock.blockData as org.bukkit.block.data.type.Switch

            data.facing = when (directionWallIsFacing) {
                "south" -> BlockFace.NORTH
                "north" -> BlockFace.SOUTH
                "west" -> BlockFace.EAST
                "east" -> BlockFace.WEST
                else -> throw IllegalArgumentException("Invalid wall direction: $directionWallIsFacing")
            }
            // set the direction of the button to face the piston.
            buttonBlock.blockData = data

            // Now we can power the button to activate the piston.
            powerOnAndOffButton(buttonBlock)
        }
        //endregion


        // IMPORTANT: We need to let the pistons extend before we move the wall region, so we will wait for a lil before excecuting the entire logic of this function..

        object : BukkitRunnable() {
            override fun run() {
                // After the pistons have been activated, we can now move the wall region and update the pistons' locations.
                updateWallRegionAndPistons()
            }
        }.runTaskLater(plugin, 2L)
    }

    private fun updateWallRegionAndPistons() {
        // region ---Update the region of the wall based on the wall direction, since in the physical world, the slime wall has moved.

        //shift the wall region in the direction it is facing by 1 block.
        when (directionWallIsFacing) {
            "south" -> wallRegion.shift(BlockVector3.at(0, 0, 1))
            "north" -> wallRegion.shift(BlockVector3.at(0, 0, -1))
            "west" -> wallRegion.shift(BlockVector3.at(-1, 0, 0))
            "east" -> wallRegion.shift(BlockVector3.at(1, 0, 0))
        }
        //endregion

        //region --- Update the Pistons' location so that they match the new wall location and aren't left behind.

        locationOfPistons.forEach { location ->
            // First we need to remove the pistons from their current locations, so that they can be moved to their new locations.
            location.block.type = Material.AIR

            //then we need to update the location of the piston in the list so that it matches the new wall location.
            when (directionWallIsFacing) {
                "south" -> location.add(0.0, 0.0, 1.0)
                "north" -> location.add(0.0, 0.0, -1.0)
                "west" -> location.add(-1.0, 0.0, 0.0)
                "east" -> location.add(1.0, 0.0, 0.0)
            }

            // Now we physically move the pistons to their new locations.
            location.block.type = Material.PISTON
            // Set the piston block data to face the direction the wall is facing.
            val pistonData = location.block.blockData as org.bukkit.block.data.type.Piston
            pistonData.facing = when (directionWallIsFacing) {
                "south" -> BlockFace.SOUTH
                "north" -> BlockFace.NORTH
                "west" -> BlockFace.WEST
                "east" -> BlockFace.EAST
                else -> throw IllegalArgumentException("Invalid wall direction: $directionWallIsFacing")
            }

            location.block.blockData = pistonData
        }
        //endregion

        lifespan--
    }
}