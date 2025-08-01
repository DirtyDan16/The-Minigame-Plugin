package base

import base.Listeners.PlayerDeathListener
import base.Minigames.BlueprintBazaar.BlueprintBazaar
import base.Minigames.BlueprintBazaar.BlueprintBazaarCommands
import base.Minigames.DiscoMayhem.DiscoMayhem
import base.Minigames.DiscoMayhem.DiscoMayhemCommands
import base.Minigames.HoleInTheWall.HoleInTheWall
import base.Minigames.HoleInTheWall.HoleInTheWallCommands
import base.Minigames.MinigameSkeleton
import base.commands.MiscCommands
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class MinigamePlugin : JavaPlugin() {
    lateinit var discoMayhem: DiscoMayhem
    lateinit var blueprintBazaar: BlueprintBazaar
    lateinit var holeInTheWall: HoleInTheWall

    override fun onEnable() {
        plugin = this // Initialize the plugin reference
        world = server.getWorld("world")!! // Initialize the world object

        initSchematicsFolders()

        discoMayhem = DiscoMayhem(this)
        blueprintBazaar = BlueprintBazaar(this)
        holeInTheWall = HoleInTheWall(this)

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
        // Create the BlueprintBazaarBuilds folder if it doesn't exist
        blueprintBazaarSchematicsFolder = File(dataFolder, "BlueprintBazaarBuilds")
        if (!blueprintBazaarSchematicsFolder.exists()) {
            blueprintBazaarSchematicsFolder.mkdirs() // Creates the folder if it doesn't exist
            logger.info("Created BlueprintBazaarBuilds folder.")
        } else {
            logger.info("BlueprintBazaarBuilds folder already exists.")
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
