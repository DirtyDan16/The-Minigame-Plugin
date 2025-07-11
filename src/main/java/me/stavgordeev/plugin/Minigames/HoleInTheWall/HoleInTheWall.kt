package me.stavgordeev.plugin.Minigames.HoleInTheWall

import me.stavgordeev.plugin.BuildLoader
import me.stavgordeev.plugin.Direction
import me.stavgordeev.plugin.MinigamePlugin
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HoleInTheWallConst.Timers
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
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HoleInTheWallConst.WallSpawnerState
import me.stavgordeev.plugin.Utils.runTaskWhen
import net.kyori.adventure.text.format.NamedTextColor

class HoleInTheWall (plugin: Plugin?) : MinigameSkeleton(plugin) {
    private lateinit var selectedMapBaseFile: File
    private lateinit var platformSchematics: Array<File> //the platform stages for a given map
    private lateinit var wallPackSchematics: Array<File> //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.
    private lateinit var mapSchematic: File //the map schematic that is being played.
    private lateinit var mapName: String //the map name that is being played. gets a value on the start() method.


    //the periodic task that runs every second to update the game state
    private lateinit var gameEvents: BukkitTask

    private var stateOfWallSpawner: WallSpawnerState = WallSpawnerState.IDLE // The state of the wall spawner. This is used to determine how the walls are spawned and what behavior they have.


    //region ----Game Modifiers that change as the game progresses
    private var timeLeft: Double = Timers.GAME_DURATION.toDouble()
    private var timeElapsed: Double = 0.0 //in seconds
    private var wallSpeed = Timers.WALL_SPEED[0] //in ticks
    private val wallSpeedUpLandmarks: IntArray = Timers.WALL_SPEED_UP_LANDMARKS //in seconds
    private var wallSpeedIndex = 0 //index of the wall speed in the array

    //the current wall difficulty in the pack. starts from EASY and increases as the game progresses.
    // note that previous wall difficulties are also used in the game, but less frequently.
    private var curWallDifficultyInPack = HoleInTheWallConst.WallDifficulty.EASY
    private val increaseWallDifficultyLandmarks: IntArray = Timers.INCREASE_WALL_DIFFICULTY_LANDMARKS //in seconds


    // A list of walls that are currently alive in the game. This is used to keep track of walls that are currently in play.
    // This list is updated as walls are spawned and deleted, and is tackled in the periodic() method.
    val existingWallsList: MutableList<Wall> = mutableListOf()

    private val wallsToDelete: MutableList<Wall> = mutableListOf() // A list of walls that are to be deleted. This is used to delete walls that are no longer alive


    //endregion -----------------------------------------------------------------------------------

    @Throws(InterruptedException::class)
    fun start(player: Player?, mapName: String) {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"))
            return
        }

        this.mapName = mapName
        start(player)

        startRepeatingGameLoop()
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

