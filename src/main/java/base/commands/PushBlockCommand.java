package base.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Objects;

public class PushBlockCommand implements CommandExecutor, TabExecutor {
    private final Plugin plugin;

    public PushBlockCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 6) {
            sender.sendMessage("Block pushing details are not complete. Please provide the x, y, z,the amount, the direction to push the block and the interval.");
            return true;
        } if (Objects.equals(args[3], "0")) {
            sender.sendMessage("Invalid amount. Please provide a valid amount to push the block.");
            return true;
        }
//        if (args[4] != "north" && args[4] != "south" && args[4] != "east" && args[4] != "west") {
//            sender.sendMessage("Invalid direction. Please provide a valid direction to push the block.");
//            return true;
//        }
        if (Double.parseDouble(args[5]) <= 0) {
            sender.sendMessage("Invalid interval. Please provide a valid interval to push the block.");
            return true;
        }

        //------------ getting the block location, amount, direction and interval from the command arguments----------------------
        Location blockLocation = new Location(sender.getServer().getWorlds().getFirst(), Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
        int amount = Integer.parseInt(args[3]);
        String direction = args[4];
        int interval = Integer.parseInt(args[5]);
        //--------------------------------------------------------------------------------------------------------------------------

        //if the quantity is negative, we need to change the direction
        if (amount < 0) {
            amount = Math.abs(amount);
            direction = direction.equals("north") ? "south" : direction.equals("south") ? "north" : direction.equals("east") ? "west" : "east";
        }

        // pushing the block every specified interval until the amount is reached 1 block at a time
        // the if statement is to prevent the block from being pushed more than the specified amount
        int finalAmount = amount;
        String finalDirection = direction;
        sender.sendMessage("Pushing block at location "+ formatLocation(blockLocation)+" , " + finalAmount + " blocks " + finalDirection + " every " + interval/20 + " seconds.");
        sender.sendMessage("Material: "+ blockLocation.getBlock().getType());
        new BukkitRunnable() {
            int counter = 0;

            @Override
            public void run() {
                sender.sendMessage("iteration: " + counter+"/"+finalAmount+".  Location: "+ formatLocation(blockLocation));
                if (counter >= finalAmount) {
                    this.cancel();
                    return;
                }

                pushBlock(finalDirection, blockLocation);

                counter++;
            }
        }.runTaskTimer(plugin, 0, interval);

        return true;

    }


    //TODO: Fix the block pushing logic not saving block direction
    private void pushBlock(String directionToMove, Location blockLocation) {
    // Get the block at the current location
    Material blockType = blockLocation.getBlock().getType();
    Vector blockDirection = blockLocation.getDirection();

    // Debug: Print the current block location and type
    System.out.println("Current Block Location: " + formatLocation(blockLocation));
    System.out.println("Current Block Type: " + blockType);

    // Set the current block to air
    blockLocation.getBlock().setType(Material.AIR);

    // Update the block location based on the direction
    switch (directionToMove) {
        case "north":
            blockLocation.add(0, 0, -1);
            break;
        case "south":
            blockLocation.add(0, 0, 1);
            break;
        case "east":
            blockLocation.add(1, 0, 0);
            break;
        case "west":
            blockLocation.add(-1, 0, 0);
            break;
    }

    // Debug: Print the new block location
    System.out.println("New Block Location: " + formatLocation(blockLocation));

    // Set the new block location to the original block type
    blockLocation.getBlock().setType(blockType);

    // Apply the copied direction to the new block location
    blockLocation.setDirection(blockDirection);

    // Debug: Print the final block location and type
    System.out.println("Final Block Location: " + formatLocation(blockLocation));
    System.out.println("Final Block Type: " + blockLocation.getBlock().getType());
}


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length == 5) {
        return List.of("north", "south", "east", "west");
    } else if (args.length == 6) {
        return List.of("1", "2", "3", "4", "5");
    }
    return List.of();
    }

    private static String formatLocation(@NotNull Location location) {
        return location.getWorld().getName() + ". (" + location.getX() + "," + location.getY() + "," + location.getZ()+")";
    }
}
