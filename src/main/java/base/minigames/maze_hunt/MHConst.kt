package base.minigames.maze_hunt

import base.minigames.maze_hunt.MHConst.MazeGen.BIT_RADIUS
import base.minigames.maze_hunt.MHConst.MazeGen.BIT_SIZE
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_X
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_Z
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Difficulty
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType

object MHConst {

    object Locations {
        val WORLD: World = org.bukkit.Bukkit.getWorld("world") ?: throw IllegalStateException("World 'world' not found")

        /** The pivot point is the point which other locations are relative to. This point is also as the (0;0) for the maze platform.*/
        val PIVOT = Location(WORLD, 0.0, 150.0, 0.0)

        val MAZE_ORIGIN = PIVOT.clone()

        val PLAYERS_START_LOCATION = Location(
            WORLD,
            MAZE_DIMENSION_X/2 * BIT_SIZE.toDouble() + MAZE_ORIGIN.x,
            MAZE_ORIGIN.y + 10,
            MAZE_DIMENSION_Z/2 * BIT_SIZE.toDouble() + MAZE_ORIGIN.z
        )
        val START_LOCATION_PLATFORM = PLAYERS_START_LOCATION.clone().apply { y -= 3 }

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

    const val STARTING_PLATFORM_RADIUS = 5
    const val STARTING_PLATFORM_LIFESPAN = 20L*5

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

        /** Materials and their relative weights to be used when generating the floor of the maze*/
        val FLOOR_MATERIALS = listOf(
            Pair(Material.COBBLESTONE,30),
            Pair(Material.STONE,50),
            Pair(Material.MOSSY_COBBLESTONE,15),
            Pair(Material.MOSSY_STONE_BRICKS,10),
            Pair(Material.ANDESITE,20),
            Pair(Material.DEEPSLATE,20),
            Pair(Material.COBBLED_DEEPSLATE,10),
            Pair(Material.GOLD_ORE,1),
            Pair(Material.IRON_ORE,2),
            Pair(Material.COAL_ORE,3),
        )
    }

    object Spawns {
        object Mobs {
            val WORLD_DIFFICULTY = Difficulty.NORMAL

            const val INITIAL_AMOUNTS_OF_MOBS_TO_SPAWN_IN_A_CYCLE = 1

            const val SPAWN_CYCLE_DELAY = 20L*10

            val NUM_OF_SPAWNS_INCREASER_TIMER_RANGE = 20L*10..20L*20


            /** List of allowed mob types and their relative weights when spawning*/
            val ALLOWED_MOB_TYPES = listOf(
                Pair(EntityType.ZOMBIE,15),
                Pair(EntityType.HUSK,10),
                Pair(EntityType.SKELETON,10),
                Pair(EntityType.STRAY,5),
                Pair(EntityType.CREEPER,5),
                Pair(EntityType.SPIDER,5),
                Pair(EntityType.ENDERMAN,2),
                Pair(EntityType.WITCH,1),
                Pair(EntityType.SILVERFISH,5),
                Pair(EntityType.BREEZE,2),
                Pair(EntityType.BLAZE,2),
                Pair(EntityType.SLIME,10),
                Pair(EntityType.MAGMA_CUBE,2)
            )
        }
    }

    data class BitPoint(var x: Int, var z: Int)
}
