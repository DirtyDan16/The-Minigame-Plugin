package me.stavgordeev.plugin.Minigames;
// src/main/java/me/stavgordeev/plugin/Minigames/MinigameSkeleton.java
import me.stavgordeev.plugin.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import static org.bukkit.Bukkit.getWorld;

public abstract class MinigameSkeleton {
    protected final Plugin plugin;
    protected volatile boolean isGameRunning;
    protected volatile boolean isGamePaused;
    protected Player thePlayer;

    protected MinigameSkeleton(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the minigame.
     * If the game is already running, it should not start the game again.
     *
     * The method calls methods that prepare the area (prepareArea()) and the game settings (prepareGameSetting()) which are abstract and should be implemented in the subclass.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    public void start(Player player) throws InterruptedException {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"));
            return;
        } else {
            Bukkit.getServer().broadcast(Component.text("Minigame started!").color(NamedTextColor.GREEN));
        }

        thePlayer = player;
        isGameRunning = true;
        isGamePaused = false;

        //----- List Of Actions To Be Done When The Game Starts -----//
        prepareArea();
        prepareGameSetting(player);
        //----------------------------------------------------------------//

    }

    /**
     * Starts the minigame in fast mode. This is essentially the same as start(), but it is the hard mode of the minigame.
     * The increased difficulty should be handled in the minigame itself.
     * Should call start() as well.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    public void startFastMode(Player player) throws InterruptedException {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"));
            return;
        }

        start(player);
    }

    /**
     * Pauses the game. Paused games can be resumed, and they keep certain logic and game logic. Should be followed with code that pauses the game, like stopping timers, freezing entities...
     */
    public void pauseGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"));
            return;
        } else if (isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already paused!"));
            return;
        }

        isGamePaused = true;
        Bukkit.getServer().broadcast(Component.text("Minigame paused!"));
    }

    /**
     * Resumes the game. Resumed games should be able to continue from where they were paused. should be followed with code that resumes the game, like starting timers, unfreezing entities...
     */
    public void resumeGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"));
            return;
        } else if (!isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not paused!"));
            return;
        }
        isGamePaused = false;
        Bukkit.getServer().broadcast(Component.text("Minigame resumed!"));
    }

    /**
     * Ends the game. Should be followed with code that cleans up the arena, the gamerules... Should also be called when the game is interrupted.
     */
    public void endGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"));
            return;
        }
        Bukkit.getServer().broadcast(Component.text("Minigame ended!").color(NamedTextColor.GREEN));

        pauseGame();
        isGameRunning = false;
        isGamePaused = false;
        thePlayer = null;
    }

    /**
     * Checks if a player is in the minigame. This will be used for event handling, such as player death.
     * @param player The player to check
     * @return True if the player is in the minigame, false otherwise
     */
    public boolean isPlayerInGame(Player player) {
        return isGameRunning && thePlayer != null && thePlayer.equals(player);
    }

    /**
     * Nukes an area. Should be overridden and followed with code that clears the physical area. Typically should be called in endGame().
     * @param center the center of the nuke
     * @param radius the radius of the nuke
     */
    public void nukeArea(Location center, int radius) {
        // Delete the surrounding area.
        Utils.nukeGameArea(center, radius);
    }

    /**
     * Prepares the area. Should be followed with code that prepares the physical area. Typically should be called in start().
     */
    public abstract void prepareArea();

    /**
     * Prepares the game rules.
     * This method is called when the game starts and is responsible for setting up the game environment.
     * This includes setting the weather, time of day, inventory, health, saturation, and other game-related settings.
     *
     * should be overriden with extra settings for the minigame.
     * For example, tping the player to a specific location.
     */
    public void prepareGameSetting(Player player) {
        //clear the weather and set the time to day
        // ~~~~ note: the dimension got is hardcoded, and it is the overworld.
        getWorld("world").setStorm(false);
        getWorld("world").setTime(1000);
        getWorld("world").setThundering(false);


        player.getInventory().clear(); // Clear the player's inventory
        player.setSaturation(20f); // Set the player's saturation to full
        player.setHealth(20.0); // Set the player's health to full
    }
}
