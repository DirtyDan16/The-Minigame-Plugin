package me.stavgordeev.plugin.Minigames;

import me.stavgordeev.plugin.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlueprintBazaar extends MinigameSkeleton {
    private final Plugin plugin;
    private volatile boolean isGameRunning;
    private volatile boolean isGamePaused;
    private Player thePlayer;

    public BlueprintBazaar (Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start(Player player) throws InterruptedException {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"));
            return;
        } else {
            Bukkit.getServer().broadcast(Component.text("Minigame started! Name: Blueprint Bazaar"));
        }

        thePlayer = player;
        isGameRunning = true;
        isGamePaused = false;
    }

    @Override
    public void startFastMode(Player player) throws InterruptedException {
        start(player);
    }

    @Override
    public void pauseGame(Player player) {

    }

    @Override
    public void resumeGame(Player player) {

    }

    @Override
    public void endGame(Player player) {

    }

    @Override
    public boolean isPlayerInGame(Player player) {
        return false;
    }

    @Override
    public void nukeArea(Location center, int radius) {
        Utils.nukeGameArea(center, radius);
    }




}
