// src/main/java/me/stavgordeev/plugin/commands/MinigameCommand.java
package base.minigames.disco_mayhem

import base.commands.MinigameCommandsSkeleton
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class DiscoMayhemCommands(private val discoMayhem: DiscoMayhem) : MinigameCommandsSkeleton() {
    override fun handleCommand(player: Player, command: Command, label: String, args: Array<String>): Boolean {
        when (args[0].lowercase(Locale.getDefault())) {
            "start" -> try {
                discoMayhem.start(player)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            "start_hard_mode" -> try {
                discoMayhem.startFastMode(player)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            "stop" -> discoMayhem.pauseGame()
            "resume" -> discoMayhem.resumeGame()
            "end" -> discoMayhem.endGame()
            "nuke_area" -> discoMayhem.nukeArea(DiscoMayhemConst.GAME_START_LOCATION,DiscoMayhemConst.NUKE_AREA_RADIUS)
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
            return mutableListOf<String>("start", "stop", "start_hard_mode", "resume", "end", "nuke_area")
        }
        return mutableListOf<String>()
    }
}