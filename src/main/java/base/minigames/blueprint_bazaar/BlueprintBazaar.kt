package base.minigames.blueprint_bazaar

import base.MinigamePlugin
import base.minigames.MinigameSkeleton
import base.minigames.MinigameSkeleton.WorldSettingsToTrack.GAMEMODE
import base.minigames.MinigameSkeleton.WorldSettingsToTrack.RANDOM_TICK_SPEED
import base.minigames.blueprint_bazaar.BPBConst.Locations
import base.other.BuildLoader
import base.other.BuildLoader.loadSchematicByFileAndCoordinates
import base.other.BuildLoader.loadSchematicByFileAndDirection
import base.utils.Utils.initFloor
import base.utils.extensions_for_classes.clearInvAndGiveItems
import base.utils.extensions_for_classes.getBlockAt
import base.utils.extensions_for_classes.getMaterialAt
import base.utils.extensions_for_classes.plus
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File
import java.io.IOException

class BlueprintBazaar(plugin: Plugin) : MinigameSkeleton() {
    //region vars
    val plugin: MinigamePlugin


    /** The builds.*/
    private lateinit var allSchematics: Array<File>
    /** The list of available builds. when a build is chosen, it is removed from this list*/
    private val availableSchematics: MutableSet<File?> = mutableSetOf()
    private var arena: File? = null

    private var curBuild: BuildBlueprint? = null
    //endregion

    init {
        if (plugin !is MinigamePlugin) throw IllegalArgumentException("Plugin must be an instance of MinigamePlugin")
        this.plugin = plugin
    }

    @Throws(InterruptedException::class)
    override fun start(sender: Player) {
        initSchematics()

        super.start(sender)

        // start the cycle of builds
        prepareNewBuild()
    }

    @Throws(InterruptedException::class)
    override fun startFastMode(player: Player) {
        super.startFastMode(player)
    }

    override fun endGame() {
        super.endGame()

        // set the settings of the world to how they were prior for the start of the minigame.
        trackerOfWorldSettingsBeforeStartingGame.apply {
            BPBConst.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, this[RANDOM_TICK_SPEED] as Int)
            for (player in players) {
                player.gameMode = this[GAMEMODE] as GameMode
                player.activePotionEffects.clear()
                player.allowFlight = false
                player.isFlying = false
            }
        }

        nukeArea(Locations.GAME_START_LOCATION,25)
    }

    override fun prepareArea() {
        nukeArea(Locations.GAME_START_LOCATION, BPBConst.GAME_AREA_RADIUS)
        BuildLoader.loadSchematicByFile(arena!!, Locations.GAME_START_LOCATION)
    }

    override fun prepareGameSetting() {
        super.prepareGameSetting()

        //tracking state
        trackerOfWorldSettingsBeforeStartingGame.apply {
            put(RANDOM_TICK_SPEED, BPBConst.WORLD.getGameRuleValue(GameRule.RANDOM_TICK_SPEED))
            put(GAMEMODE, players[0].gameMode)
        }

        //setting state
        BPBConst.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED,0)

        for (player in players) {
            // Teleport the player to the start location
            player.teleport(Locations.GAME_START_LOCATION.clone().add(0.0, 8.0, 0.0))
            player.gameMode = GameMode.SURVIVAL
            player.allowFlight = true
        }
    }

    /**
     * Initializes the availableSchematics list with all the builds in the schematics folder, as well as some extras.
     */
    fun initSchematics() {
        // Gets the schematics folder from the MinigamePlugin.java. This is where the builds are stored.
        val schematicsFolder = plugin.getSchematicsFolder("blueprintbazaar")

        schematicsFolder.listFiles().forEach { path ->
            when (path.name.substringBefore('.')) {
                "BlueprintBazaarBuilds" -> this.allSchematics = path.listFiles()
                "arena" -> arena = path
                else -> IOException("can't find file in blueprint bazaar")
            }
        }

        availableSchematics += allSchematics.toList()
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
        val chosenBuild = availableSchematics.random()
        availableSchematics.remove(chosenBuild)

        return chosenBuild
    }

    fun prepareNewBuild() {
        val chosenBuild = chooseNewBuild()

        if (chosenBuild == null) {
            Bukkit.getServer().broadcast(Component.text("No more builds available!").color(net.kyori.adventure.text.format.NamedTextColor.AQUA))
            endGame()
            return
        }

        // Randomly decide if the build should be mirrored //fixme: false for now bcuz mirroring does more than wanted and moves the entre pos of the build plot
        val shouldBeMirrored = false//Random().nextBoolean()

        // Create the new build
        curBuild = createNewBuild(chosenBuild, Locations.CENTER_BUILD_SHOWCASE_PLOT, shouldBeMirrored)

        // register the player block placing listener for this build.
        MinigamePlugin.plugin.server.pluginManager.registerEvents(curBuild!!, plugin)

        val message = "List of ingredients for build: \n ${chosenBuild.name} \n ${curBuild?.materialList.toString()} "
        Bukkit.getServer().broadcast(Component.text(message).color(net.kyori.adventure.text.format.NamedTextColor.AQUA))

        for (player in players) {
            player.clearInvAndGiveItems(curBuild!!.materialList,64)
        }
    }

    private fun createNewBuild(chosenBuild: File, location: Location, shouldBeMirrored: Boolean = false): BuildBlueprint {
        Bukkit.getServer().broadcast(Component.text("New building created!"))

        // physically load the schematic at the given location

        // store the region of the loaded schematic in this
        val region: CuboidRegion = loadSchematicByFileAndDirection(
            chosenBuild,
            location,
            BPBConst.Build.buildFacingDirection,
            shouldBeMirrored,
        ) as CuboidRegion

        val minP = region.minimumPoint
        val maxP = region.maximumPoint


        //copy the floor from the displayed build plot to the build plot the player is gonna recreate the build at. also clear any blocks in the area of the recreated plot.
        for (vector in region) {
            when (vector.y) {
                minP.y -> BPBConst.WORLD.getBlockAt(vector + Locations.CENTER_BUILD_PLOT_OFFSET).type = BPBConst.WORLD.getMaterialAt(vector)
                else -> BPBConst.WORLD.getBlockAt(vector + Locations.CENTER_BUILD_PLOT_OFFSET).type = Material.AIR
            }
        }


        val buildRegion = CuboidRegion(
            BlockVector3.at(minP.x,minP.y+1,minP.z),
            maxP
        )

        //now that the schematic is loaded, we can create a new Build object

        // Create a new Build object with the loaded schematic
        return BuildBlueprint(
            this,
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

        // unregister the player block placing listener for the previous build if there's any.
        if (curBuild != null) {
            HandlerList.unregisterAll(curBuild!!)
        }

    }

    fun completeBuild(build: BuildBlueprint) {
        deleteBuild(build.region)
        prepareNewBuild()
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
            val curX = Locations.CENTER_BUILD_SHOWCASE_PLOT.x().toInt() + (10 * (index % 6))
            val curY = (Locations.CENTER_BUILD_SHOWCASE_PLOT.y()).toInt()
            val curZ = (Locations.CENTER_BUILD_SHOWCASE_PLOT.z() + 10 * (index / 6)).toInt()

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


