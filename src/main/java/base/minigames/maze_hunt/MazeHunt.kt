package base.minigames.maze_hunt

import base.annotations.CalledByCommand
import base.annotations.ShouldBeReset
import base.minigames.MinigameSkeleton
import base.minigames.maze_hunt.MHConst.Locations.MAZE_ORIGIN
import base.minigames.maze_hunt.MHConst.Locations.WORLD
import org.bukkit.Location
import org.bukkit.entity.Player
import base.minigames.maze_hunt.MHConst.Locations
import base.minigames.maze_hunt.MHConst.MazeGen
import base.minigames.maze_hunt.MHConst.Spawns.Mobs
import base.minigames.maze_hunt.MHConst.MazeGen.BIT_SIZE
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_X
import base.minigames.maze_hunt.MHConst.MazeGen.MAZE_DIMENSION_Z
import base.utils.Direction
import base.utils.extensions_for_classes.getBlockAt
import org.bukkit.Material
import org.jetbrains.kotlinx.multik.api.d2array
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.ndarray.data.D2Array
import org.jetbrains.kotlinx.multik.ndarray.data.get
import org.jetbrains.kotlinx.multik.ndarray.data.set
import base.minigames.maze_hunt.MHConst.BitPoint
import base.utils.Utils
import base.utils.extensions_for_classes.getWeightedRandom
import base.utils.Utils.initFloor
import base.utils.Utils.successChance
import com.destroystokyo.paper.event.block.BlockDestroyEvent
import org.bukkit.Difficulty
import org.bukkit.GameRule
import org.bukkit.GameRule.DO_DAYLIGHT_CYCLE
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class MazeHunt(val plugin: Plugin) : MinigameSkeleton() , Listener {
    override val minigameName: String = this::class.simpleName ?: "Unknown"


    /** this set keeps track of all the indices of the bits that have been generated */
    @ShouldBeReset
    val generatedBitsIndexes: MutableSet<BitPoint> = mutableSetOf()

    /** Number of mobs to spawn every mob spawning cycle. Gets increased as time goes on. */
    @ShouldBeReset
    var amountOfMobsToSpawnPerInterval: Int = Mobs.INITIAL_AMOUNTS_OF_MOBS_TO_SPAWN_IN_A_CYCLE

    /** Number of Loot Crates to spawn every crate spawning cycle. Gets increased as time goes on. */
    @ShouldBeReset
    var amountOfCratesToSpawnPerInterval: Int = MHConst.Spawns.LootCrates.INITIAL_AMOUNTS_OF_CRATES_TO_SPAWN_IN_A_CYCLE

    @CalledByCommand
    override fun start(sender: Player) {
        try {
            super.startSkeleton(sender)

            pausableRunnables += Utils.PausableBukkitRunnable(plugin as JavaPlugin, remainingTicks = MHConst.STARTING_PLATFORM_LIFESPAN) {
                startGameLoop()
            }.apply { this.start() }

        } catch (e: InterruptedException) {
            pauseGame()
            throw e
        }
    }

    private fun startGameLoop() {
        //region Start spawning mobs
        pausableRunnables += Utils.PausableBukkitRunnable(plugin as JavaPlugin, periodTicks = Mobs.SPAWN_CYCLE_DELAY) {
            repeat(amountOfMobsToSpawnPerInterval) {
                if (generatedBitsIndexes.isEmpty())
                    return@repeat

                val chosenMobToSpawn = Mobs.ALLOWED_MOB_TYPES.getWeightedRandom()

                val chosenLocationToSpawnAt: Location = generatedBitsIndexes.random().let {
                    getBitLocation(it.x, it.z)
                }.apply { y += 1 }

                WORLD.spawnEntity(chosenLocationToSpawnAt, chosenMobToSpawn)
            }

            for (player in players) { player.sendMessage("New mob wave!") }

        }.apply { this.start() }

        // Gradually increase the number of mobs spawned per interval
        pausableRunnables += Utils.PausableBukkitRunnable(plugin, periodTicks = Mobs.NUM_OF_SPAWNS_INCREASER_TIMER_RANGE.random()) {
            amountOfMobsToSpawnPerInterval += 1
        }.apply { this.start() }

        //endregion

        //region Start Spawning Loot Crates
        pausableRunnables += Utils.PausableBukkitRunnable(plugin, periodTicks = Mobs.SPAWN_CYCLE_DELAY) {
            repeat(amountOfCratesToSpawnPerInterval) {
                if (generatedBitsIndexes.isEmpty())
                    return@repeat

                val chosenCrateType = MHConst.Spawns.LootCrates.LootCrateType.entries.random()

                val chosenLocationToSpawnAt: Location = generatedBitsIndexes.random().let {
                    getBitLocation(it.x, it.z)
                }.apply {
                    x += (-MazeGen.BIT_RADIUS..MazeGen.BIT_RADIUS).random()
                    y += 2
                    z += (-MazeGen.BIT_RADIUS..MazeGen.BIT_RADIUS).random()
                }

                var blockAt = WORLD.getBlockAt(chosenLocationToSpawnAt)

                blockAt.type = chosenCrateType.material

                // We store metadata of the crate type so that only blocks with this metadata will drop loot when broken, and not just every block that has been broken.
                // The onLootCrateBreak will listen to the block break and check the metadata of the block.
                blockAt.setMetadata("isALootCrate", FixedMetadataValue(plugin, chosenCrateType.name))
            }
        }.apply { this.start() }

        // Gradually increase the number of crates spawned per interval
        pausableRunnables += Utils.PausableBukkitRunnable(plugin, periodTicks = MHConst.Spawns.LootCrates.NUM_OF_SPAWNS_INCREASER_TIMER_RANGE.random()) {
            amountOfCratesToSpawnPerInterval += 1
        }.apply { this.start() }
        //endregion
    }

    @EventHandler
    private fun onLootCrateBreak(event: BlockBreakEvent) {
        if (event.block.hasMetadata("isALootCrate").not())
            return

        val metaList = event.block.getMetadata("isALootCrate")

        val typeName = metaList.first { it.owningPlugin == plugin }.asString()

        // convert string back to enum
        val crateType = MHConst.Spawns.LootCrates.LootCrateType.valueOf(typeName)

        // Now you have the enum instance and can access its pool.

        // Poll n number of rolls from the pool
        val itemsInside: List<ItemStack> = List(crateType.rolls.random()) { crateType.lootTable.getWeightedRandom() }
        itemsInside.forEach { event.player.inventory.addItem(it) }

        // We'll delete the metadata to not accidentally think later on that there's still loot to obtain from a place where there isn't a loot crate
        event.block.removeMetadata("isALootCrate",plugin)

        // disable the block drop so that the physical block won't be used/exploited
        event.isDropItems = false
    }

    @CalledByCommand
    override fun endGame() {
        endGameSkeleton()

        nukeArea()
        deleteStartingPlatform()// delete the starting platform for cases where it is still there

        WORLD.difficulty = Difficulty.PEACEFUL
        WORLD.setGameRule(GameRule.DO_FIRE_TICK,false)

        //RESET GLOBAL VARIABLES
        generatedBitsIndexes.clear()
        amountOfMobsToSpawnPerInterval = Mobs.INITIAL_AMOUNTS_OF_MOBS_TO_SPAWN_IN_A_CYCLE
        amountOfCratesToSpawnPerInterval= MHConst.Spawns.LootCrates.INITIAL_AMOUNTS_OF_CRATES_TO_SPAWN_IN_A_CYCLE

        players.forEach { player ->
            player.inventory.clear()
        }
    }

    private fun deleteStartingPlatform() {
        initFloor(
            MHConst.STARTING_PLATFORM_RADIUS,
            MHConst.STARTING_PLATFORM_RADIUS,
            Material.AIR,
            Locations.START_LOCATION_PLATFORM,
            WORLD
        )
    }

    @CalledByCommand
    fun nukeArea() {
        // Nuke the game area
        for (vector in Locations.MAZE_REGION) {
            var blockAt = WORLD.getBlockAt(vector)
            blockAt.type = Material.AIR

            if (blockAt.hasMetadata("isALootCrate"))
                blockAt.removeMetadata("isALootCrate",plugin)
        }
    }

    override fun prepareGameSetting() {
        super.prepareGameSetting()

        WORLD.time = 1000
        WORLD.setGameRule(DO_DAYLIGHT_CYCLE, false)
        WORLD.setGameRule(GameRule.DO_FIRE_TICK,false)
        WORLD.difficulty = Mobs.WORLD_DIFFICULTY

        for (player in players) {
            player.teleport(Locations.PLAYERS_START_LOCATION)
            player.gameMode = org.bukkit.GameMode.SURVIVAL
        }
    }

    /** Disable mobs getting burned by the sun while Maze Hunt is running*/
    @EventHandler
    fun onEntityCombust(event: EntityCombustEvent) {
        if (!isGameRunning) return

        event.isCancelled = true
    }


    val _TRUE = 1.toByte()
    val _FALSE = 0.toByte()
    override fun prepareArea() {
        nukeArea()

        // Create the starting platform for the players to stand on. It'll be deleted momentarily.
        initFloor(
            MHConst.STARTING_PLATFORM_RADIUS,
            MHConst.STARTING_PLATFORM_RADIUS,
            Material.GLASS,
            Locations.START_LOCATION_PLATFORM,
            WORLD
        )


        pausableRunnables += Utils.PausableBukkitRunnable(plugin as JavaPlugin, MHConst.STARTING_PLATFORM_LIFESPAN) {
            deleteStartingPlatform()
        }.apply { this.start() }


        //region Code that creates the maze area

        // generate the corners of the maze area
        Locations.MAZE_REGION.let {
            WORLD.getBlockAt(it.minimumPoint).type = Material.REDSTONE_BLOCK
            WORLD.getBlockAt(it.maximumPoint).type = Material.REDSTONE_BLOCK
        }


        /** 2D array to keep track of generated bits*/
        val mazeMatrix: D2Array<Byte> = mk.d2array(MAZE_DIMENSION_X, MAZE_DIMENSION_Z, { _FALSE })


        // Start generating from the center of the maze
        mazeMatrix[MAZE_DIMENSION_X / 2, MAZE_DIMENSION_Z / 2] = _TRUE
        physicallyCreateBit(MAZE_DIMENSION_X/2,MAZE_DIMENSION_Z/2, MazeGen.BIT_RADIUS)

        // -1 because we already placed the first bit in the center
        var numberOfBitsLeftToGenerate = MazeGen.AMOUNT_OF_BITS - 1
        // add the center bit to the set of generated bits
        generatedBitsIndexes += BitPoint(MAZE_DIMENSION_X / 2, MAZE_DIMENSION_Z / 2)


        // Limit the number of bit-snakes to prevent infinite loops
        var maxAmountOfTries = MazeGen.MAX_ATTEMPTS_TO_GENERATE

        while (numberOfBitsLeftToGenerate > 0 && maxAmountOfTries > 0) {
            maxAmountOfTries -= 1

            // Select a random spot already generated from the bits that have been generated. It stores the coordinates of the bit. Start a new chain of bits from there
            val newBitCreatorStartingSpot: BitPoint = generatedBitsIndexes.random()

            numberOfBitsLeftToGenerate = startNewChainOfBits(newBitCreatorStartingSpot, mazeMatrix,generatedBitsIndexes, numberOfBitsLeftToGenerate)
        }

        //endregion
    }

    /**
     * Start a new chain of bits from the given starting spot in the maze matrix.
     * A chain of bits is a series of bits that are connected to each other in a mostly straight line, with a small chance to change the direction at each step.
     * The chain of bits will continue until it either runs out of bits to generate or it reaches a point where it can no longer safely generate a new bit in the current direction.
     * @param newBitCreatorStartingSpot The starting spot for the new chain of bits in the maze matrix.
     * @param mazeMatrix The maze matrix.
     * @param bitsLeft The number of bits left to generate in the maze.
     * @return The updated number of bits left to generate after generating the new chain of bits.
     */
    private fun startNewChainOfBits(
        newBitCreatorStartingSpot: BitPoint,
        mazeMatrix: D2Array<Byte>,
        generatedBitsIndexes: MutableSet<BitPoint>,
        bitsLeft: Int
    ) : Int {
        var returnedNumOfBitsLeft = bitsLeft

        // in the direction the new chain of bits will go towards mostly. we need to make sure that it is safe to travel in that direction, so we filter the directions first
        var potentialDirections = Direction.entries.filter { dir ->
            isSafeToTravelForwards(dir, newBitCreatorStartingSpot, mazeMatrix)
        }
        // If there is no safe direction to travel, return early
        if (potentialDirections.isEmpty()) return returnedNumOfBitsLeft


        var curDirectionOfChain = potentialDirections.random()

        var curLengthOfChain = 0

        //region Code for deciding the new Bit that will be generated and generating it
        while (
            returnedNumOfBitsLeft > 0 &&
            curLengthOfChain < MazeGen.MAX_LENGTH_OF_CHAIN &&
            isSafeToTravelForwards(curDirectionOfChain, newBitCreatorStartingSpot, mazeMatrix)
        ) {
            // calculate the position of the new bit to be generated
            when (curDirectionOfChain) {
                Direction.NORTH -> {newBitCreatorStartingSpot.z -= 1 }
                Direction.SOUTH -> { newBitCreatorStartingSpot.z += 1 }
                Direction.EAST -> { newBitCreatorStartingSpot.x += 1 }
                Direction.WEST -> { newBitCreatorStartingSpot.x -= 1 }
            }

            physicallyCreateBit(newBitCreatorStartingSpot.x,newBitCreatorStartingSpot.z, MazeGen.BIT_RADIUS)

            mazeMatrix[newBitCreatorStartingSpot.x, newBitCreatorStartingSpot.z] = _TRUE
            generatedBitsIndexes += newBitCreatorStartingSpot.copy()

            returnedNumOfBitsLeft -= 1
            curLengthOfChain += 1

            // have a small chance to change the predominant direction
            if (successChance(MazeGen.PROBABILITY_OF_CHANGING_DIRECTION)) {
                curDirectionOfChain = listOf(
                    curDirectionOfChain.getClockwise(),
                    curDirectionOfChain.getCounterClockwise()
                ).random()
            }
        }
        //endregion

        return returnedNumOfBitsLeft
    }

    /**
     * Check if it is safe to travel forwards in the current direction of the chain of bits.
     * It is safe to travel forwards if there is enough space in the maze matrix to generate a new bit in that direction, without touching any surrounding existing bit.
     * @param direction The current direction of the chain of bits.
     * @param point The current point in the maze matrix.
     * @param mazeMatrix The maze matrix.
     * @return True if it is safe to travel forwards, false otherwise.
     */
    private fun isSafeToTravelForwards(
        direction: Direction,
        point: BitPoint,
        mazeMatrix: D2Array<Byte>
    ) : Boolean {
        fun isAreaClear(point: BitPoint, xRange: IntRange, zRange: IntRange): Boolean {
            return zRange.all { zOffset ->
                xRange.all { xOffset ->
                    mazeMatrix[point.x + xOffset,point.z + zOffset] == _FALSE
                }
            }
        }

        // the tiles to check in the direction of travel.
        val directionTilesToCheck = -1..1

        // how far to check in the direction of travel
        val longReach = 2
        var smallReach = 1
        //

        return when (direction) {
            Direction.NORTH -> if (point.z >= longReach) {
                isAreaClear(point, directionTilesToCheck, -minOf(point.z, longReach)..-smallReach)
            } else false
            Direction.SOUTH -> if (point.z <= MAZE_DIMENSION_Z-1 - longReach) {
                isAreaClear(point, directionTilesToCheck, smallReach..minOf(point.z, longReach))
            } else false
            Direction.EAST -> if (point.x <= MAZE_DIMENSION_Z-1 - longReach) {
                isAreaClear(point, smallReach..minOf(point.x, longReach), directionTilesToCheck)
            } else false
            Direction.WEST -> if (point.x >= longReach) {
                isAreaClear(point,-minOf(point.x, longReach)..-smallReach, directionTilesToCheck)
            } else false
        }
    }

    /**
     * Physically create a maze Bit in the game world - this is a square-like platform defined by the size.
     * @param bitIndexX The x index of the bit in the maze matrix.
     * @param bitIndexZ The z index of the bit in the maze matrix.
     * @param radius The radius of the bit in blocks. The size of the bit will be (radius * 2 + 1) x (radius * 2 + 1)
     * Made from a predefined block type
     */
    fun physicallyCreateBit(bitIndexX: Int, bitIndexZ: Int, radius: Int) {
        val center = getBitLocation(bitIndexX, bitIndexZ)

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val selectedLocation = Location(WORLD, center.x + x, center.y, center.z + z)
                selectedLocation.block.type = MazeGen.FLOOR_MATERIALS.getWeightedRandom()
            }
        }
    }

    private fun getBitLocation(bitIndexX: Int, bitIndexZ: Int): Location {
        val center = Location(
            WORLD,
            bitIndexX.toDouble() * BIT_SIZE + MAZE_ORIGIN.x,
            MAZE_ORIGIN.y,
            bitIndexZ.toDouble() * BIT_SIZE + MAZE_ORIGIN.z
        )
        return center
    }
}

