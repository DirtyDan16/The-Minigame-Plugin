package base.Minigames.BlueprintBazaar

import com.sk89q.worldedit.regions.Region
import base.Other.BuildLoader
import base.Other.BuildLoader.loadSchematicByFileAndCoordinates
import base.Other.BuildLoader.loadSchematicByFileAndLocation
import base.MinigamePlugin
import base.Minigames.MinigameSkeleton
import base.Other.Utils.initFloor
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.util.*
import base.Minigames.BlueprintBazaar.BlueprintBazaarConst.Locations

class BlueprintBazaar(plugin: Plugin?) : MinigameSkeleton(plugin) {
    private val allSchematics: Array<File> // The builds.
    private val availableSchematics: MutableList<File?> = mutableListOf<File?>() // The builds. when a build is chosen, it is removed from this list

    init {
        if (plugin !is MinigamePlugin) throw IllegalArgumentException("Plugin must be an instance of MinigamePlugin")

        // Gets the schematics folder from the MinigamePlugin.java. This is where the builds are stored.
        val schematicsFolder = plugin.getSchematicsFolder("blueprintbazaar") // The folder where the builds are stored
        this.allSchematics = schematicsFolder.listFiles() // Gets the builds from the folder
    }

    @Throws(InterruptedException::class)
    override fun start(player: Player?) {
        super.start(player)

        initSchematics() // Initializes the availableSchematics list
    }

    @Throws(InterruptedException::class)
    override fun startFastMode(player: Player?) {
        super.startFastMode(player)
    }

    override fun prepareArea() {
        nukeArea(Locations.GAME_START_LOCATION, BlueprintBazaarConst.GAME_AREA_RADIUS)
        initFloor(20, 20, Material.RED_WOOL, Locations.GAME_START_LOCATION, BlueprintBazaarConst.WORLD)
    }

    override fun prepareGameSetting(player: Player) {
        super.prepareGameSetting(player)
        player.teleport(
            Locations.GAME_START_LOCATION.clone().add(0.0, 8.0, 0.0)
        ) // Teleport the player to the start location
    }

    /**
     * Initializes the availableSchematics list with all the builds in the schematics folder.
     */
    fun initSchematics() {
        // Adds the builds to the availableSchematics list
        checkNotNull(allSchematics)
        availableSchematics.addAll(allSchematics.toList())
    }

    /**
     * Chooses a new build from the availableSchematics list. The build is taken out of the available list
     * @return The chosen build
     */
    private fun chooseNewBuild(): File? {
        if (availableSchematics.isEmpty()) {
            // Handle the case where there are no available schematics
            plugin.logger.severe("No available schematics to choose from.")
            return null
        }
        // Choose a random build from the schematics folder and delete it from the list
        val getARandomBuild = Random()
        val chosenBuild = availableSchematics.removeAt(getARandomBuild.nextInt(availableSchematics.size))

        return chosenBuild
    }

    fun prepareNewBuild() {
        val chosenBuild = chooseNewBuild()
        // Create the new build
        createNewBuild(chosenBuild!!, Locations.CENTER_BUILD_PLOT)
    }

    private fun deleteBuild(region: Region) {
        // Set all blocks within the boundaries to air
        for (block in region.boundingBox) {
            val blockLocation = Location(
                BlueprintBazaarConst.WORLD,
                block.x.toDouble(),
                block.y.toDouble(),
                block.z.toDouble()
            )
            blockLocation.block.type = Material.AIR
        }
    }


    private fun createNewBuild(chosenBuild: File, location: Location?) {
        Bukkit.getServer().broadcast(Component.text("New building created!"))
        loadSchematicByFileAndLocation(chosenBuild, location)
    }

    /**
     * Loads all the builds in the schematics folder. The builds are loaded in a grid pattern.
     */
    fun loadAllSchematics() {
        var index = 0
        // Load all the builds in the schematics folder
        // Load the schematic relative to the center build plot. The x and z coordinates are Modified in a way that makes the builds appear in a grid.
        for (schematic in allSchematics) {
            // Calculate the x, y, and z coordinates for the build
            val curX = Locations.CENTER_BUILD_PLOT.x().toInt() + (10 * (index % 6))
            val curY = (Locations.CENTER_BUILD_PLOT.y()).toInt()
            val curZ = (Locations.CENTER_BUILD_PLOT.z() + 10 * (index / 6)).toInt()

            // Initialize the floor for this build
            initFloor(
                6,
                6,
                Material.RED_WOOL,
                Location(BlueprintBazaarConst.WORLD, (curX - 3).toDouble(), (curY - 2).toDouble(), curZ.toDouble()),
                BlueprintBazaarConst.WORLD
            )
            // Load the schematic
            loadSchematicByFileAndCoordinates(schematic, curX, curY, curZ)

            // Increment the index for the position of the next build
            index++
        }
    }

    //fixme: doesn't get rid of the top of given schematics when we remove an old schematic. didn't manage to solve it.
    private fun cycleThroughSchematics() {
        object : BukkitRunnable() {
            var index: Int = 0
            var region: Region? = null
            var schematic: File? = null

            override fun run() {
                // Delete previous build if it exists
                if (schematic != null) { deleteBuild(region!!) }

                // Check if we've gone through all schematics
                if (index >= allSchematics.size) {
                    cancel()
                    return
                }

                // Display new schematic
                schematic = chooseNewBuild()
                if (schematic?.exists() == true) {
                    loadSchematicByFileAndLocation(schematic!!, Locations.CENTER_BUILD_PLOT)
                    region = BuildLoader.getRegionFromFile(schematic!!, Locations.CENTER_BUILD_PLOT)
                }

                // Move to next schematic
                index++
            }
        }.runTaskTimer(plugin, 0L, 100L)
    }
}
