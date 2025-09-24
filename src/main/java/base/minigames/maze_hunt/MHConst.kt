@file:Suppress("DEPRECATION")

package base.minigames.maze_hunt

import base.minigames.maze_hunt.MHConst.MazeGen.BIT_RADIUS
import base.minigames.maze_hunt.MHConst.MazeGen.BIT_SIZE
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_X
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_Z
import base.utils.extensions_for_classes.duraRange
import base.utils.extensions_for_classes.modifyDuraBy
import base.utils.extensions_for_classes.remainingDurability
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Difficulty
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.EntityType.*
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionType

object MHConst {

    object Locations {
        val WORLD: World = Bukkit.getWorld("world") ?: throw IllegalStateException("World 'world' not found")

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
            MAZE_ORIGIN.y + 2,
            MAZE_ORIGIN.z + MAZE_DIMENSION_Z * BIT_SIZE + BIT_RADIUS
        )

        val MAZE_REGION = CuboidRegion(BOTTOM_CORNER, TOP_CORNER)

        val MIN_LEGAL_Y_LEVEL = MAZE_ORIGIN.clone().y - 15
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

        /**
         * The starting duration of a given maze layout, before switching the maze and cleaning the arena to a different one
         */
        const val REGENERATE_MAZE_INITIAL_COOLDOWN = 20L*30
        const val REGENERATE_MAZE_INITIAL_COOLDOWN_FOR_HARD_MODE = 20L*50

        const val INCREASE_IN_DURATION_FOR_MAZE_GENERATION = 20L*10

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

            const val INITIAL_AMOUNTS_OF_MOBS_TO_SPAWN_IN_A_CYCLE = 2
            const val INITIAL_AMOUNTS_OF_MOBS_TO_SPAWN_IN_A_CYCLE_FOR_HARD_MODE = 10

            const val SPAWN_CYCLE_DELAY = 20L*10

            val NUM_OF_SPAWNS_INCREASER_TIMER_RANGE = 20L*10..20L*20
            const val NUM_OF_SPAWNS_INCREASER_TIMER = 20L*15

