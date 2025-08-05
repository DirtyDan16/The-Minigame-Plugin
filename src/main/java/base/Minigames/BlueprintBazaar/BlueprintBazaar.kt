package base.Minigames.BlueprintBazaar

import com.sk89q.worldedit.regions.CuboidRegion
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
import base.Minigames.BlueprintBazaar.BPBConst.Locations
import org.bukkit.GameRule
import base.Minigames.MinigameSkeleton.WorldSettingsToTrack.*
import com.sk89q.worldedit.math.BlockVector3

class BlueprintBazaar(plugin: Plugin) : MinigameSkeleton(plugin) {
    //region vars
    /** The builds.*/
    private val allSchematics: Array<File>
    /** The list of available builds. when a build is chosen, it is removed from this list*/
    private val availableSchematics: MutableList<File?> = mutableListOf<File?>()

    private var curBuild: BuildBlueprint? = null
    //endregion

    init {
        if (plugin !is MinigamePlugin) throw IllegalArgumentException("Plugin must be an instance of MinigamePlugin")

        // Gets the schematics folder from the MinigamePlugin.java. This is where the builds are stored.
        val schematicsFolder = plugin.getSchematicsFolder("blueprintbazaar") // The folder where the builds are stored
        this.allSchematics = schematicsFolder.listFiles() // Gets the builds from the folder
    }

    @Throws(InterruptedException::class)
    override fun start(player: Player) {
        super.start(player)

        initSchematics() // Initializes the availableSchematics list

        //prepareNewBuild()
    }

    @Throws(InterruptedException::class)
    override fun startFastMode(player: Player) {
        super.startFastMode(player)
    }

    override fun endGame() {
        super.endGame()


        // set the settings of the world to how they were prior for the start of the minigame.
        BPBConst.WORLD.setGameRule(
            GameRule.RANDOM_TICK_SPEED,
            trackerOfWorldSettingsBeforeStartingGame[RANDOM_TICK_SPEED] as Int
        )


        nukeArea(Locations.GAME_START_LOCATION,25)
    }

    override fun prepareArea() {
        nukeArea(Locations.GAME_START_LOCATION, BPBConst.GAME_AREA_RADIUS)
        initFloor(20, 20, Material.RED_WOOL, Locations.GAME_START_LOCATION, BPBConst.WORLD)
    }

    override fun prepareGameSetting(player: Player) {
        super.prepareGameSetting(player)
        player.teleport(
            Locations.GAME_START_LOCATION.clone().add(0.0, 8.0, 0.0)
        ) // Teleport the player to the start location

        trackerOfWorldSettingsBeforeStartingGame[RANDOM_TICK_SPEED] = BPBConst.WORLD.getGameRuleValue(GameRule.RANDOM_TICK_SPEED)

        BPBConst.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED,0)
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


        if (chosenBuild == null) {
            Bukkit.getServer().broadcast(Component.text("No more builds available!").color(net.kyori.adventure.text.format.NamedTextColor.AQUA))
            endGame()
            return
        }

        // Randomly decide if the build should be mirrored //fixme: false for now
        val shouldBeMirrored = false//Random().nextBoolean()
        // Create the new build
        curBuild = createNewBuild(chosenBuild, Locations.CENTER_BUILD_SHOWCASE_PLOT, shouldBeMirrored)

        val message = "List of ingridients for build:\n ${curBuild?.materialList.toString()} "
        Bukkit.getServer().broadcast(Component.text(message).color(net.kyori.adventure.text.format.NamedTextColor.AQUA))
    }

    private fun createNewBuild(chosenBuild: File, location: Location, shouldBeMirrored: Boolean = false): BuildBlueprint {
        Bukkit.getServer().broadcast(Component.text("New building created!"))

        // physically load the schematic at the given location

        // store the region of the loaded schematic in this
        val region: CuboidRegion = loadSchematicByFileAndLocation(
            chosenBuild,
            location,
            BPBConst.Build.buildFacingDirection,
            shouldBeMirrored,
        ) as CuboidRegion

        val minP = region.minimumPoint
        val maxP = region.maximumPoint

        val buildRegion = CuboidRegion(
            BlockVector3.at(minP.x,minP.y+1,minP.z),
            maxP
        )

        //TODO: will be used later to create the floor of the build that is being built
        val floorRegion = CuboidRegion(
            minP,
            BlockVector3.at(maxP.x,minP.y,maxP.z)
        )

        //now that the schematic is loaded, we can create a new Build object

        // Create a new Build object with the loaded schematic
        return BuildBlueprint(
            buildRegion
        )
    }

    private fun deleteBuild(region: CuboidRegion) {
        // Set all blocks within the boundaries to air
        for (block in region.boundingBox) {
            val blockLocation = Location(
                BPBConst.WORLD,
                block.x.toDouble(),
                block.y.toDouble(),
                block.z.toDouble()
            )
            blockLocation.block.type = Material.AIR
        }
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
                Location(BPBConst.WORLD, (curX - 3).toDouble(), (curY - 2).toDouble(), curZ.toDouble()),
                BPBConst.WORLD
            )
            // Load the schematic
            loadSchematicByFileAndCoordinates(schematic, curX, curY, curZ)

            // Increment the index for the position of the next build
            index++
        }
    }

    //fixme: doesn't get rid of the top of given schematics when we remove an old schematic. didn't manage to solve it.
    fun cycleThroughSchematics() {
        if (!isGameRunning || isGamePaused) {
            sender!!.sendMessage("Game is not currently alive to do this.")
            return
        }

        val runnable = object : BukkitRunnable() {
            var index: Int = 0

            override fun run() {
                // Delete previous build if it exists
                curBuild?.let { deleteBuild(it.region) }

                // Check if we've gone through all schematics
                if (index >= allSchematics.size) {
                    cancel()
                    return
                }

                // Display new schematic
                prepareNewBuild()

                // Move to next schematic
                index++
            }
        }
        runnable.runTaskTimer(plugin, 0L, BPBConst.Timers.DELAY_BETWEEN_SHOWCASING_BUILDS)
        runnables.add(runnable)
    }
}
