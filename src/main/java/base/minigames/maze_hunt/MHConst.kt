@file:Suppress("DEPRECATION")

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
import org.bukkit.inventory.ItemStack

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
            Material.COBBLESTONE to 30,
            Material.STONE to 50,
            Material.MOSSY_COBBLESTONE to 15,
            Material.MOSSY_STONE_BRICKS to 10,
            Material.ANDESITE to 20,
            Material.DEEPSLATE to 20,
            Material.COBBLED_DEEPSLATE to 10,
            Material.GOLD_ORE to 1,
            Material.IRON_ORE to 2,
            Material.COAL_ORE to 3,
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
                EntityType.ZOMBIE to 15,
                EntityType.HUSK to 10,
                EntityType.SKELETON to 10,
                EntityType.STRAY to 5,
                EntityType.CREEPER to 5,
                EntityType.SPIDER to 5,
                EntityType.ENDERMAN to 2,
                EntityType.WITCH to 1,
                EntityType.SILVERFISH to 5,
                EntityType.BREEZE to 2,
                EntityType.BLAZE to 2,
                EntityType.SLIME to 10,
                EntityType.MAGMA_CUBE to 2
            )
        }

        object LootCrates {
            enum class LootCrateMaterial(block: Material) {
                MELEE_WEAPON_LOOT_TABLE(Material.RED_WOOL),
                RANGED_WEAPON_LOOT_TABLE(Material.PURPLE_WOOL),
                ARMOR_LOOT_TABLE(Material.GREEN_WOOL),
                FOOD_LOOT_TABLE(Material.YELLOW_WOOL),
            }

            private enum class DurabilityRange(val range: IntRange) {
                LEATHER_ARMOR(5..20),
                IRON_ARMOR(4..8),
                DIAMOND_ARMOR(0..4),
                WOOD_WEAPONS(15..30),
                STONE_WEAPONS(10..20),
                IRON_WEAPONS(5..15),
                DIAMOND_WEAPONS(5..10),
                BOWS(10..25),
                FISHING_ROD(5..10);
                fun durability(): Short = range.random().toShort()
            }

            val MELEE_WEAPON_LOOT_TABLE = listOf(
                ItemStack(Material.WOODEN_SWORD).apply { durability = DurabilityRange.WOOD_WEAPONS.durability() } to 10,
                ItemStack(Material.WOODEN_AXE).apply { durability = DurabilityRange.WOOD_WEAPONS.durability() } to 8,
                ItemStack(Material.STONE_SWORD).apply { durability = DurabilityRange.STONE_WEAPONS.durability() } to 7,
                ItemStack(Material.STONE_AXE).apply { durability = DurabilityRange.STONE_WEAPONS.durability() } to 5,
                ItemStack(Material.IRON_SWORD).apply { durability = DurabilityRange.IRON_WEAPONS.durability() } to 4,
                ItemStack(Material.IRON_AXE).apply { durability = DurabilityRange.IRON_WEAPONS.durability() } to 3,
                ItemStack(Material.DIAMOND_SWORD).apply { durability = DurabilityRange.DIAMOND_WEAPONS.durability() } to 1,
            )

            val RANGED_WEAPON_LOOT_TABLE = listOf(
                ItemStack(Material.BOW).apply { durability = DurabilityRange.BOWS.durability() } to 2,
                ItemStack(Material.CROSSBOW).apply { durability = DurabilityRange.BOWS.durability() } to 2,
                ItemStack(Material.FISHING_ROD).apply { durability = DurabilityRange.FISHING_ROD.durability() } to 1,
                ItemStack(Material.ARROW,4) to 5,
                ItemStack(Material.ARROW,12) to 1,
                ItemStack(Material.TIPPED_ARROW,4) to 2,
                ItemStack(Material.WIND_CHARGE,4) to 2,
                ItemStack(Material.SPLASH_POTION) to 2,
            )

            val ARMOR_LOOT_TABLE = listOf(
                // Leather Armor (Common)
                ItemStack(Material.LEATHER_HELMET).apply { durability = DurabilityRange.LEATHER_ARMOR.durability() } to 10,
                ItemStack(Material.LEATHER_CHESTPLATE).apply { durability = DurabilityRange.LEATHER_ARMOR.durability() } to 10,
                ItemStack(Material.LEATHER_LEGGINGS).apply { durability = DurabilityRange.LEATHER_ARMOR.durability() } to 10,
                ItemStack(Material.LEATHER_BOOTS).apply { durability = DurabilityRange.LEATHER_ARMOR.durability() } to 10,

                // Iron Armor (Uncommon)
                ItemStack(Material.IRON_HELMET).apply { durability = DurabilityRange.IRON_ARMOR.durability() } to 6,
                ItemStack(Material.IRON_CHESTPLATE).apply { durability = DurabilityRange.IRON_ARMOR.durability() } to 6,
                ItemStack(Material.IRON_LEGGINGS).apply { durability = DurabilityRange.IRON_ARMOR.durability() } to 6,
                ItemStack(Material.IRON_BOOTS).apply { durability = DurabilityRange.IRON_ARMOR.durability() } to 6,

                // Diamond Armor (Rare)
                ItemStack(Material.DIAMOND_HELMET).apply { durability = DurabilityRange.DIAMOND_ARMOR.durability() } to 1,
                ItemStack(Material.DIAMOND_CHESTPLATE).apply { durability = DurabilityRange.DIAMOND_ARMOR.durability() } to 1,
                ItemStack(Material.DIAMOND_LEGGINGS).apply { durability = DurabilityRange.DIAMOND_ARMOR.durability() } to 1,
                ItemStack(Material.DIAMOND_BOOTS).apply { durability = DurabilityRange.DIAMOND_ARMOR.durability() } to 1
            )

            val FOOD_LOOT_TABLE = listOf(
                ItemStack(Material.COOKIE,8) to 5,
                ItemStack(Material.CARROT,4) to 5,
                ItemStack(Material.POTATOES,4) to 5,
                ItemStack(Material.APPLE,4) to 5,
                ItemStack(Material.BREAD,3) to 3,
                ItemStack(Material.COOKED_PORKCHOP,2) to 2,
                ItemStack(Material.COOKED_CHICKEN,2) to 2,
                ItemStack(Material.COOKED_BEEF,2) to 2,
                ItemStack(Material.GOLDEN_APPLE,1) to 1
            )
        }
    }

    data class BitPoint(var x: Int, var z: Int)
}
