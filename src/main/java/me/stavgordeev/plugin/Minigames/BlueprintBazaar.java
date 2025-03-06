package me.stavgordeev.plugin.Minigames;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlueprintBazaar {
    private final Plugin plugin;
    private volatile boolean isGameRunning;
    private volatile boolean isGamePaused;
    private Player thePlayer;

    public BlueprintBazaar(Plugin plugin) {
        this.plugin = plugin;
    }
}
