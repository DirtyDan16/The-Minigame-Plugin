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

    fun getSchematicsFolder(minigame: String): File {
        return when (minigame) {
            "blueprintbazaar" -> File(dataFolder, "BlueprintBazaar")
            "holeinthewall" -> File(dataFolder, "HoleInTheWall")
            else -> throw IllegalStateException("Unexpected value: $minigame")
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
