package me.stavgordeev.plugin.Minigames.HoleInTheWall

import me.stavgordeev.plugin.BuildLoader
import me.stavgordeev.plugin.Direction
import me.stavgordeev.plugin.MinigamePlugin
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HITWConst.Timers
import me.stavgordeev.plugin.Minigames.MinigameSkeleton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.io.IOException
import java.util.*
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HITWConst.WallSpawnerState
import me.stavgordeev.plugin.Utils.activateTaskAfterConditionIsMet
import net.kyori.adventure.text.format.NamedTextColor
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HITWConst.WallSpawnerMode
import org.bukkit.scheduler.BukkitRunnable

class HoleInTheWall (plugin: Plugin?) : MinigameSkeleton(plugin) {
    //region vars

    private lateinit var selectedMapBaseFile: File
    private lateinit var platformSchematics: Array<File> //the platform stages for a given map
    private lateinit var wallPackSchematics: Array<File> //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.
    private lateinit var mapSchematic: File //the map schematic that is being played.
    private lateinit var mapName: String //the map name that is being played. gets a value on the start() method.


    //the periodic task that runs every second to update the game state
    private lateinit var gameEvents: BukkitTask




    //region ----Game Modifiers that change as the game progresses
    private var timeLeft: Double = Timers.GAME_DURATION.toDouble()
    private var timeElapsed: Double = 0.0 //in seconds
    private var wallSpeed = Timers.WALL_SPEED[0] //in ticks
    private val wallSpeedUpLandmarks: IntArray = Timers.WALL_SPEED_UP_LANDMARKS //in seconds
    private var wallSpeedIndex = 0 //index of the wall speed in the array

    //the current wall difficulty in the pack. starts from EASY and increases as the game progresses.
    // note that previous wall difficulties are also used in the game, but less frequently.
    private var curWallDifficultyInPack = HITWConst.WallDifficulty.EASY
    private val increaseWallDifficultyLandmarks: IntArray = Timers.INCREASE_WALL_DIFFICULTY_LANDMARKS //in seconds


    // A list of walls that are currently alive in the game. This is used to keep track of walls that are currently in play.
    // This list is updated as walls are spawned and deleted, and is tackled in the periodic() method.
    val existingWallsList: MutableList<Wall> = mutableListOf()

    private val wallsToDelete: MutableList<Wall> = mutableListOf() // A list of walls that are to be deleted. This is used to delete walls that are no longer alive

    private var stateOfWallSpawner: WallSpawnerState = WallSpawnerState.IDLE // The state of the wall spawner. This is used to determine what action is being done at any given moment and to ensure that nothing unexpected or unwanted occurs with behaviors to walls.

    // The current mode of spawning walls logic. A mode dictates what possible WallSpawnerStates can be done in the state machine at a given moment.
    // The moment swaps naturally every so often to increase replayability.
    private lateinit var wallSpawningMode: WallSpawnerMode

    //endregion -----------------------------------------------------------------------------------
    //endregion

    @Throws(InterruptedException::class)
    fun start(player: Player, mapName: String,wallSpawningMode: String) {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"))
            return
        }
        changeWallSpawningMode(wallSpawningMode)

        this.mapName = mapName
        start(player)

