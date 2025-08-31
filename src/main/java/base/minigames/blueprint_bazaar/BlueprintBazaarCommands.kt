package base.minigames.blueprint_bazaar

import base.commands.MinigameCommandsSkeleton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class BlueprintBazaarCommands(private val blueprintbazaar: BlueprintBazaar) : MinigameCommandsSkeleton() {
    override fun handleCommand(sender: Player, command: Command, label: String, args: Array<String>): Boolean {
        when (args[0].lowercase(Locale.getDefault())) {
            "start" -> try {
                blueprintbazaar.start(sender)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            "start_hard_mode" -> try {
                blueprintbazaar.startFastMode(sender)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            "stop" -> blueprintbazaar.pauseGame()
            "resume" -> blueprintbazaar.resumeGame()
            "end" -> blueprintbazaar.endGame()
            "nuke_area" -> blueprintbazaar.nukeArea(BPBConst.Locations.GAME_START_LOCATION, BPBConst.Locations.GAME_AREA_RADIUS)
            "spawn_build" -> blueprintbazaar.skipToNextBuild()
            "showcase_all_builds" -> blueprintbazaar.loadAllSchematics()
            "init_schematics" -> blueprintbazaar.initSchematics()
            "cycle_through_schematics" -> blueprintbazaar.cycleThroughSchematics()
            else -> Bukkit.getServer().broadcast(Component.text("Unknown command.").color(NamedTextColor.RED))
        }
        return true
    }

    override fun handleTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): MutableList<String> {
        if (args.size == 1) {
            return mutableListOf(
                "start",
                "stop",
                "start_hard_mode",
                "resume",
                "end",
                "nuke_area",
                "spawn_build",
                "showcase_all_builds",
                "cycle_through_schematics",
                "init_schematics"
            )
        }
        return mutableListOf()
    }
}