            /** List of allowed mob types and their relative weights when spawning*/
            val ALLOWED_MOB_TYPES = listOf(
                ZOMBIE to 15,
                HUSK to 10,
                SKELETON to 10,
                STRAY to 5,
                CREEPER to 5,
                SPIDER to 5,
                ENDERMAN to 2,
                WITCH to 1,
                SILVERFISH to 5,
                BREEZE to 2,
                BLAZE to 2,
                SLIME to 10,
                MAGMA_CUBE to 2
            )
        }

        object LootCrates {
            const val INITIAL_AMOUNTS_OF_CRATES_TO_SPAWN_IN_A_CYCLE = 2
            const val INITIAL_AMOUNTS_OF_CRATES_TO_SPAWN_IN_A_CYCLE_FOR_HARD_MODE = 10

            val NUM_OF_SPAWNS_INCREASER_TIMER_RANGE = 20L*10..20L*20

            /**
             * Defines the different loot crate types.
             * Also assigns a given type to its corresponding information - its display block, its loot table, and how many rolls you get from the type.
             */
            enum class LootCrateType(
                val material: Material,
                val lootTable: List<Pair<ItemStack, Int>>,
                val rolls: IntRange
            ) {
                MELEE_WEAPON_LOOT_TABLE(Material.RED_WOOL,meleeWeaponLootTable,1..2),
                RANGED_WEAPON_LOOT_TABLE(Material.ORANGE_WOOL, rangedWeaponLootTable, 1..3),
                ARMOR_LOOT_TABLE(Material.GREEN_WOOL, armorLootTable, 1..2),
                FOOD_LOOT_TABLE(Material.YELLOW_WOOL, foodLootTable, 1..5),
                POTION_LOOT_TABLE(Material.PURPLE_WOOL,potionLootTable,1..2)
            }

            // Extension function to set durability randomly based on duraRange and the number of copies this item has
            fun Pair<ItemStack,Int>.applyRandomDurability() : ItemStack {
                val duraRange: IntRange = first.duraRange!!

                val copyOfItem = first.clone().apply { remainingDurability = 0 }

                repeat(second) {
                    copyOfItem modifyDuraBy duraRange.random()
                }

                return copyOfItem
            }

            /**
             * Represents different durability ranges for items in the game.
             * Each enum constant defines a specific range of durability values for different item types.
             * The range specifies the dura that is remaining!
             * Used to randomly generate durability values within appropriate ranges for different tiers of items.
             */
            private enum class DurabilityRange(val duraRange: IntRange) {
                LEATHER_ARMOR(10..20),
                IRON_ARMOR(8..12),
                DIAMOND_ARMOR(4..8),
                WOOD_WEAPONS(15..20),
                STONE_WEAPONS(10..15),
                IRON_WEAPONS(8..15),
                DIAMOND_WEAPONS(5..10),
                BOWS(10..18),
                FISHING_ROD(5..10);
            }

            /**
             * Melee weapon loot table containing different tiers of weapons with their weights.
             * Higher weights indicate more common items.
             * Contains:
             * - Wooden weapons (Sword: 10, Axe: 8)
             * - Stone weapons (Sword: 7, Axe: 5)
             * - Iron weapons (Sword: 4, Axe: 3)
             * - Diamond sword (Weight: 1)
             */
            val meleeWeaponLootTable  = listOf(
                ItemStack(Material.WOODEN_SWORD).apply { duraRange = DurabilityRange.WOOD_WEAPONS.duraRange } to 8,
                ItemStack(Material.WOODEN_AXE).apply { duraRange = DurabilityRange.WOOD_WEAPONS.duraRange} to 8,
                ItemStack(Material.STONE_SWORD).apply { duraRange = DurabilityRange.STONE_WEAPONS.duraRange} to 7,
                ItemStack(Material.STONE_AXE).apply { duraRange = DurabilityRange.STONE_WEAPONS.duraRange} to 5,
                ItemStack(Material.IRON_SWORD).apply { duraRange = DurabilityRange.IRON_WEAPONS.duraRange} to 4,
                ItemStack(Material.IRON_AXE).apply { duraRange = DurabilityRange.IRON_WEAPONS.duraRange} to 3,
                ItemStack(Material.DIAMOND_SWORD).apply { duraRange = DurabilityRange.DIAMOND_WEAPONS.duraRange} to 1,
            )

            /**
             * Ranged weapon loot table containing different projectile weapons and ammunition with their weights.
             * Contains:
             * - Bows and Crossbows (Weight: 2 each)
             * - Fishing Rod (Weight: 1)
             * - Arrows (4x: Weight 5, 12x: Weight 1)
             * - Special ammunition (Tipped Arrows, Wind Charges: Weight 2 each)
             * - Splash Potion (Weight: 2)
             */
            val rangedWeaponLootTable = listOf(
                ItemStack(Material.BOW).apply {  duraRange = DurabilityRange.BOWS.duraRange} to 2,
                ItemStack(Material.CROSSBOW).apply {  duraRange = DurabilityRange.BOWS.duraRange} to 2,
                ItemStack(Material.FISHING_ROD).apply {  duraRange = DurabilityRange.FISHING_ROD.duraRange} to 1,
                ItemStack(Material.SNOWBALL,16) to 5,
                ItemStack(Material.ARROW,4) to 5,
                ItemStack(Material.ARROW,12) to 1,
                ItemStack(Material.TIPPED_ARROW,4).apply {
                    val meta = itemMeta as PotionMeta
                    meta.basePotionType = PotionType.SLOWNESS
                    itemMeta = meta
                } to 2,
                ItemStack(Material.WIND_CHARGE,4) to 2,
            )

            private fun ItemStack.setPotionType(type: PotionType) {
                val meta = (itemMeta as PotionMeta)
                meta.basePotionType = type
                itemMeta = meta
            }

            /**
             * Potion loot table containing different types of splash potions with their weights.
             * Higher weights indicate more common items.
             * Contains:
             * - Combat potions (Harming, Healing: Weight 10 each)
             * - Utility potions (Swiftness, Poison: Weight 7 each)
             * - Support potions (Regeneration: Weight 6)
             * - Special effects (Leaping, Long Slow Falling: Weight 4 each)
             * - Debuff potions (Strong Slowness: Weight 3)
             */
            val potionLootTable = listOf(
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.HARMING) } to 10,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.HEALING) } to 10,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.SWIFTNESS) } to 7,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.POISON) } to 7,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.REGENERATION) } to 6,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.LEAPING) } to 4,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.LONG_SLOW_FALLING) } to 4,
                ItemStack(Material.SPLASH_POTION).apply { setPotionType(PotionType.STRONG_SLOWNESS) } to 3,
            )


            /**
             * Armor loot table containing different tiers of armor pieces with their weights.
             * Contains:
             * - Leather armor set (All pieces: Weight 10)
             * - Iron armor set (All pieces: Weight 6)
             * - Diamond armor set (All pieces: Weight 1)
             * Each piece includes a helmet, chestplate, leggings, and boots
             */
            val armorLootTable = listOf(
                // Leather Armor (Common)
                ItemStack(Material.LEATHER_HELMET).apply { duraRange = DurabilityRange.LEATHER_ARMOR.duraRange} to 10,
                ItemStack(Material.LEATHER_CHESTPLATE).apply { duraRange = DurabilityRange.LEATHER_ARMOR.duraRange} to 10,
                ItemStack(Material.LEATHER_LEGGINGS).apply { duraRange = DurabilityRange.LEATHER_ARMOR.duraRange} to 10,
                ItemStack(Material.LEATHER_BOOTS).apply { duraRange = DurabilityRange.LEATHER_ARMOR.duraRange} to 10,

                // Iron Armor (Uncommon)
                ItemStack(Material.IRON_HELMET).apply { duraRange = DurabilityRange.IRON_ARMOR.duraRange} to 6,
                ItemStack(Material.IRON_CHESTPLATE).apply { duraRange = DurabilityRange.IRON_ARMOR.duraRange} to 6,
                ItemStack(Material.IRON_LEGGINGS).apply { duraRange = DurabilityRange.IRON_ARMOR.duraRange} to 6,
                ItemStack(Material.IRON_BOOTS).apply { duraRange = DurabilityRange.IRON_ARMOR.duraRange} to 6,

                // Diamond Armor (Rare)
                ItemStack(Material.DIAMOND_HELMET).apply { duraRange = DurabilityRange.DIAMOND_ARMOR.duraRange} to 1,
                ItemStack(Material.DIAMOND_CHESTPLATE).apply { duraRange = DurabilityRange.DIAMOND_ARMOR.duraRange} to 1,
                ItemStack(Material.DIAMOND_LEGGINGS).apply { duraRange = DurabilityRange.DIAMOND_ARMOR.duraRange} to 1,
                ItemStack(Material.DIAMOND_BOOTS).apply { duraRange = DurabilityRange.DIAMOND_ARMOR.duraRange} to 1
            )

            /**
             * Food loot table containing different types of food items with their weights.
             * Contains:
             * - Basic foods (Cookie x8, Carrot x4, Potato x4, Apple x4: Weight 5)
             * - Medium tier foods (Bread x3: Weight 3)
             * - Cooked meats (Porkchop x2, Chicken x2, Beef x2: Weight 2)
             * - Special food (Golden Apple x1: Weight 1)
             */
            val foodLootTable = listOf(
                ItemStack(Material.COOKIE,8) to 5,
                ItemStack(Material.CARROT,4) to 5,
                ItemStack(Material.POTATO,4) to 5,
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