        startRepeatingGameLoop()


    }

    fun changeWallSpawningMode(mode: String) {
        // This func can be called whether the game is alive or not. (for the command that uses it

        fun changeMode(mode: WallSpawnerMode) {
            wallSpawningMode = mode
            Bukkit.getServer().broadcast(Component.text("wallSpawnerMode = $wallSpawningMode").color(
            NamedTextColor.DARK_AQUA))


        }

        WallSpawnerMode.entries.forEach {
            if (mode.uppercase() == it.name) {
                changeMode(it)
                return
            }
        }

        if (mode == "Alternating") {
            object : BukkitRunnable() {
                override fun run() {
                    if (isGamePaused || !isGameRunning) {
                        cancel()
                        return
                    }

                    changeMode(WallSpawnerMode.entries.random())
                }
            }.runTaskTimer(plugin,0L,Timers.ALTERNATING_WALL_SPAWNER_MODES_DELAY)

            return
        }

        // if we got here, it means that the sender hasn't sent a proper mode to play
        Bukkit.getServer().broadcast(Component.text("the wallSpawnerMode provided is not valid. not starting the game").color(NamedTextColor.DARK_AQUA))
        throw IllegalArgumentException("HITW: mode provided to play as is illegal")
    }

    override fun endGame(player: Player?) {
        if (!isGameRunning) {
            player!!.sendMessage("Minigame is not running!")
            return
        }
        super.endGame(player)
        // Cancel the periodic task that updates the game state and handles all game events - such as wall movement, wall spawning, and wall deletion.
        gameEvents.cancel()

        // Clear the list of alive walls
        existingWallsList.clear()

        // Clear the directions that were calulated for walls
        directionsOfUpcomingWalls.clear()
        directionOfUpcomingWall = Direction.NORTH

        stateOfWallSpawner = WallSpawnerState.IDLE // Reset the state of the wall spawner to IDLE

        this.nukeArea(HITWConst.Locations.PIVOT, 60) // Clear the area around the spawn point
    }

    private fun startRepeatingGameLoop() {
        fun handlePsychWallsThatRanOutOfLifespan(wall: Wall) {
            // If the wall is a psych wall, we will keep it existing for a lil, then later decide if it should be removed or not.
            Bukkit.getScheduler().runTaskLater(MinigamePlugin.plugin, Runnable {
                // Randomly decide if the wall should be removed or not.
                // 66% - to get removed, 34% - to stay.
                val chosenToBeRemoved = (0..100).random() <= 66

                Bukkit.getServer().broadcast(Component.text("chosenToBeRemoved = $chosenToBeRemoved").color(
            NamedTextColor.DARK_AQUA))

                // If the wall is chosen to be removed, we'll remove it, otherwise, we will resume its movement after a delay.
                if (chosenToBeRemoved) {
                    wall.shouldBeRemoved = true
                } else {
                    activateTaskAfterConditionIsMet(1L,{getAliveMovingWalls().isEmpty()} , {
                        wall.shouldBeStopped = false
                        wall.lifespanRemaining = HITWConst.PSYCH_WALL_THAT_RETURNS_TO_MOVING_LIFESPAN // Reset the lifespan of the wall to a lifespan that is enough for it to reach the same distance as a regular wall.

                        // get rid of the identity of the wall - since psych walls should only stop themselves once, and we don't want for them to stop later on when the lifespan is 0 again
                        wall.isPsych = false

                        wall.isBeingHandled = false
                    })
                }

            }, Timers.STOPPED_WALL_DELAY_BEFORE_ACTION_DEALT.random())
        }


        if (!this.isGameRunning || isGamePaused) {
            logger().warn("HITW: Game is not running, cannot start periodic task")
            return
        }

        var tickCount: Int = 0 // Used to keep track of the number of ticks that have passed since the game started





        //Update every second the time left and the time elapsed, and keep track if certain events should trigger based on the time that has elapsed.
        gameEvents = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {

        tickCount++
        timeLeft-= 1/20
        timeElapsed+= 1/20
        if (timeLeft <= 0) {
            endGame(thePlayer)
        }

        //region ---Check if the wall speed should be increased
        if (wallSpeedIndex < wallSpeedUpLandmarks.size && timeElapsed >= wallSpeedUpLandmarks[wallSpeedIndex]) {
            wallSpeed = Timers.WALL_SPEED[++wallSpeedIndex]
        }
        //endregion

        //region ---Check if the wall difficulty should be increased
        //TODO: implement logic
        if (curWallDifficultyInPack != HITWConst.WallDifficulty.VERY_HARD && timeElapsed >= increaseWallDifficultyLandmarks[curWallDifficultyInPack]) {
            when (++curWallDifficultyInPack) {
                HITWConst.WallDifficulty.MEDIUM -> {}
                HITWConst.WallDifficulty.HARD -> {}
                HITWConst.WallDifficulty.VERY_HARD -> {}
            }
        }
        //endregion


        //region --Check if the walls should be moved, and handle if they should be stopped/deleted/resumed

        //If the time elapsed is a multiple of the wall speed (which resembles how often the walls should be moved at in ticks), then move the walls
        if (tickCount % wallSpeed == 0) {
            // Get the walls that have a lifespan that's greater than 0
            for (wall in getAliveMovingWalls()) {
                // Move the wall if its lifespan is greater than 0 and it should not be stopped
                wall.move()
            }
            for (wall in getWallsThatAreStopped()) {
                if (!wall.isPsych) {
                    //TODO: currently, regular walls are removed immediately, but we can make it so that they can be stopped instead of removed for various reasons
                    wall.shouldBeRemoved = true // If the wall is not a psych wall, we will remove it immediately.
                } else {
                    if (!wall.isBeingHandled) {
                        wall.isBeingHandled = true
                        handlePsychWallsThatRanOutOfLifespan(wall)
                    }
                }
                // If the wall is no longer alive, delete it via adding it to a new list of walls to delete
                if (wall.shouldBeRemoved) wallsToDelete.add(wall)
            }

            // Delete the walls that are no longer alive
            wallsToDelete.forEach { deleteWall(it) }
             // Clear the list of walls to delete after deleting them so that we don't delete the same walls again
            wallsToDelete.clear()
        }
        //endregion

        //region --Add new walls to the game

        // Limit the number of walls to HARD_CAP_MAX_POSSIBLE_AMOUNT_OF_WALLS at a time
        if (existingWallsList.size < HITWConst.HARD_CAP_MAX_POSSIBLE_AMOUNT_OF_WALLS) {
            // We'll make a state machine. depending on the state of the game, we'll decide to spawn new walls with different behavior and traits.
            manageWallSpawning()
        }
        //endregion

        }, Timers.DELAY_BEFORE_STARTING_GAME,1L)



    }

    lateinit var directionOfUpcomingWall: Direction
    val directionsOfUpcomingWalls: MutableList<Direction> = mutableListOf()

    private fun manageWallSpawning() {
        //TODO: the logic currently is very dull and incomplete
        fun isSafeToSpawnWall() : Boolean {

            val directionsExistingWallsHave: Set<Direction> = existingWallsList.map { it.directionWallComesFrom }.toSet()
            val numOfDirectionsExistingWallsHave = directionsExistingWallsHave.size

            return when (numOfDirectionsExistingWallsHave) {
                0 -> true
                1 -> {
                    val lastWall: Wall = existingWallsList.last()

                    when {
                        directionOfUpcomingWall in directionsExistingWallsHave ->
                            lastWall.lifespanTraveled >= lastWall.minimumLifespanTraveledWhereWallsCanSpawnBehindIt

                        directionOfUpcomingWall.getClockwise() in directionsExistingWallsHave ||
                        directionOfUpcomingWall.getCounterClockwise() in directionsExistingWallsHave ->
                            lastWall.lifespanTraveled >= HITWConst.DEFAULT_WALL_TRAVEL_LIFESPAN - 7

                        directionOfUpcomingWall.getOpposite() in directionsExistingWallsHave ->
                            lastWall.lifespanTraveled >= HITWConst.DEFAULT_WALL_TRAVEL_LIFESPAN - 4

                        else -> false
                    }
                }
                2,3,4 -> false
                else -> {throw Exception("numOfDirectionsExistingWallHave must be between 0 and 4") }
            }
        }

        fun attemptChangingStateTo(wantedState: WallSpawnerState) {
            val canTransition = when (stateOfWallSpawner) {
                WallSpawnerState.IDLE -> wantedState in setOf(
                            WallSpawnerState.INTENDING_TO_CREATE_1_WALL,
                            WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE,
                            WallSpawnerState.SPAWNING
                )

                WallSpawnerState.INTENDING_TO_CREATE_1_WALL -> wantedState in setOf(
                            WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN
                )

                WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE -> wantedState in setOf(
                            WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN
                )


                WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN -> wantedState in setOf(
                            WallSpawnerState.SPAWNING,
                            WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE
                )

                WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE -> wantedState in setOf(
                            WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS
                )

                WallSpawnerState.SPAWNING -> wantedState in setOf(
                            WallSpawnerState.IDLE
                )

                WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS -> wantedState in setOf(
                            WallSpawnerState.IDLE
                )

                WallSpawnerState.DO_NO_ACTION -> false
            }

            if (!canTransition) throw IllegalArgumentException("The wanted wall spawner state to transition was ${wantedState}. The current state however is is $stateOfWallSpawner")

            stateOfWallSpawner = wantedState

            Bukkit.getServer().broadcast(Component.text("Wall spawner state changed to: $stateOfWallSpawner").color(NamedTextColor.GRAY))
        }

        // The State Evaluator.
        when (stateOfWallSpawner) {
            WallSpawnerState.IDLE -> { //region IDLE
                val wantedState = when (wallSpawningMode) {
                    WallSpawnerMode.WALL_CHAINER -> {
                        // if we don't have any walls in the arena, we can add one immediately, otherwise we'll decide where and when to add it via the bridger states
                        if (existingWallsList.isEmpty()) {
                            directionOfUpcomingWall = Direction.entries.random()
                            WallSpawnerState.SPAWNING
                        } else {
                            WallSpawnerState.INTENDING_TO_CREATE_1_WALL
                        }


                    }
                    WallSpawnerMode.WALLS_FROM_ALL_DIRECTIONS -> {
                        WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE
                    }
//                    WallSpawnerMode.WALLS_ARE_UNPREDICTABLE -> TODO()
//                    WallSpawnerMode.WALLS_REVERSE -> TODO()
                }

                attemptChangingStateTo(wantedState)
            } //endregion

            WallSpawnerState.SPAWNING -> { //region SPAWNING
                createNewWall(directionOfUpcomingWall, false)
                attemptChangingStateTo(WallSpawnerState.IDLE)
            } //endregion

            WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE -> { //region SPAWNING_MULTIPLE_WALLS_AT_ONCE
                // one wall from the wave must not be psych, while the rest will be psych. since the directions are shuffled, we can just take the first element.
                createNewWall(directionsOfUpcomingWalls.removeFirst(),false)

                // now make the remaining walls to be psych
                directionsOfUpcomingWalls.forEach { createNewWall(it, true) }
                directionsOfUpcomingWalls.clear()

                attemptChangingStateTo(WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS)
            }//endregion

            WallSpawnerState.INTENDING_TO_CREATE_1_WALL -> {  //region INTENDING_TO_CREATE_1_WALL

                val weightsOfDirections = mutableMapOf<Direction, Int>() // A map to hold the weights of each direction

                // gather the direction of the last wall that was spawned
                val directionOfLastWall = existingWallsList.last().directionWallComesFrom

                // Assign weights to each direction based on the mode we're at
                when (wallSpawningMode) {
                    WallSpawnerMode.WALL_CHAINER -> {
                        // If the last wall was spawned in a different direction, we can spawn a wall in the same direction
                        weightsOfDirections[directionOfLastWall] = 5
                        weightsOfDirections[directionOfLastWall.getClockwise()] = 1
                        weightsOfDirections[directionOfLastWall.getOpposite()] = 1
                        weightsOfDirections[directionOfLastWall.getCounterClockwise()] = 1
                    }
                    else -> throw IllegalArgumentException("HITW: Invalid wall spawning mode to be at for this state: $wallSpawningMode")
                }


                // Select a direction based on the weights
                directionOfUpcomingWall = run {
                    val totalWeight: Int = weightsOfDirections.values.sum()

                    val randomValue: Int = Random().nextInt(totalWeight)
                    var cumulativeWeight = 0

                    for ((direction, weight) in weightsOfDirections) {
                        cumulativeWeight += weight
                        if (randomValue < cumulativeWeight) return@run direction
                    }

                    throw IllegalStateException("HITW: No direction selected, something went wrong with the weights")
                }

                activateTaskAfterConditionIsMet(
                    1L,
                    {isSafeToSpawnWall()},
                    {attemptChangingStateTo(WallSpawnerState.SPAWNING)}
                )


                attemptChangingStateTo(WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN)
            } //endregion

            WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE -> { //region INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE
                // take randomly between 2 and 4 directions from the Direction enum to add to the DirectionsOfUpcomingWalls
                val numOfWallsToSpawn: Int = Random().nextInt(2,4+1)

                val availableDirections: List<Direction> = Direction.entries.shuffled().take(numOfWallsToSpawn)

                directionsOfUpcomingWalls.addAll(availableDirections)

                activateTaskAfterConditionIsMet(
                    1L,
                    {existingWallsList.isEmpty()},
                    {attemptChangingStateTo(WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE)}
                )

                attemptChangingStateTo(WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN)

            }//endregion

            WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN -> { //region WAITING_FOR_NEXT_WALL
            } //endregion

            WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS -> {//region WAITING_TILL_THERE_ARE_NO_EXISTING_WALLS
                if (existingWallsList.isEmpty()) attemptChangingStateTo(WallSpawnerState.IDLE)
            }//endregion

            WallSpawnerState.DO_NO_ACTION -> {}
        }
    }

    override fun prepareArea() {
        fun getGameBaseFolder(): File {
            check(plugin is MinigamePlugin) { "Invalid plugin type" }
            val baseFolder: File = plugin.getSchematicsFolder(HITWConst.GAME_FOLDER)
            Objects.requireNonNull(baseFolder, "Game base folder not found")
            return baseFolder
        }

        fun loadMapSchematics(baseFolder: File) {
            // Check if the base folder exists and is a directory. If not, throw an exception. Otherwise, proceed to find the map schematics.
            val files: Array<File> = baseFolder.listFiles() ?: throw IOException("No files found in base folder named ${baseFolder.name}")

            selectedMapBaseFile = Arrays.stream(files)
                .filter { file: File -> file.isDirectory() && file.getName() == mapName }
                .findFirst()
                .orElse(null)
        }

        fun processMapComponents() {
            val mapComponents: Array<File> = selectedMapBaseFile.listFiles()
            for (component in mapComponents) {
                when (component.getName()) {
                    HITWConst.PLATFORMS_FOLDER -> {
                        platformSchematics = component.listFiles() ?: throw IOException("No platform schematics found in ${component.name}")
                    }
                    HITWConst.WALLPACK_FOLDER -> {
                        wallPackSchematics = component.listFiles() ?: throw IOException("No wall pack schematics found in ${component.name}")
                    }
                    HITWConst.MAP_FOLDER -> {
                        mapSchematic = component.listFiles()?.firstOrNull()
                            ?: throw IOException("No map schematic found in ${component.name}")
                    }
                }
            }
        }


        // Clear the area around the spawn point
        this.nukeArea(HITWConst.Locations.PIVOT, 40)

        try {
            val baseFolder = getGameBaseFolder()
            loadMapSchematics(baseFolder)
            processMapComponents()
        } catch (e: IOException) {
            logger().error("HITW: I/O failure while preparing area", e)
            endGame(thePlayer);
        } catch (e: IllegalStateException) {
            logger().error("HITW: Invalid state during map preparation", e)
            endGame(thePlayer);
        } catch (e: Exception) {
            logger().error("HITW: Unexpected error during game setup", e)
            endGame(thePlayer);
        }

        // Load the map schematic (the deco arena)
        BuildLoader.loadSchematicByFileAndLocation(mapSchematic, HITWConst.Locations.CENTER_OF_MAP)
        // Load the platform schematic (the platform that players will stand on)
        BuildLoader.loadSchematicByFileAndLocation(platformSchematics[2], HITWConst.Locations.PLATFORM)
    }

    override fun prepareGameSetting(player: Player) {
        //if we want to test the game easily, we'll set the isDevelopment flag to true
        if (HITWConst.isInDevelopment) {
            player.gameMode = GameMode.CREATIVE
        } else {
            super.prepareGameSetting(player)
            player.gameMode = GameMode.ADVENTURE
            player.teleport(HITWConst.Locations.SPAWN) // Teleport the player to the spawn point of the game
        }

        //give the player infinite jump boost 2.
        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP_BOOST, -1, 1, false))
    }

    fun deleteWall(wall: Wall) {
        BuildLoader.deleteSchematic(wall.wallRegion.minimumPoint, wall.wallRegion.maximumPoint)
        // delete the wall reference from the AliveWallsList
        val hasWallBeenDeleted = existingWallsList.remove(wall)

        if (!hasWallBeenDeleted) {
            logger().warn("HITW: Wall deletion failed, wall not found in the alive walls list")
        }
    }

    // DO NOT MODIFY THIS FOR DEBUGGING PURPOSES
    fun createNewWall(direction: Direction,isPsych: Boolean) {
        val wallFile = wallPackSchematics.random() // Randomly select a wall from the wall pack
        val shouldBeFlipped: Boolean = Random().nextBoolean() // Randomly decide if the wall should be flipped

        val newWall = Wall(wallFile, direction, shouldBeFlipped,isPsych) // Create a new wall
        existingWallsList.add(newWall) // Add the new wall to the list of alive walls

    }

    //MODIFY THIS FOR DEBUGGING PURPOSES
    fun createNewWall() {
        val wallFile = wallPackSchematics.random() // Randomly select a wall from the wall pack
        val direction = Direction.entries.toTypedArray().random() // Randomly select a direction for the wall to come from
        val shouldBeFlipped: Boolean = Random().nextBoolean() // Randomly decide if the wall should be flipped
        val newWall = Wall(wallFile, direction,shouldBeFlipped,false) // Create a new wall
        existingWallsList.add(newWall) // Add the new wall to the list of alive walls


        newWall.showBlocks() // Show the corners of the wall for debugging purposes
        Bukkit.getServer().broadcast(Component.text("flipped: ${newWall.isFlipped}. DirectionWallCome: ${newWall.directionWallComesFrom}").color(
            NamedTextColor.DARK_AQUA))
    }

    fun clearWalls() {
        while (existingWallsList.isNotEmpty()) {
            deleteWall(existingWallsList[0])
        }
        directionsOfUpcomingWalls.clear()
    }

    fun getAliveMovingWalls(): List<Wall> {
        return existingWallsList.filter { !it.shouldBeStopped } // Return only the walls that are currently moving
    }
    fun getWallsThatAreStopped(): List<Wall> {
        return existingWallsList.filter { it.shouldBeStopped } // Return only the walls that are currently stopped
    }


}

