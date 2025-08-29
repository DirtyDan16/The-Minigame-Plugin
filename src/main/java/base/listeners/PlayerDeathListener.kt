// src/main/java/me/stavgordeev/plugin/listeners/PlayerDeathListener.java
package base.listeners

import base.minigames.disco_mayhem.DiscoMayhem
import base.minigames.hole_in_the_wall.HoleInTheWall
import base.minigames.maze_hunt.MazeHunt
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDeathListener(
    private val discoMayhem: DiscoMayhem,
    private val holeInTheWall: HoleInTheWall,
    private val mazeHunt: MazeHunt
) : Listener {
    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        when {
            mazeHunt.isPlayerInGame(event.entity) -> mazeHunt.endGame()
            discoMayhem.isPlayerInGame(event.entity) -> discoMayhem.endGame()
            holeInTheWall.isPlayerInGame(event.entity) -> holeInTheWall.endGame()
        }
    }
}