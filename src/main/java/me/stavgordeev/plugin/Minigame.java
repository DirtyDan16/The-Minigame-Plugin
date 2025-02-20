// src/main/java/me/stavgordeev/plugin/Minigame.java
package me.stavgordeev.plugin;

import me.stavgordeev.plugin.Constants.MGConst;
import org.bukkit.*;
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


    //-Game Modifiers that change as the game progresses to scale difficulty-//
    private int upperBound__startingIntervalForChangingFloor = MGConst.FloorLogic.ChangingFloor.UPPER_BOUND_START_INTERVAL;
    private int lowerBound__startingIntervalForChangingFloor = MGConst.FloorLogic.ChangingFloor.LOWER_BOUND_START_INTERVAL;
    private int upperBound__stopChangingFloorInterval = MGConst.FloorLogic.ChangingFloor.UPPER_BOUND_STOP_INTERVAL;
    private int lowerBound__stopChangingFloorInterval = MGConst.FloorLogic.ChangingFloor.LOWER_BOUND_STOP_INTERVAL;
    //----------------------------------------------------------------------//

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

        //----- List Of Actions To Be Done When The Game Starts -----//

        nukeArea(MGConst.GAME_START_LOCATION, 50); // Clear the area before starting the game
        // Teleport the player to the starting location 8 blocks above the ground
        player.teleport(MGConst.GAME_START_LOCATION.clone().add(0, 8, 0));
        player.sendMessage("Minigame started!");

        initFloor(7, 7, Material.GLASS);

        MGConst.WORLD.setTime(6000); // Set the time to day
        MGConst.WORLD.setStorm(false); // Disable rain
        MGConst.WORLD.setThundering(false); // Disable thunder

        player.setGameMode(GameMode.ADVENTURE); // Set the player's game mode to adventure

        //----------------------------------------------------------------//



        // Wait a lil before starting the floor mechanics.
        new BukkitRunnable() {
            @Override
            public void run() {
                preppingForAFloorCycle(MGConst.GAME_START_LOCATION);

                // Wait a lil before removing the initial floor.
                new BukkitRunnable(){
                    @Override
                    public void run() {
                        initFloor(7, 7, Material.AIR);
                    }
                }.runTaskLater(plugin, 60);

                cancel();
            }
        }.runTaskLater(plugin, 40);
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

        nukeArea(MGConst.GAME_START_LOCATION, 50);

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

    private void preppingForAFloorCycle(Location referenceLocation) {
        if (!isGameRunning || isGamePaused) {
            return;
        }

        Bukkit.broadcastMessage("prepping for change floor");

        Random radiusRandomizer = new Random(),intervalRandomizer = new Random();

        // Randomize the radius of the floor and the interval between floor changes.
        int xRad = radiusRandomizer.nextInt(MGConst.FloorLogic.FloorSize.LOWER_BOUND_X_RADIUS, MGConst.FloorLogic.FloorSize.UPPER_BOUND_X_RADIUS+1);
        int zRad = radiusRandomizer.nextInt(MGConst.FloorLogic.FloorSize.LOWER_BOUND_Z_RADIUS, MGConst.FloorLogic.FloorSize.UPPER_BOUND_Z_RADIUS+1);
        int interval = intervalRandomizer.nextInt(lowerBound__startingIntervalForChangingFloor, upperBound__startingIntervalForChangingFloor+1);
        int stopInterval = intervalRandomizer.nextInt(lowerBound__stopChangingFloorInterval, upperBound__stopChangingFloorInterval+1);

        // Randomize the center of the new floor. For the z and x coordinates, the min value represents the min distance compared to the last floor reference. For the y coordinate, there is a min and max value.
        Random newCenterCoordinatesRandomizer = new Random();
        int randomisedXDiff = newCenterCoordinatesRandomizer.nextInt(MGConst.FloorLogic.NewFloorSpawnBoundaries.LOWER_BOUND_X_CENTER, MGConst.FloorLogic.NewFloorSpawnBoundaries.UPPER_BOUND_X_CENTER+1);
        randomisedXDiff = randomlyChangeSign(randomisedXDiff);
        int randomisedZDiff = newCenterCoordinatesRandomizer.nextInt(MGConst.FloorLogic.NewFloorSpawnBoundaries.LOWER_BOUND_Z_CENTER, MGConst.FloorLogic.NewFloorSpawnBoundaries.UPPER_BOUND_Z_CENTER+1);
        randomisedZDiff = randomlyChangeSign(randomisedZDiff);
        int randomisedYDiff = newCenterCoordinatesRandomizer.nextInt(MGConst.FloorLogic.NewFloorSpawnBoundaries.LOWER_BOUND_Y_CENTER, MGConst.FloorLogic.NewFloorSpawnBoundaries.UPPER_BOUND_Y_CENTER+1);

        // center of the new floor. the new center is tied to the reference location.
        Location center = referenceLocation.clone().add(new Location(MGConst.WORLD,randomisedXDiff,randomisedYDiff,randomisedZDiff));
        Bukkit.broadcastMessage(ChatColor.BLUE + "Diff in centers: " + randomisedXDiff + " " + randomisedYDiff + " " + randomisedZDiff);
        Bukkit.broadcastMessage(ChatColor.BLUE + "new floor center: " + formatLocation(center));

        // Start the floor change logic cycle.
        changeFloor(center,xRad, zRad);
        activateChangeFloorTimerWithGrowingFrequency(center,interval,stopInterval, xRad, zRad);

    }

    private int randomlyChangeSign(int value) {
        Random random = new Random();
        boolean isFlipped = random.nextBoolean();
        if (isFlipped) value = -value;

        return value;
    }

    /**
     * Recursively calls the changeFloor method with a decreasing interval. The interval is decremented by 1 each time the method is called.
     * @param interval
     * @param stopInterval
     * @param xRad
     * @param zRad
     */
    private void activateChangeFloorTimerWithGrowingFrequency(Location center,int interval,int stopInterval,int xRad, int zRad) {
        if (!isGameRunning || isGamePaused) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {

                if (interval == stopInterval || interval == MGConst.MIN_INTERVAL) {
                    Bukkit.broadcastMessage("recursion stopped. interval is " + interval);

                    chooseFloorBlockType(center,xRad,zRad);

                    cancel();
                    return;
                }

                changeFloor(center,xRad, zRad);

                // Recursively call the method with the new interval
                activateChangeFloorTimerWithGrowingFrequency( center,interval-1,stopInterval,xRad,zRad);
            }
        }.runTaskLater(plugin, interval);
    }

    public void initFloor(int xLengthRad, int zLengthRad,Material material) {
        if (!isGameRunning || isGamePaused) {
            return;
        }
        // Initialize the floor under the player to stone 1 block at a time. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1.
        Location center = MGConst.GAME_START_LOCATION.clone().add(new Location(MGConst.WORLD,0,5,0));
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                Location selectedLocation = new Location(MGConst.WORLD, center.getX() + x, center.getY(), center.getZ() + z);
                selectedLocation.getBlock().setType(material);
            }
        }

        Bukkit.broadcastMessage("floor initialized");
    }

    public void changeFloor(Location center, int xLengthRad, int zLengthRad) {
        Random blockTypeRandomizer = new Random();
        //Bukkit.broadcastMessage("floor changed");

        Material[] blockTypes = MGConst.FloorLogic.DEFAULT_FLOOR_BLOCK_TYPES;

        // Change the floor under the player to random materials. The floor is a rectangle with side lengths 2*xLengthRad+1 and 2*zLengthRad+1. goes over 1 block at a time.
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                int material = blockTypeRandomizer.nextInt(blockTypes.length);
                Location selectedLocation = new Location(MGConst.WORLD, center.getX() + x, center.getY(), center.getZ() + z);
                selectedLocation.getBlock().setType(blockTypes[material]);
            }
        }
    }

    public void removeFloorExceptForChosenMaterial(Location center, int xLengthRad, int zLengthRad, Material materialToKeep) {
        Bukkit.broadcastMessage("floor removal");

        // Take the current floor and remove all the materials except for the materialToKeep. go through 1 block at a time. the size of the floor is 2*xLengthRad+1 and 2*zLengthRad+1.
        for (int x = -xLengthRad; x <= xLengthRad; x++) {
            for (int z = -zLengthRad; z <= zLengthRad; z++) {
                Location selectedLocation = new Location(MGConst.WORLD, center.getX() + x, center.getY(), center.getZ() + z);

                // Only change the block if it is not the material to keep
                if (selectedLocation.getBlock().getType() != materialToKeep) selectedLocation.getBlock().setType(Material.AIR);
            }
        }

        // At this stage, a new floor is set elsewhere. The player will have a limited time to go from the old floor to the new floor. the timer and the logic
        // can be seen in the bukkit runnable below.
        preppingForAFloorCycle(center);

        // Remove the remaining parts of the floor after a certain amount of time. This is the time the player has to go from the old floor to the new floor.
        //fixme: if the new floor is too close to the old one, this runnable will remove blocks from the new floor that their material is the same
        // as the old chosen material from , if they are in the bounds of the old floor.
        new BukkitRunnable() {
            @Override
            public void run() {
                // Go over the material that isn't deleted and remove it as well.
                for (int x = -xLengthRad; x <= xLengthRad; x++) {
                    for (int z = -zLengthRad; z <= zLengthRad; z++) {
                        Location selectedLocation = new Location(MGConst.WORLD, center.getX() + x, center.getY(), center.getZ() + z);

                        // Remove the selected Material
                        if (selectedLocation.getBlock().getType() == materialToKeep) selectedLocation.getBlock().setType(Material.AIR);
                    }
                }
                cancel();
            }
        }.runTaskLater(plugin, MGConst.FloorLogic.DURATION_OF_STAYING_IN_A_FLOOR_WITH_ONLY_CHOSEN_MATERIAL);
    }

    private void chooseFloorBlockType(Location center,int xRad, int zRad) {
        Random blockTypeRandomizer = new Random();
        Material[] floorBlockTypes = MGConst.FloorLogic.DEFAULT_FLOOR_BLOCK_TYPES;

        Material material = floorBlockTypes[blockTypeRandomizer.nextInt(floorBlockTypes.length)]; // get a random material from the list of floor block types
        Bukkit.broadcastMessage(ChatColor.RED + "floor type chosen: " + material.toString());

        // Give the material to all players in their 5th hotbar slot
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().setItem(4, new ItemStack(material));
        }

        //TODO: as the game progresses, the time to remove the floor should be shortened.

        // Remove all the floor except for the chosen material. the time given is the time to remove the floor. overtime this will be shortened as the game progresses and gets more difficult.
        new BukkitRunnable() {
            @Override
            public void run() {
                removeFloorExceptForChosenMaterial(center,xRad, zRad, material);

                //remove the material from the players' hotbar, so it won't confuse them.
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.getInventory().clear(4);
                }

                cancel();
            }
        }.runTaskLater(plugin, MGConst.FloorLogic.DELAY_TO_SELECT_A_FLOOR_MATERIAL);
    }

    private static String formatLocation(@NotNull Location location) {
        return location.getWorld().getName() + ". (" + location.getX() + "," + location.getY() + "," + location.getZ()+")";
    }
}