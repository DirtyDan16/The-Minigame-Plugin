package me.stavgordeev.plugin.Minigames;

import me.stavgordeev.plugin.Utils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class BlueprintBazaar extends MinigameSkeleton {

    public BlueprintBazaar (Plugin plugin) {
        super(plugin);
    }

    @Override
    public void start(Player player) throws InterruptedException {
        super.start(player);
    }

    @Override
    public void startFastMode(Player player) throws InterruptedException {
        super.startFastMode(player);
    }

    @Override
    public void pauseGame(Player player) {
        super.pauseGame(player);
    }

    @Override
    public void resumeGame(Player player) {
        super.resumeGame(player);
    }

    @Override
    public void endGame(Player player) {
        super.endGame(player);
    }

    @Override
    public boolean isPlayerInGame(Player player) {
        return super.isPlayerInGame(player);
    }

    @Override
    public void nukeArea(Location center, int radius) {
        Utils.nukeGameArea(center, radius);
    }
}
