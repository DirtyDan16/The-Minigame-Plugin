// src/main/java/me/stavgordeev/plugin/Minigame.java
package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Constants.MinigameConstants;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public class Minigame {
    private final Plugin plugin;
    private volatile boolean isGameRunning;
    private volatile boolean isGamePaused;
    private Player thePlayer;

    public Minigame(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(Player player) throws InterruptedException {
        if (isGameRunning) {
            player.sendMessage("Minigame is already running!");
            return;
        }
        isGameRunning = true;
        thePlayer = player;

        player.teleport(MinigameConstants.GAME_START_LOCATION.clone().add(0, 8, 0));
        player.sendMessage("Minigame started!");

        initFloor(5, 5, Material.STONE);


        new BukkitRunnable() {
            @Override
            public void run() {
                preppingForChangeFloor();
                initFloor(5, 5,Material.AIR);
                cancel();
            }
        }.runTaskLater(plugin, 30);
    }

    public void pauseGame(Player player) {
        if (!isGameRunning) {
            player.sendMessage("Minigame is not running!");
            return;
        } else if (isGamePaused) {
            player.sendMessage("Minigame is already paused!");
            return;
        }

        isGamePaused = true;
        player.sendMessage("Minigame stopped!");
        // Add more actions here
    }

    public void resumeGame(Player player) {
        if (!isGameRunning) {
            player.sendMessage("Minigame is not running!");
            return;
        } else if (!isGamePaused) {
            player.sendMessage("Minigame is not paused!");
            return;
        }

        isGamePaused = false;
        player.sendMessage("Minigame resumed!");
        // Add more actions here
    }

    public void endGame(Player player) {
        if (!isGameRunning) {
            player.sendMessage("Minigame is not running!");
            return;
        }

        isGameRunning = false;
        isGamePaused = false;
        thePlayer = null;
        player.sendMessage("Game ended!");

        nukeArea(MinigameConstants.GAME_START_LOCATION, 50);

        //player.teleport(MinigameConstants.GAME_START_LOCATION.clone().add(0, -70, 0));
    }

    public void nukeArea(Location center, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location currentLocation = center.clone().add(x, y, z);
                    currentLocation.getBlock().setType(Material.AIR);
                }
            }
        }
    }

    private void preppingForChangeFloor() {
        if (!isGameRunning || isGamePaused) {
            return;
        }

        Bukkit.broadcastMessage("prepping for change floor");

        Random radiusRandomizer = new Random(),intervalRandomizer = new Random();
        int xRad = radiusRandomizer.nextInt(MinigameConstants.LOWER_BOUND_CHANGING_FLOOR_X_RADIUS,MinigameConstants.UPPER_BOUND_CHANGING_FLOOR_X_RADIUS);
        int zRad = radiusRandomizer.nextInt(MinigameConstants.LOWER_BOUND_CHANGING_FLOOR_Z_RADIUS,MinigameConstants.UPPER_BOUND_CHANGING_FLOOR_Z_RADIUS);
        int interval = intervalRandomizer.nextInt(MinigameConstants.LOWER_BOUND_CHANGING_FLOOR_INTERVAL,MinigameConstants.UPPER_BOUND_CHANGING_FLOOR_INTERVAL);
        int stopInterval = intervalRandomizer.nextInt(MinigameConstants.LOWER_BOUND_STOP_CHANGING_FLOOR_INTERVAL,MinigameConstants.UPPER_BOUND_STOP_CHANGING_FLOOR_INTERVAL);


        changeFloor(xRad, zRad);
        activateChangeFloorTimerWithGrowingFrequency(interval,stopInterval, xRad, zRad);

    }

    /**
     * Recursively calls the changeFloor method with a decreasing interval. The interval is decremented by 1 each time the method is called.
     * @param interval
     * @param stopInterval
     * @param xRad
     * @param zRad
     */
    private void activateChangeFloorTimerWithGrowingFrequency(int interval,int stopInterval,int xRad, int zRad) {
        if (!isGameRunning || isGamePaused) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                if (interval == stopInterval || interval == MinigameConstants.MIN_INTERVAL) {
                    Bukkit.broadcastMessage("recursion stopped. interval is " + interval);

                    chooseFloorBlockType(xRad,zRad);

                    cancel();
                    return;
                }

                changeFloor(xRad, zRad);

                // Recursively call the method with the new interval
                activateChangeFloorTimerWithGrowingFrequency( interval-1,stopInterval,xRad,zRad);
            }
        }.runTaskLater(plugin, interval);
    }

    public void initFloor(int xLengthRad, int zLengthRad,Material material) {
        if (!isGameRunning || isGamePaused) {
            return;
        }
        // Initialize the floor under the player to stone 1 block at a time. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1.
        Location center = MinigameConstants.GAME_START_LOCATION.clone().add(new Location(MinigameConstants.WORLD,0,5,0));
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                Location selectedLocation = new Location(MinigameConstants.WORLD, center.getX() + x, center.getY(), center.getZ() + z);
                selectedLocation.getBlock().setType(material);
            }
        }

        Bukkit.broadcastMessage("floor initialized");
    }

    public void changeFloor(int xLengthRad, int zLengthRad) {
        Random blockTypeRandomizer = new Random();
        Bukkit.broadcastMessage("floor changed");

        Location center = MinigameConstants.GAME_START_LOCATION;
        Material[] blockTypes = MinigameConstants.DEFAULT_FLOOR_BLOCK_TYPES;

        // Change the floor under the player to random materials. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1. goes over 1 block at a time.
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                int material = blockTypeRandomizer.nextInt(blockTypes.length);
                Location selectedLocation = new Location(MinigameConstants.WORLD, center.getX() + x, center.getY(), center.getZ() + z);
                selectedLocation.getBlock().setType(blockTypes[material]);
            }
        }
    }

    public void removeFloorExceptForChosenMaterial(int xLengthRad, int zLengthRad, Material materialToKeep) {
        Bukkit.broadcastMessage("floor removal");

        Location center = MinigameConstants.GAME_START_LOCATION;

        // Take the current floor and remove all the materials except for the materialToKeep. go through 1 block at a time. the size of the floor is 2*xLengthRad+1 and 2*zLengthRad+1.
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                Location selectedLocation = new Location(MinigameConstants.WORLD, center.getX() + x, center.getY(), center.getZ() + z);

                // Only change the block if it is not the material to keep
                if (selectedLocation.getBlock().getType() != materialToKeep) selectedLocation.getBlock().setType(Material.AIR);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Go over the material that isn't deleted and remove it as well.
                for (int x = -xLengthRad; x <= xLengthRad; x++) {
                    for (int z = -zLengthRad; z <= zLengthRad; z++) {
                        Location selectedLocation = new Location(MinigameConstants.WORLD, center.getX() + x, center.getY(), center.getZ() + z);

                        // Remove the selected Material
                        selectedLocation.getBlock().setType(Material.AIR);
                    }
                }

                preppingForChangeFloor();
                cancel();
            }
        }.runTaskLater(plugin, MinigameConstants.DURATION_OF_STAYING_IN_A_FLOOR_WITH_ONLY_CHOSEN_MATERIAL);
    }

    private void chooseFloorBlockType(int xRad, int zRad) {
        Random blockTypeRandomizer = new Random();
        Material[] floorBlockTypes = MinigameConstants.DEFAULT_FLOOR_BLOCK_TYPES;

        Material material = floorBlockTypes[blockTypeRandomizer.nextInt(floorBlockTypes.length)]; // get a random material from the list of floor block types
        Bukkit.broadcastMessage(ChatColor.RED + "floor type chosen: " + material.toString());

        // Give the material to all players in their 5th hotbar slot
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().setItem(4, new ItemStack(material));
        }

        // Remove all the floor except for the chosen material. the time given is the time to remove the floor. overtime this will be shortened as the game progresses and gets more difficult.
        new BukkitRunnable() {
            @Override
            public void run() {
                removeFloorExceptForChosenMaterial(xRad, zRad, material);
                cancel();
            }
        }.runTaskLater(plugin, MinigameConstants.INITIAL_DELAY_TO_SELECT_A_FLOOR_MATERIAL);
    }

    private static String formatLocation(@NotNull Location location) {
        return location.getWorld().getName() + ". (" + location.getX() + "," + location.getY() + "," + location.getZ()+")";
    }
}