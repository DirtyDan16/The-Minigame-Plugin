package base.Listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerMovementListener implements Listener {
    int jumpBoostLevel = 1;
    boolean isOnGround = true;
    Location playerLocationWhenJumped = null;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Location from = event.getFrom();
        Location to = event.getTo();
        //player.sendMessage("You moved from " + from + " to " + to);
        if (isJumping(player, from, to) && !isOnGround) {
            player.sendMessage("You jumped! Effect level: " + jumpBoostLevel);
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,Integer.MAX_VALUE, jumpBoostLevel));
            jumpBoostLevel++;
        } else {
            //System.out.println("You are not jumping");
        }
    }

    public boolean isJumping(Player player,Location from, Location to) {
        if (to.getY() > from.getY() && !player.isFlying() && from.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
            isOnGround = false;
            return true;
        } else if (to.getY() < from.getY() && !player.isFlying() && from.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
            isOnGround = true;
            return false;
        }
        return false;
    }

}