        this.nukeArea(HoleInTheWallConst.Locations.PIVOT, 60) // Clear the area around the spawn point
    }

    private fun startRepeatingGameLoop() {
        fun handlePsychWallsThatRanOutOfLifespan(wall: Wall) {
            // If the wall is a psych wall, we will keep it existing for a lil, then later decide if it should be removed or not.
            Bukkit.getScheduler().runTaskLater(MinigamePlugin.plugin, Runnable {
                val chosenToBeRemoved = Random().nextBoolean() // Randomly decide if the wall should be removed or not. this is to add some randomness to the game.

                Bukkit.getServer().broadcast(Component.text("chosenToBeRemoved = ${chosenToBeRemoved}").color(
            NamedTextColor.DARK_AQUA))

                // If the wall is chosen to be removed, we'll remove it, otherwise, we will resume its movement after a delay.
                if (chosenToBeRemoved) {
                    wall.shouldBeRemoved = true
                } else {
                    runTaskWhen({getAliveMovingWalls().isEmpty()} ,1L, {
                        wall.shouldBeStopped = false
                        wall.lifespan = HoleInTheWallConst.PSYCH_WALL_THAT_RETURNS_TO_MOVING_LIFESPAN // Reset the lifespan of the wall to a lifespan that is enough for it to reach the same distance as a regular wall.
                        Bukkit.getServer().broadcast(Component.text("life = ${wall.lifespan}").color(NamedTextColor.DARK_AQUA))

                        // get rid of the identity of the wall - since psych walls should only stop themselves once, and we don't want for them to stop later on when the lifespan is 0 again
                        wall.isPsych = false

                        wall.isBeingHandled = false
                    })
                }

            }, Timers.STOPPED_WALL_DELAY_BEFORE_ACTION_DEALT)
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
        if (curWallDifficultyInPack != HoleInTheWallConst.WallDifficulty.VERY_HARD && timeElapsed >= increaseWallDifficultyLandmarks[curWallDifficultyInPack]) {
            when (++curWallDifficultyInPack) {
                HoleInTheWallConst.WallDifficulty.MEDIUM -> {}
                HoleInTheWallConst.WallDifficulty.HARD -> {}
                HoleInTheWallConst.WallDifficulty.VERY_HARD -> {}
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
        if (existingWallsList.size < HoleInTheWallConst.HARD_CAP_MAX_POSSIBLE_AMOUNT_OF_WALLS) {
            // We'll make a state machine. depending on the state of the game, we'll decide to spawn new walls with different behavior and traits.
            manageWallSpawning()
        }
        //endregion

        }, Timers.DELAY_BEFORE_STARTING_GAME,1L)



    }

    lateinit var DirectionOfUpcomingWall: Direction
    private fun manageWallSpawning() {
        fun changeStateTo(newState: WallSpawnerState) {
            stateOfWallSpawner = newState
            Bukkit.getServer().broadcast(Component.text("Wall spawner state changed to: $newState").color(NamedTextColor.GRAY))
        }

        when (stateOfWallSpawner) {
            WallSpawnerState.IDLE -> {
                // If the spawner is idle, we can create a new wall
                val spawningState = listOf(
                    WallSpawnerState.INTENDING_TO_CREATE_WALL_IN_A_DIFFERENT_DIRECTION,
                    WallSpawnerState.INTENDING_TO_CREATE_WALL_IN_THE_SAME_DIRECTION
                ).random() // Randomly select a state to transition to


                changeStateTo(spawningState)
            }

            WallSpawnerState.SPAWNING -> {
                // If the spawner is spawning a wall, we can create a new wall
                //fixme: the isPsych = random() is temp
                createNewWall(DirectionOfUpcomingWall, Random().nextBoolean())
                changeStateTo(WallSpawnerState.IDLE)
            }

            WallSpawnerState.INTENDING_TO_CREATE_WALL_IN_A_DIFFERENT_DIRECTION -> {
                // gather the direction of the last wall that was spawned
                val directionOfLastWall = existingWallsList.lastOrNull()?.directionWallComesFrom ?: Direction.NORTH

                // Randomly select a new direction that is different from the last wall's direction
                //TODO: atm this will spawn walls in the adjacent directions, but we can make it so that it spawns walls in the opposite direction as well.
                DirectionOfUpcomingWall = when (directionOfLastWall) {
                    Direction.SOUTH -> arrayOf(Direction.WEST, Direction.EAST).random()
                    Direction.NORTH -> arrayOf(Direction.WEST, Direction.EAST).random()
                    Direction.WEST -> arrayOf(Direction.NORTH, Direction.SOUTH).random()
                    Direction.EAST -> arrayOf(Direction.NORTH, Direction.SOUTH).random()
                }

                // the time to wait before spawning a new wall from the same direction
                val waitingTime: Long = Timers.DELAY_BEFORE_SPAWNING_A_WALL_FROM_A_DIFFERENT_DIRECTION.random()

                // Schedule a task to change the state to SPAWNING after a delay
                Bukkit.getServer().scheduler.runTaskLater(plugin,
                    Runnable {changeStateTo(WallSpawnerState.SPAWNING)},
                    waitingTime)


                changeStateTo(WallSpawnerState.WAITING_FOR_NEXT_WALL)
            }

            WallSpawnerState.INTENDING_TO_CREATE_WALL_IN_THE_SAME_DIRECTION -> {
                // gather the direction of the last wall that was spawned
                DirectionOfUpcomingWall = existingWallsList.lastOrNull()?.directionWallComesFrom ?: Direction.NORTH

                // the time to wait before spawning a new wall from the same direction
                val waitingTime: Long = Timers.DELAY_BEFORE_SPAWNING_A_WALL_FROM_THE_SAME_DIRECTION.random()

                // Schedule a task to change the state to SPAWNING after a delay
                Bukkit.getServer().scheduler.runTaskLater(plugin,
                    Runnable {changeStateTo(WallSpawnerState.SPAWNING)},
                    waitingTime)


                changeStateTo(WallSpawnerState.WAITING_FOR_NEXT_WALL)
            }

            WallSpawnerState.WAITING_FOR_NEXT_WALL -> {

            }
        }
    }

    override fun prepareArea() {
        fun getGameBaseFolder(): File {
            check(plugin is MinigamePlugin) { "Invalid plugin type" }
            val baseFolder: File = plugin.getSchematicsFolder(HoleInTheWallConst.GAME_FOLDER)
            Objects.requireNonNull<File?>(baseFolder, "Game base folder not found")
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
                    HoleInTheWallConst.PLATFORMS_FOLDER -> {
                        platformSchematics = component.listFiles() ?: throw IOException("No platform schematics found in ${component.name}")
                    }
                    HoleInTheWallConst.WALLPACK_FOLDER -> {
                        wallPackSchematics = component.listFiles() ?: throw IOException("No wall pack schematics found in ${component.name}")
                    }
                    HoleInTheWallConst.MAP_FOLDER -> {
                        mapSchematic = component.listFiles()?.firstOrNull()
                            ?: throw IOException("No map schematic found in ${component.name}")
                    }
                }
            }
        }


        // Clear the area around the spawn point
        this.nukeArea(HoleInTheWallConst.Locations.PIVOT, 40)

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
        BuildLoader.loadSchematicByFileAndLocation(mapSchematic, HoleInTheWallConst.Locations.CENTER_OF_MAP)
        // Load the platform schematic (the platform that players will stand on)
        BuildLoader.loadSchematicByFileAndLocation(platformSchematics[2], HoleInTheWallConst.Locations.PLATFORM)
    }

    override fun prepareGameSetting(player: Player) {
        //if we want to test the game easily, we'll set the isDevelopment flag to true
        if (HoleInTheWallConst.isInDevelopment) {
            player.gameMode = GameMode.CREATIVE
        } else {
            super.prepareGameSetting(player)
            player.gameMode = GameMode.ADVENTURE
            player.teleport(HoleInTheWallConst.Locations.SPAWN) // Teleport the player to the spawn point of the game
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
        aliveWallsList.add(newWall) // Add the new wall to the list of alive walls

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
    }

    fun getAliveMovingWalls(): List<Wall> {
        return existingWallsList.filter { !it.shouldBeStopped } // Return only the walls that are currently moving
    }
    fun getWallsThatAreStopped(): List<Wall> {
        return existingWallsList.filter { it.shouldBeStopped } // Return only the walls that are currently stopped
    }
}

