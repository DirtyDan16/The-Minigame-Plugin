// src/main/java/me/stavgordeev/plugin/listeners/PlayerDeathListener.java
package me.stavgordeev.plugin.Listeners;

import me.stavgordeev.plugin.Minigames.DiscoMayhem.DiscoMayhem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {
    private final DiscoMayhem discoMayhem;

    public PlayerDeathListener(DiscoMayhem discoMayhem) {
        this.discoMayhem = discoMayhem;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (discoMayhem.isPlayerInGame(event.getEntity())) {
            discoMayhem.endGame(event.getEntity());
        }
    }
}