package me.stavgordeev.plugin.Minigames;
// src/main/java/me/stavgordeev/plugin/Minigames/MinigameSkeleton.java
import me.stavgordeev.plugin.MinigamePlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public abstract class MinigameSkeleton {
    /**
     * Starts the minigame. should be followed with code that prepares the arena, the gamerules...
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    public abstract void start(Player player) throws InterruptedException;

    /**
     * Starts the minigame in fast mode. This is essentially the same as start(), but it is the hard mode of the minigame. should call start() as well.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    public abstract void startFastMode(Player player) throws InterruptedException;

    /**
     * Pauses the game. Paused games can be resumed, and they keep certain logic and game logic. should be followed with code that pauses the game, like stopping timers, freezing entities...
     * @param player the player that paused the game
     */
    public abstract void pauseGame(Player player);

    /**
     * Resumes the game. Resumed games should be able to continue from where they were paused. should be followed with code that resumes the game, like starting timers, unfreezing entities...
     * @param player the player that resumed the game
     */
    public abstract void resumeGame(Player player);

    /**
     * Ends the game. should be followed with code that cleans up the arena, the gamerules... Should also be called when the game is interrupted.
     * @param player the player that ended the game
     */
    public abstract void endGame(Player player);

    /**
     * Checks if a player is in the game. should return true if the player is in the game, false otherwise.
     * @param player the player to check
     * @return true if the player is in the game, false otherwise
     */
    public abstract boolean isPlayerInGame(Player player);

    /**
     * Nukes an area. should be followed with code that destroys all entities and blocks in a certain radius around the player.
     * @param center the center of the nuke
     * @param radius the radius of the nuke
     */
    public abstract void nukeArea(Location center, int radius);
}
