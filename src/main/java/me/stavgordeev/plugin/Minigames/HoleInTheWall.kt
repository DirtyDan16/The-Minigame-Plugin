package me.stavgordeev.plugin.Minigames

import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import me.stavgordeev.plugin.Constants.HoleInTheWallConst
import me.stavgordeev.plugin.Constants.HoleInTheWallConst.Timers
import me.stavgordeev.plugin.MinigamePlugin
import net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException
import java.util.*

class HoleInTheWall (plugin: Plugin?) : MinigameSkeleton(plugin) {
    private lateinit var selectedMapBaseFile: File
    private lateinit var platformSchematics: Array<File> //the platform stages for a given map
    private lateinit var wallPackSchematics: MutableList<File> //the wallpack selected from a given map. each element features a group of files of walls, whose grouped via difficulty.
    private lateinit var mapName: String //the map name that is being played. gets a value on the start() method.

    //region ----Game Modifiers that change as the game progresses
    private var timeLeft = Timers.GAME_DURATION
    private var timeElapsed = 0 //in seconds
    private var wallSpeed = Timers.WALL_SPEED[0] //in ticks
    private val wallSpeedUpLandmarks: IntArray = Timers.WALL_SPEED_UP_LANDMARKS //in seconds
    private var wallSpeedIndex = 0 //index of the wall speed in the array

    //the current wall difficulty in the pack. starts from EASY and increases as the game progresses.
    // note that previous wall difficulties are also used in the game, but less frequently.
    private var curWallDifficultyInPack = HoleInTheWallConst.WallDifficulty.EASY
    private val increaseWallDifficultyLandmarks: IntArray = Timers.INCREASE_WALL_DIFFICULTY_LANDMARKS //in seconds

    //endregion -----------------------------------------------------------------------------------
    @Throws(InterruptedException::class)
    fun start(player: Player?, mapName: String) {
        this.mapName = mapName
        super.start(player)

        //--------------
        startGameEvents()
    }

    private fun startGameEvents() {
        //Update every second the time left and the time elapsed, and keep track if certain events should trigger based on the time that has elapsed.
        run {
            object : BukkitRunnable() {
                override fun run() {
                    timeLeft--
                    timeElapsed++
                    if (timeLeft <= 0) {
                        endGame(thePlayer)
                        cancel()
                    }

                    //Check if the wall speed should be increased
                    if (wallSpeedIndex < wallSpeedUpLandmarks.size && timeElapsed >= wallSpeedUpLandmarks[wallSpeedIndex]) {
                        wallSpeed = Timers.WALL_SPEED[++wallSpeedIndex]
                    }

                    //Check if the wall difficulty should be increased
                    //TODO: implement logic
                    if (curWallDifficultyInPack != HoleInTheWallConst.WallDifficulty.VERY_HARD && timeElapsed >= increaseWallDifficultyLandmarks[curWallDifficultyInPack]) {
                        when (++curWallDifficultyInPack) {
                            HoleInTheWallConst.WallDifficulty.MEDIUM -> {}
                            HoleInTheWallConst.WallDifficulty.HARD -> {}
                            HoleInTheWallConst.WallDifficulty.VERY_HARD -> {}
                        }
                    }
                }
            }.runTaskTimer(plugin, 0, 20) // 20 ticks = 1 second
        }
    }

    override fun nukeArea(center: Location?, radius: Int) {
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
                .filter { file: File -> file.isFile() && file.getName() == mapName }
                .findFirst()
                .orElse(null)
        }

        fun processMapComponents() {
            fun loadWallPackSchematics(wallPack: File) {
                val wallFiles: Array<File> = wallPack.listFiles() ?: throw IOException("No wall schematics found")
                Collections.addAll(wallPackSchematics, *wallFiles)
            }

            val mapComponents: Array<File> = selectedMapBaseFile.listFiles()
            for (component in mapComponents) {
                when (component.getName()) {
                    HoleInTheWallConst.PLATFORMS_FOLDER -> platformSchematics = component.listFiles()
                    HoleInTheWallConst.WALLPACK_FOLDER -> loadWallPackSchematics(component)
                    HoleInTheWallConst.MAP_FOLDER -> { /* Reserved for future implementation */
                    }
                }
            }
        }

        try {
            val baseFolder = getGameBaseFolder()
            loadMapSchematics(baseFolder)
            processMapComponents()
        } catch (e: IOException) {
            logger().error("HITW: I/O failure while preparing area", e)
        } catch (e: IllegalStateException) {
            logger().error("HITW: Invalid state during map preparation", e)
        } catch (e: Exception) {
            logger().error("HITW: Unexpected error during game setup", e)
        }
        finally {
            endGame(thePlayer);
        }
    }

    override fun prepareGameSetting(player: Player?) {
    }
}