package base.minigames.maze_hunt

import base.minigames.maze_hunt.MHConst.MazeGen.BIT_RADIUS
import base.minigames.maze_hunt.MHConst.MazeGen.BIT_SIZE
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_X
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_Z
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

object MHConst {


    object Locations {
        val WORLD: World = org.bukkit.Bukkit.getWorld("world") ?: throw IllegalStateException("World 'world' not found")

        /** The pivot point is the point which other locations are relative to. This point is also as the (0;0) for the maze platform.*/
        val PIVOT = Location(WORLD, 0.0, 150.0, 0.0)

        val MAZE_ORIGIN = PIVOT.clone()

        val GAME_START_LOCATION = Location(
            WORLD,
            MAZE_DIMENSION_X/2 * BIT_SIZE.toDouble() + MAZE_ORIGIN.x,
            MAZE_ORIGIN.y + 1,
            MAZE_DIMENSION_Z/2 * BIT_SIZE.toDouble() + MAZE_ORIGIN.z
        )

        val BOTTOM_CORNER: BlockVector3 = BlockVector3.at(
            MAZE_ORIGIN.x - BIT_RADIUS,
            MAZE_ORIGIN.y - 1,
            MAZE_ORIGIN.z - BIT_RADIUS
        )

        val TOP_CORNER: BlockVector3 = BlockVector3.at(
            MAZE_ORIGIN.x + MAZE_DIMENSION_X * BIT_SIZE + BIT_RADIUS,
            MAZE_ORIGIN.y + 1,
            MAZE_ORIGIN.z + MAZE_DIMENSION_Z * BIT_SIZE + BIT_RADIUS
        )

        val MAZE_REGION = CuboidRegion(BOTTOM_CORNER, TOP_CORNER)
    }


    object MazeGen {
        /** Radius of each bit in blocks
         * BIT_SIZE = RADIUS * 2 + 1
         * Each bit is a square of size (BIT_SIZE x BIT_SIZE) blocks
         * from each cardinal direction of the bit can be connected a different bit. */
        const val BIT_RADIUS = 1
        /** Size of each bit in blocks*/
        const val BIT_SIZE = BIT_RADIUS * 2 + 1

        /** How far the maze can stretch in the x coordinate in bits*/
        const val MAZE_DIMENSION_X = 16
        /** How far the maze can stretch in the z coordinate in bits*/
        const val MAZE_DIMENSION_Z = 16

        /** Total number of bits to be generated in the maze */
        const val AMOUNT_OF_BITS: Int = ((MAZE_DIMENSION_X * MAZE_DIMENSION_Z) / 3)

        /** Maximum length of a single chain of bits before forcing to start a new chain*/
        const val MAX_LENGTH_OF_CHAIN = 10

        /** Maximum number of attempts to generate a new bit-snake before stopping the generation process*/
        const val MAX_ATTEMPTS_TO_GENERATE = AMOUNT_OF_BITS

        /** Probability (0.0 to 1.0) of changing the direction that the Chain of Bits goes towards*/
        const val PROBABILITY_OF_CHANGING_DIRECTION = 0.3

        val FLOOR_MATERIAL = Material.OAK_PLANKS
    }

    data class BitPoint(var x: Int, var z: Int)
}
