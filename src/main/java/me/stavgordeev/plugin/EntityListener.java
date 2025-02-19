package me.stavgordeev.plugin;

import org.bukkit.ChatColor;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityListener implements Listener {

    @EventHandler
    public void onEntityRightClick(PlayerInteractEntityEvent event) {

        Entity entity = event.getRightClicked();

        entity.setMetadata("clicked", new org.bukkit.metadata.FixedMetadataValue(AMinecraftPlugin1.plugin, true));
        if (entity.hasMetadata("clicked")) {
            System.out.println("Player right-clicked an entity! of type " + entity.getClass().getName());
            event.getPlayer().sendMessage("You right-clicked an entity!");
            entity.setCustomName(ChatColor.RED +"Right-clicked entity");
        }

        if (entity.getType() == EntityType.COW) {
            event.getPlayer().sendMessage("You right-clicked a cow!");
            Cow cow = (Cow) event.getRightClicked();
            cow.getWorld().createExplosion(cow.getLocation(), 2.0f);

        }

    }
}
