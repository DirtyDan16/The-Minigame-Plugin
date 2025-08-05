package base.Minigames

import base.Other.Utils.nukeGameArea
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.concurrent.Volatile

abstract class MinigameSkeleton

protected constructor(@JvmField protected val plugin: Plugin) {
    @JvmField
    @Volatile
    protected var isGameRunning: Boolean = false

    @JvmField
    @Volatile
    protected var isGamePaused: Boolean = false
    protected var sender: Player? = null

    /**
    * This list tracks all scheduled tasks that are made via the help of BukkitRunnables. used to cancel the scheduling when desired.
    * This list is automatically called and canceled upon in the endGame() method.
    * a good use for using this is whenever the game is paused and you would want to stop all the tasks.
    */
    protected val runnables: MutableList<BukkitRunnable> = mutableListOf()

    enum class WorldSettingsToTrack {
        TIME_OF_DAY,
        RANDOM_TICK_SPEED
    }

    protected val trackerOfWorldSettingsBeforeStartingGame = mutableMapOf<WorldSettingsToTrack, Any?>(
        WorldSettingsToTrack.TIME_OF_DAY to null,
        WorldSettingsToTrack.RANDOM_TICK_SPEED to null
    )

    /**
     * Starts the minigame.
     * If the game is already running, it should not start the game again.
     *
     * The method calls methods that prepare the area (prepareArea()) and the game settings (prepareGameSetting()) which are abstract and should be implemented in the subclass.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    @Throws(InterruptedException::class)
    open fun start(player: Player) {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"))
            return
        } else {
            Bukkit.getServer().broadcast(Component.text("Minigame started!").color(NamedTextColor.GREEN))
        }

        sender = player
        isGameRunning = true
        isGamePaused = false

        //----- List Of Actions To Be Done When The Game Starts -----//
        prepareArea()
        prepareGameSetting(player)

        //----------------------------------------------------------------//
    }

    /**
     * Starts the minigame in fast mode. This is essentially the same as start(), but it is the hard mode of the minigame.
     * The increased difficulty should be handled in the minigame itself.
     * Should call start() as well.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    @Throws(InterruptedException::class)
    open fun startFastMode(player: Player) {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"))
            return
        }

        start(player)
    }

    /**
     * Pauses the game. Paused games can be resumed, and they keep certain logic and game logic. Should be followed with code that pauses the game, like stopping timers, freezing entities...
     */
    open fun pauseGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return
        } else if (isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already paused!"))
            return
        }

        isGamePaused = true


        Bukkit.getServer().broadcast(Component.text("Minigame paused!"))
    }

    /**
     * Resumes the game. Resumed games should be able to continue from where they were paused. should be followed with code that resumes the game, like starting timers, unfreezing entities...
     */
    open fun resumeGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return
        } else if (!isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not paused!"))
            return
        }
        isGamePaused = false
        Bukkit.getServer().broadcast(Component.text("Minigame resumed!"))
    }

    /**
     * Ends the game. Should be followed with code that cleans up the arena, the gamerules... Should also be called when the game is interrupted.
     */
    open fun endGame() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return
        }
        Bukkit.getServer().broadcast(Component.text("Minigame ended!").color(NamedTextColor.GREEN))

        pauseGame()
        // copy the list so that we don't get ConcurrentModificationException via adding new runnables to the list while iterating over it
        runnables.toList().forEach { it.cancel()}
        runnables.clear()

        isGameRunning = false
        isGamePaused = false
        sender = null
    }

    /**
     * Checks if a player is in the minigame. This will be used for event handling, such as player death.
     * @param player The player to check
     * @return True if the player is in the minigame, false otherwise
     */
    fun isPlayerInGame(player: Player?): Boolean {
        return isGameRunning && sender != null && sender == player
    }

    /**
     * Nukes an area. Should be overridden and followed with code that clears the physical area. Typically should be called in endGame().
     * @param center the center of the nuke
     * @param radius the radius of the nuke
     */
    open fun nukeArea(center: Location, radius: Int) {
        // Delete the surrounding area.
        nukeGameArea(center, radius)
    }

    /**
     * Prepares the area. Should be followed with code that prepares the physical area. Typically should be called in start().
     */
    abstract fun prepareArea()

    /**
     * Prepares the game rules.
     * This method is called when the game starts and is responsible for setting up the game environment.
     * This includes setting the weather, time of day, inventory, health, saturation, and other game-related settings.
     *
     * should be overriden with extra settings for the minigame.
     * For example, tping the player to a specific location.
     */
    open fun prepareGameSetting(player: Player) {
        //clear the weather and set the time to day
        // ~~~~ note: the dimension got is hardcoded, and it is the overworld.
        Bukkit.getWorld("world")!!.setStorm(false)
        Bukkit.getWorld("world")!!.time = 1000
        Bukkit.getWorld("world")!!.isThundering = false


        player.inventory.clear() // Clear the player's inventory
        player.saturation = 20f // Set the player's saturation to full
        player.health = 20.0 // Set the player's health to full
    }
}
