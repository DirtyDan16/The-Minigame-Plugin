package base.minigames.maze_hunt

import base.annotations.CalledByCommand
import base.minigames.MinigameSkeleton
import org.bukkit.Location
import org.bukkit.entity.Player

class MazeHunt : MinigameSkeleton() {
    @CalledByCommand
    override fun start(sender: Player) {
        super.start(sender)
    }

    @CalledByCommand
    override fun startFastMode(player: Player) {
        super.startFastMode(player)
    }

    @CalledByCommand
    override fun pauseGame() {
        super.pauseGame()
    }

    @CalledByCommand
    override fun resumeGame() {
        super.resumeGame()
    }

    @CalledByCommand
    override fun endGame() {
        super.endGame()
    }

    override fun nukeArea(center: Location, radius: Int) {
        super.nukeArea(center, radius)
    }

    override fun prepareGameSetting() {
        super.prepareGameSetting()
    }

    override fun prepareArea() {
        TODO("Not yet implemented")
    }


}