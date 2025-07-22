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
import me.stavgordeev.plugin.Utils.getNextWeighted
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class HoleInTheWall (plugin: Plugin?) : MinigameSkeleton(plugin) {
    //region vars

    private lateinit var selectedMapBaseFile: File
    private lateinit var platformSchematics: Array<File> //the platform stages for a given map
    private lateinit var wallPackSchematics: Array<File> //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.
    private lateinit var mapSchematic: File //the map schematic that is being played.
    private lateinit var mapName: String //the map name that is being played. gets a value on the start() method.


    //the periodic task that runs every second to update the game state
    private lateinit var gameEvents: BukkitTask

    // A list of runnables that are actively running in the game. we keep track of them so that we can cancel them conveniently...
    // for example, when we switch the mode of the wall spawner, we want to cancel all the runnables that want to switch the state of the wall spawner in the background.
    private val runnables: MutableList<BukkitRunnable> = mutableListOf()




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

    private var stateOfWallSpawner: WallSpawnerState = WallSpawnerState.DO_NO_ACTION // The state of the wall spawner. This is used to determine what action is being done at any given moment and to ensure that nothing unexpected or unwanted occurs with behaviors to walls.

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
        stateOfWallSpawner = WallSpawnerState.IDLE // Set the initial state of the wall spawner to IDLE

        this.mapName = mapName
        start(player)

        startRepeatingGameLoop()
    }

    fun changeWallSpawningMode(mode: String) {
        // This func can be called whether the game is alive or not. (for the command that uses it

        fun changeMode(mode: WallSpawnerMode) {
            wallSpawningMode = mode

            // cancel all the runnables that are in charge of changing the state of the wall spawner, and then clear the list of runnables after canceling them
            //we do .toList() to avoid ConcurrentModificationException
            runnables.toList().forEach { it.cancel() }
            runnables.clear()

            // Clear the list of walls that were planned to be spawned in the game, since otherwise, when we will spawn in walls, the old walls will spawn along with the new ones. (which will deff make walls collide with each other)
            upcomingWalls.clear()

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

    override fun endGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("game is not running!").color(NamedTextColor.RED))
            return
        }
        super.endGame()
        // Cancel the periodic task that updates the game state and handles all game events - such as wall movement, wall spawning, and wall deletion.
        gameEvents.cancel()

        runnables.forEach { it.cancel() }
        runnables.clear()

        stateOfWallSpawner = WallSpawnerState.DO_NO_ACTION // Reset the state of the wall spawner

        // Clear the list of alive walls
        existingWallsList.clear()

        // Clear the walls that were planned to be pasted into existence
        upcomingWalls.clear()


        this.nukeArea(HITWConst.Locations.PIVOT, 60) // Clear the area around the spawn point
    }

    override fun pauseGame() {
        super.pauseGame()
        // Cancel the periodic task that updates the game state and handles all game events - such as wall movement, wall spawning, and wall deletion.
        gameEvents.cancel()
    }

    override fun resumeGame() {
        super.resumeGame()
        // Start the periodic task that updates the game state and handles all game events - such as wall movement, wall spawning, and wall deletion.
        startRepeatingGameLoop()
    }


    private fun startRepeatingGameLoop() {
        fun handlePsychWallsThatRanOutOfLifespan(wall: Wall) {
            // If the wall is a psych wall, we will keep it existing for a lil, then later decide if it should be removed or not.
            Bukkit.getScheduler().runTaskLater(MinigamePlugin.plugin, Runnable {
                // If the wall is chosen to be removed, we'll remove it, otherwise, we will resume its movement after a delay.
                if (wall.shouldRemovePsychThatStopped) {
                    wall.shouldBeRemoved = true
                } else {
                    activateTaskAfterConditionIsMet(
                        condition = {getAliveMovingWalls().isEmpty()} ,
                        action = {
                            wall.shouldBeStopped = false
                            wall.lifespanRemaining = HITWConst.PSYCH_WALL_THAT_RETURNS_TO_MOVING_LIFESPAN // Reset the lifespan of the wall to a lifespan that is enough for it to reach the same distance as a regular wall.

                            // get rid of the identity of the wall - since psych walls should only stop themselves once, and we don't want for them to stop later on when the lifespan is 0 again
                            wall.isPsych = false

                            wall.isBeingHandled = false
                        }
                    )
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
            endGame()
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

                when {
                    //TODO: currently, regular walls are removed immediately, but we can make it so that they can be stopped instead of removed for various reasons
                    !wall.isPsych -> wall.shouldBeRemoved = true // If the wall is not a psych wall, we will remove it immediately.

                    wall.isPsych && !wall.shouldBeRemoved -> {
                        if (!wall.isBeingHandled) {
                            wall.isBeingHandled = true
                            handlePsychWallsThatRanOutOfLifespan(wall)
                        }
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

    val upcomingWalls: MutableList<Wall> = mutableListOf()// A list of walls that are upcoming to be spawned. This is used to keep track of walls that are about to be spawned in the game.

    // A flag for the State: INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE
    // With mode: WALLS_FROM_2_OPPOSITE_DIRECTIONS
    // Indicates if the upcoming real wall is coming from the same direction as the last wall that was spawned.
    private var isUpcomingRealWallComingFromSameDirection: Boolean = Random.nextBoolean()

    private fun manageWallSpawning() {
        //TODO: the logic currently is very dull and incomplete
        //only works with adding 1 wall at a time
        fun isSafeToSpawnWall() : Boolean {

            val directionsExistingWallsHave: Set<Direction> = existingWallsList.map { it.directionWallComesFrom }.toSet()
            val directionOfUpcomingWall: Direction = upcomingWalls[0].directionWallComesFrom

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
                            WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS, //used only when we change the mode of the wall spawner, when we cancel runnables that are sending you to a desired state..
                            WallSpawnerState.SPAWNING,
                            WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE
                )

                WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE -> wantedState in setOf(
                            WallSpawnerState.IDLE
                )

                WallSpawnerState.SPAWNING -> wantedState in setOf(
                            WallSpawnerState.IDLE
                )

                WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS -> wantedState in setOf(
                            WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS,  //THIS IS CRITICAL TO HAVE SINCE WHEN CHANGING MODES, we might try to change states to this state multiple times, from the condition that is called when the runable is cancelled which are canceled when the mode is changed
                            WallSpawnerState.IDLE
                )

                WallSpawnerState.DO_NO_ACTION -> wantedState in setOf(
                            WallSpawnerState.IDLE
                )
            }

            if (!canTransition) {
                Bukkit.getServer().broadcast(Component.text("HITW: Cannot transition from $stateOfWallSpawner to $wantedState").color(NamedTextColor.RED))
                pauseGame()
            }

            stateOfWallSpawner = wantedState

            Bukkit.getServer().broadcast(Component.text("state = $stateOfWallSpawner").color(NamedTextColor.GRAY))
            //Bukkit.getServer().broadcast(Component.text("mode = $wallSpawningMode").color(NamedTextColor.WHITE))
        }

        // The State Evaluator.
        when (stateOfWallSpawner) {
            WallSpawnerState.IDLE -> { //region IDLE
                if (!isGameRunning) return

                val wantedState = when (wallSpawningMode) {
                    WallSpawnerMode.WALL_CHAINER -> {
                        // if we don't have any walls in the arena, we can add one immediately, otherwise we'll decide where and when to add it via the bridger states
                        if (existingWallsList.isEmpty()) {
                            // Create a new wall with the a random direction and add it to the upcoming walls list
                            createNewWall(Direction.entries.random(), false)

                            WallSpawnerState.SPAWNING
                        } else {
                            WallSpawnerState.INTENDING_TO_CREATE_1_WALL
                        }


                    }
                    WallSpawnerMode.WALLS_FROM_ALL_DIRECTIONS,
                    WallSpawnerMode.WALLS_FROM_2_OPPOSITE_DIRECTIONS -> {
                        WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE
                    }
//                    WallSpawnerMode.WALLS_ARE_UNPREDICTABLE -> TODO()
//                    WallSpawnerMode.WALLS_REVERSE -> TODO()

                }

                attemptChangingStateTo(wantedState)
            } //endregion

            WallSpawnerState.SPAWNING -> { //region SPAWNING
                bringWallToLife(upcomingWalls[0]) // Make the wall exist in the world by loading the schematic
                upcomingWalls.clear()

                attemptChangingStateTo(WallSpawnerState.IDLE)
            } //endregion

            WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE -> { //region SPAWNING_MULTIPLE_WALLS_AT_ONCE
                upcomingWalls.forEach { wall -> bringWallToLife(wall) }
                upcomingWalls.clear()

                attemptChangingStateTo(WallSpawnerState.IDLE)
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
                    else -> throw IllegalArgumentException("HITW: Invalid wall spawning mode: $wallSpawningMode to be at for this state: $stateOfWallSpawner")
                }

                // Select a direction based on the weights
                val directionOfUpcomingWall = Random.getNextWeighted(weightsOfDirections)

                createNewWall(directionOfUpcomingWall, false) // Create a new wall with the selected direction and add it to the upcoming walls list

                val runnable = activateTaskAfterConditionIsMet(
                    condition = {isSafeToSpawnWall()},
                    action =  {attemptChangingStateTo(WallSpawnerState.SPAWNING)},
                    actionToDoIfCanceled =  {attemptChangingStateTo(WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS)}
                )
                runnables.add(runnable)

                attemptChangingStateTo(WallSpawnerState.WAITING_A_LIL_TILL_WALL_HAS_SPACE_TO_SPAWN)
            } //endregion

            WallSpawnerState.INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE -> { //region INTENDING_TO_CREATE_MULTIPLE_WALLS_AT_ONCE
                var conditionToSwapState: () -> Boolean = { true } // Default condition to swap state, will be set later based on the mode

                when (wallSpawningMode) {
                    WallSpawnerMode.WALLS_FROM_ALL_DIRECTIONS -> {
                        // take randomly between 2 and 4 directions from the Direction enum to add to the DirectionsOfUpcomingWalls
                        val numOfWallsToSpawn = Random.nextInt(2, 4 + 1)

                        val directionsToSpawn = Direction.entries.shuffled().take(numOfWallsToSpawn).toMutableList()

                        // one wall from the wave must not be psych, while the rest will be psych. we'll take the first direction from the directionsOfUpcomingWalls and spawn it as a regular wall. (states that lead to this state may have shuffled the directions)
                        for (direction in directionsToSpawn) {
                            val isPsych =
                                direction != directionsToSpawn.first() // The first wall will not be a psych wall, the rest will be

                            // Randomly decide if the wall should be removed or not.
                            // 66% - to get removed, 34% - to stay.
                            val chosenToBeRemoved =
                                (0..100).random() <= HITWConst.WallSpawnerModes.WALLS_FROM_ALL_DIRECTIONS.CHANCE_THAT_PSYCH_WALL_WILL_GET_REMOVED

                            createNewWall(direction, isPsych, chosenToBeRemoved)
                        }

                        // If we are spawning walls from all directions, we will wait until there are no existing walls
                        conditionToSwapState = { existingWallsList.isEmpty() }
                    }

                    WallSpawnerMode.WALLS_FROM_2_OPPOSITE_DIRECTIONS -> {
                        if (existingWallsList.isEmpty()) {
                            Direction.entries.random().let { direction ->
                                createNewWall(direction, false)
                                createNewWall(direction.getOpposite(), true)
                            }
                        } else {
                            // Get the wall that is not a psych wall out of the walls
                            val realWall: Wall = existingWallsList.last { wall -> !wall.isPsych }
                            val directionOfRealWall = realWall.directionWallComesFrom

                            fun createDuo(isPsychA: Boolean, isPsychB: Boolean) {
                                createNewWall(directionOfRealWall, isPsychA)
                                createNewWall(directionOfRealWall.getOpposite(), isPsychB)
                            }


                            // If the upcoming real wall is being planned to come from the same direction as the last wall, we will create a duo of walls where the first wall is a real wall and the second wall is a psych wall.
                            if (isUpcomingRealWallComingFromSameDirection) {
                                createDuo(false, true)

                                // Randomly decide if the next real wall will come from the same direction as the last wall.
                                isUpcomingRealWallComingFromSameDirection =
                                    (0..100).random() <= HITWConst.WallSpawnerModes.WALLS_FROM_2_OPPOSITE_DIRECTIONS.CHANCE_THAT_WALL_WILL_SPAWN_FROM_THE_SAME_DIRECTION
                            } else {
                                // Now, to create a real wall that comes from the opposite direction of the last real wall, we need to first wait until the last real wall from the opposite direction has traveled enough distance. Until then, we will stall by making both walls psych walls.

                                if (realWall.lifespanRemaining < 10) {
                                    createDuo(true, false)

                                    isUpcomingRealWallComingFromSameDirection = true
                                } else {
                                    createDuo(true, true)
                                }
                            }


                            conditionToSwapState =
                            {
                                if (getAliveMovingWalls().isEmpty()) true
                                else {
                                    // If the last wall that was spawned has traveled enough distance, we can swap state.
                                    getAliveMovingWalls().last().lifespanTraveled >= HITWConst.WallSpawnerModes.WALLS_FROM_2_OPPOSITE_DIRECTIONS.MINIMUM_SPACE_BETWEEN_2_WALLS_FROM_THE_SAME_DIRECTION
                                }
                            }
                        }
                        // Make it so that when the lifespan of any of those walls has reached 0, they'll immediately be removed, instead of just stopping in place.
                        upcomingWalls.forEach { it -> it.shouldBeRemoved = true}
                    }

                    else -> throw IllegalArgumentException("HITW: Invalid wall spawning mode: $wallSpawningMode to be at for this state: $stateOfWallSpawner")
                }

                val runnable = activateTaskAfterConditionIsMet(
                    condition =  conditionToSwapState ,
                    action =  {attemptChangingStateTo(WallSpawnerState.SPAWNING_MULTIPLE_WALLS_AT_ONCE)},
                    actionToDoIfCanceled =  {attemptChangingStateTo(WallSpawnerState.SWAPPING_TO_IDLE_WHEN_THERE_ARE_NO_EXISTING_WALLS)}
                )
                runnables.add(runnable) // Add the runnable to the list of runnables so that we can cancel it later if needed (like when the mode is changed)

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
            endGame();
        } catch (e: IllegalStateException) {
            logger().error("HITW: Invalid state during map preparation", e)
            endGame();
        } catch (e: Exception) {
            logger().error("HITW: Unexpected error during game setup", e)
            endGame();
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

    //DO MODIFY THIS FOR DEBUGGING PURPOSES
    fun createNewWall() {
        val wallFile = wallPackSchematics.random() // Randomly select a wall from the wall pack
        val direction = Direction.entries.toTypedArray().random() // Randomly select a direction for the wall to come from
        val shouldBeFlipped: Boolean = Random.nextBoolean() // Randomly decide if the wall should be flipped
        val newWall = Wall(wallFile, direction,shouldBeFlipped) // Create a new wall

        bringWallToLife(newWall) // Make the wall exist in the world by loading the schematic


        //newWall.showBlocks() // Show the corners of the wall for debugging purposes
        Bukkit.getServer().broadcast(Component.text("flipped: ${newWall.isFlipped}. DirectionWallCome: ${newWall.directionWallComesFrom}").color(
            NamedTextColor.DARK_AQUA))
    }



    // DO NOT MODIFY THIS FOR DEBUGGING PURPOSES

    fun createNewWall(direction: Direction,isPsych: Boolean, shouldPsychDieWhenStopped: Boolean = true) {
        val wallFile = wallPackSchematics.random() // Randomly select a wall from the wall pack
        val shouldBeFlipped: Boolean = Random.nextBoolean() // Randomly decide if the wall should be flipped


        val newWall = Wall(wallFile, direction, shouldBeFlipped,isPsych,shouldPsychDieWhenStopped) // Create a new wall

        upcomingWalls.add(newWall) // Add the new wall to the list of upcoming walls
    }

    fun bringWallToLife(wall: Wall) {
        // Make the wall exist in the world by loading the schematic
        wall.makeWallExist()
        // Add the new wall to the list of existing walls. the wall is added at the end of the list!
        existingWallsList.add(wall)
    }



    fun clearWalls() {
        while (existingWallsList.isNotEmpty()) {
            deleteWall(existingWallsList[0])
        }
    }

    fun getAliveMovingWalls(): List<Wall> {
        return existingWallsList.filter { !it.shouldBeStopped } // Return only the walls that are currently moving
    }
    fun getWallsThatAreStopped(): List<Wall> {
        return existingWallsList.filter { it.shouldBeStopped } // Return only the walls that are currently stopped
    }


}

