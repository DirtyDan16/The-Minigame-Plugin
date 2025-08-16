package base

import base.listeners.PlayerDeathListener
import base.minigames.blueprint_bazaar.BlueprintBazaar
import base.minigames.blueprint_bazaar.BlueprintBazaarCommands
import base.minigames.disco_mayhem.DiscoMayhem
import base.minigames.disco_mayhem.DiscoMayhemCommands
import base.minigames.hole_in_the_wall.HoleInTheWall
import base.minigames.hole_in_the_wall.HoleInTheWallCommands
import base.minigames.MinigameSkeleton
import base.commands.MiscCommands
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MinigamePlugin : JavaPlugin() {

    lateinit var discoMayhem: DiscoMayhem
    lateinit var blueprintBazaar: BlueprintBazaar
    lateinit var holeInTheWall: HoleInTheWall

    override fun onEnable() {
        plugin = this
        initSchematicsFolders()


        discoMayhem = DiscoMayhem(this)
        blueprintBazaar= BlueprintBazaar(this)
        holeInTheWall = HoleInTheWall(this)


        world = server.getWorld("world")!! // Initialize the world object


        // Register the event listeners
        server.pluginManager.registerEvents(PlayerDeathListener(discoMayhem, holeInTheWall), this)


        getCommand("mg_disco_mayhem")?.setExecutor(
            DiscoMayhemCommands(discoMayhem)
        )
        getCommand("mg_blueprint_bazaar")?.setExecutor(
            BlueprintBazaarCommands(blueprintBazaar)
        )
        getCommand("mg_hole_in_the_wall")?.setExecutor(
            HoleInTheWallCommands(holeInTheWall)
        )
        getCommand("misc")?.setExecutor(MiscCommands(this))
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private lateinit var blueprintBazaarSchematicsFolder: File
    private lateinit var holeInTheWallSchematicsFolder: File
    private fun initSchematicsFolders() {
        // Create the BlueprintBazaar folder if it doesn't exist
        blueprintBazaarSchematicsFolder = File(dataFolder, "BlueprintBazaar")
        if (!blueprintBazaarSchematicsFolder.exists()) {
            blueprintBazaarSchematicsFolder.mkdirs() // Creates the folder if it doesn't exist
            logger.info("Created BlueprintBazaar folder.")
        } else {
            logger.info("BlueprintBazaar folder already exists.")
        }

        holeInTheWallSchematicsFolder = File(dataFolder, "HoleInTheWall")
        if (!blueprintBazaarSchematicsFolder.exists()) {
            // Creates the folder if it doesn't exist
            if (blueprintBazaarSchematicsFolder.mkdirs()) logger.info("Created HoleInTheWall folder.")
            else logger.warning("Failed to create HoleInTheWall folder.")
        } else {
            logger.info("HoleInTheWall folder already exists.")
        }
    }

    fun getSchematicsFolder(minigame: String): File {
        return when (minigame) {
            "blueprintbazaar" -> blueprintBazaarSchematicsFolder
            "holeinthewall" -> holeInTheWallSchematicsFolder
            else -> throw IllegalStateException("Unexpected value: " + minigame)
        }
    }

    fun getInstanceOfMinigame(minigame: MinigameType): MinigameSkeleton {
        return when (minigame) {
            MinigameType.HOLE_IN_THE_WALL -> this.holeInTheWall
            MinigameType.DISCO_MAYHEM -> this.discoMayhem
            MinigameType.BLUEPRINT_BAZAAR -> this.blueprintBazaar
        }
    }

    companion object {
        lateinit var plugin: MinigamePlugin
        lateinit var world: World

        enum class MinigameType {
            HOLE_IN_THE_WALL,
            DISCO_MAYHEM,
            BLUEPRINT_BAZAAR
        }
    }
}
