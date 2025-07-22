// src/main/java/me/stavgordeev/plugin/listeners/PlayerDeathListener.java
package me.stavgordeev.plugin.Listeners;

import me.stavgordeev.plugin.Minigames.DiscoMayhem.DiscoMayhem;
import me.stavgordeev.plugin.Minigames.HoleInTheWall.HoleInTheWall;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final DiscoMayhem discoMayhem;
    private final HoleInTheWall holeInTheWall;

    public PlayerDeathListener(DiscoMayhem discoMayhem, HoleInTheWall holeInTheWall) {
        this.discoMayhem = discoMayhem;
        this.holeInTheWall = holeInTheWall;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (discoMayhem.isPlayerInGame(event.getEntity())) {
            discoMayhem.endGame();
        }
        if (holeInTheWall.isPlayerInGame(event.getEntity())) {
            holeInTheWall.endGame();
        }
    }
}