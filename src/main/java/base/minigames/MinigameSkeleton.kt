package base.minigames

import base.annotations.CalledByCommand
import base.utils.ExitStatus
import base.utils.Utils
import base.utils.Utils.nukeGameArea
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

@Suppress("NOTHING_TO_INLINE")
abstract class MinigameSkeleton

protected constructor() {

    //region Fields
    var isGameRunning: Boolean = false
    var isGamePaused: Boolean = false
    var sender: Player? = null
    var players: MutableList<Player> = mutableListOf()

    /**
     *
     * This list tracks all scheduled tasks that are made via the help of BukkitRunnables. Used to cancel the scheduling when desired.
     * This list is automatically called and canceled upon in the endGame() method.
     * A good use for using this is whenever the game is paused, and you would want to stop all the tasks.
     */
//    @Deprecated("Use [pausableRunnables] instead for tasks that need to be paused and resumed.", ReplaceWith("pausableRunnables"))
    val runnables: MutableList<BukkitRunnable> = mutableListOf()

    /**
     * This list tracks all scheduled tasks that are made via the help of [Utils.PausableBukkitRunnable]. Used to pause and resume the scheduling when desired.
     * This list is automatically called and canceled upon in the endGame() method.
     * When the game is paused, all the runnables in this list are paused, and when the game is resumed, all the runnables in this list are resumed.
     */
    val pausableRunnables: MutableList<Utils.PausableBukkitRunnable> = mutableListOf()

    enum class WorldSettingsToTrack {
        TIME_OF_DAY,
        RANDOM_TICK_SPEED,
        GAMEMODE,
        HASTE,
        DIFFICULTY
    }

    protected val trackerOfWorldSettingsBeforeStartingGame: MutableMap<WorldSettingsToTrack, Any?> = WorldSettingsToTrack.entries.associateWith { null }.toMutableMap()
    //endregion

    /**
     * Starts the minigame.
     * If the game is already running, it should not start the game again.
     *
     * The method calls methods that prepare the area ([prepareArea]) and the game settings ([prepareGameSetting]) which are abstract and should be implemented in the subclass.
     * @param sender the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    @Throws(InterruptedException::class)
    @CalledByCommand
    open fun start(sender: Player) {}
    protected inline fun startSkeleton(sender: Player) {
        if (isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already running!"))
            return
        } else {
            Bukkit.getServer().broadcast(Component.text("Minigame started!").color(NamedTextColor.GREEN))
        }

        this@MinigameSkeleton.sender = sender
        players += Bukkit.getServer().onlinePlayers
        isGameRunning = true
        isGamePaused = false

        //----- List Of Actions To Be Done When The Game Starts -----//
        prepareArea()
        prepareGameSetting()

        //----------------------------------------------------------------//
    }

    /**
     * Starts the minigame in fast mode. This is essentially the same as [start], but it is the hard mode of the minigame.
     * The increased difficulty should be handled in the minigame itself.
     * Should call [start] as well.
     * @param player the player that started the minigame
     * @throws InterruptedException if the game is interrupted
     */
    @Throws(InterruptedException::class)
    @CalledByCommand
    open fun startFastMode(player: Player) {startFastMode(player)}

    /**
     * Pauses the game. Paused games can be resumed, and they keep certain logic and game logic. Should be overridden and followed with code that pauses the game, like stopping timers, freezing entities...
     */
    @CalledByCommand
    open fun pauseGame() {pauseGameSkeleton()}
    protected inline fun pauseGameSkeleton() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return
        } else if (isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is already paused!"))
            return
        }

        isGamePaused = true

        pausableRunnables.removeIf { it.shouldBeRemoved }

        for (runnable in pausableRunnables) {
            runnable.pause()
        }

        Bukkit.getServer().broadcast(Component.text("Minigame paused!"))
    }

    /**
     * Resumes the game. Resumed games should be able to continue from where they were paused. should be overridden and followed with code that resumes the game, like starting timers, unfreezing entities...
     */
    @CalledByCommand
    open fun resumeGame() {resumeGameSkeleton()}
    protected inline fun resumeGameSkeleton() {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return
        } else if (!isGamePaused) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not paused!"))
            return
        }
        isGamePaused = false

        for (runnable in pausableRunnables) {
            runnable.start()
        }

        Bukkit.getServer().broadcast(Component.text("Minigame resumed!"))
    }

    /**
     * Ends the game. Should be overridden and followed with code that cleans up the arena, the gamerules... Should also be called when the game is interrupted.
     * Should also call [endGameSkeleton] at the start of it
     */
    @CalledByCommand
    open fun endGame() { endGameSkeleton() }
    protected inline fun endGameSkeleton() : ExitStatus {
        if (!isGameRunning) {
            Bukkit.getServer().broadcast(Component.text("Minigame is not running!"))
            return ExitStatus.EARLY_EXIT
        }
        Bukkit.getServer().broadcast(Component.text("Minigame ended!").color(NamedTextColor.GREEN))

        pauseGame()
        // copy the list so that we don't get ConcurrentModificationException via adding new runnables to the list while iterating over it
        runnables.toList().forEach { it.cancel()}
        runnables.clear()

        isGameRunning = false
        isGamePaused = false
        sender = null
        players.clear()

        return ExitStatus.COMPLETED
    }

    /**
     * Checks if a player is in the minigame. This will be used for event handling, such as player death.
     * @param player The player to check
     * @return True if the player is in the minigame, false otherwise
     */
    fun isPlayerInGame(player: Player?): Boolean {
        return isGameRunning && players.contains(player)
    }

    /**
     * Nukes an area. Should be overridden and followed with code that clears the physical area. Typically, it should be called in [endGameSkeleton].
     * @param center the center of the nuke
     * @param radius the radius of the nuke
     */
    open fun nukeArea(center: Location, radius: Int) {
        // Delete the surrounding area.
        nukeGameArea(center, radius)
    }

    /**
     * Prepares the area. Should be followed with code that prepares the physical area. Typically, it should be called in [start].
     */
    abstract fun prepareArea()

    /**
     * Prepares the game rules.
     * This method is called when the game starts and is responsible for setting up the game environment.
     * This includes setting the weather, time of day, inventory, health, saturation, and other game-related settings.
     *
     * Should be overridden with extra settings for the minigame.
     * For example, tping the player to a specific location.
     */
    open fun prepareGameSetting() {
        //clear the weather and set the time to day
        // ~~~~ note: the dimension got is hardcoded, and it is the overworld.
        Bukkit.getWorld("world")!!.setStorm(false)
        Bukkit.getWorld("world")!!.time = 1000
        Bukkit.getWorld("world")!!.isThundering = false

        for (player in players) {
            player.inventory.clear() // Clear the player's inventory
            player.saturation = 20f // Set the player's saturation to full
            player.health = 20.0 // Set the player's health to full
        }
    }
}
