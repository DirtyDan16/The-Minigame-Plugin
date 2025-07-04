package me.stavgordeev.plugin;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.EditSession;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class BuildLoader {
    /**
     * Load a schematic file into the world at the specified location.
     *
     * @param file        The schematic file to load.
     * @param world The world to load the schematic into.
     * @param x           The x-coordinate to paste the schematic at.
     * @param y           The y-coordinate to paste the schematic at.
     * @param z           The z-coordinate to paste the schematic at.
     */
    public static void loadSchematic(File file, World world, int x, int y, int z) {
        //fixme: make sure that pasted blocks are not affected by gravity
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName());
            return;
        }

        //Location location = new Location(world, x, y, z);

        // Load the schematic. We'll wrap it in a try-resource to make sure it's closed properly.
        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) // Get a reader for the schematic.
        {
            Clipboard clipboard = reader.read(); // Load the schematic into a clipboard.
            // Create an edit session and paste the schematic.
            EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(world))
                    .build();
            // Create an operation to paste the schematic.
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z)) // Paste location
                    .ignoreAirBlocks(false)
                    .build();


            //disableGravity(location,10); // Disable gravity for the blocks in the schematic.

            // Execute the operation.
            Operations.complete(operation);
            // Close the edit session.
            editSession.close();

            //enableGravity(location,10); // Re-enable gravity at the area where the schematic was pasted. However, the current pasted blocks will not be affected by this.

            Bukkit.getLogger().info("Successfully pasted schematic: " + file.getName());

        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.getMessage());
        } catch (WorldEditException e) {
            Bukkit.getLogger().severe("WorldEdit error while pasting schematic: " + e.getMessage());
        }
    }

    public static void loadSchematic(File schematic, World world, Location location) {
        loadSchematic(schematic,world, (int) location.x(), (int) location.y(), (int) location.z());
    }
    public static void loadSchematic(File schematic, Location location) {
        loadSchematic(schematic,location.getWorld(), (int) location.x(), (int) location.y(), (int) location.z());
    }

    public static void loadSchematicByDirection(File file, Location location, String direction) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName());
            return;
        }

        World world = location.getWorld();
        int x = location.getBlockX(); int y = location.getBlockY(); int z = location.getBlockZ();


        // get the current direction the schematic is already facing, and based on that and the desired direction, calculate by how much to rotate the schematic.
        int rotation = switch (direction.toUpperCase()) {
                case "NORTH" -> 0;
                case "EAST" -> 90;
                case "SOUTH" -> 180;
                case "WEST" -> 270;
                default -> {
                    Bukkit.getLogger().warning("Unknown direction: " + direction + ", defaulting to NORTH (0°)");
                    yield 0;
                }

        };


        // Load the schematic. We'll wrap it in a try-resource to make sure it's closed properly.
        try (FileInputStream fis = new FileInputStream(file);
             ClipboardReader reader = format.getReader(fis)) // Get a reader for the schematic.
        {
            Clipboard clipboard = reader.read(); // Load the schematic into a clipboard.
            // Create an edit session, rotate the schematic based on the 'direction' parameter and then paste the schematic.
            EditSession editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(world))
                    .build();


            // Rotate clipboard
            ClipboardHolder holder = new ClipboardHolder(clipboard);
            // Convert degrees to WorldEdit's 2D Y-axis rotation
            AffineTransform transform = new AffineTransform();
            transform = transform.rotateY(rotation);
            holder.setTransform(transform);


            // Create an operation to paste the schematic.
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z)) // Paste location
                    .ignoreAirBlocks(false)
                    .build();


            // Execute the operation.
            Operations.complete(operation);
            // Close the edit session.
            editSession.close();

            Bukkit.getLogger().info("Successfully pasted schematic: " + file.getName());

        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.getMessage());
        } catch (WorldEditException e) {
            Bukkit.getLogger().severe("WorldEdit error while pasting schematic: " + e.getMessage());
        }
    }

    public static void deleteSchematic(Location firstCorner, Location secondCorner) {
        // Get the world from the first corner.
        World world = firstCorner.getWorld();
        if (world == null) {
            Bukkit.getLogger().severe("World is null for the given location.");
            return;
        }

        // Calculate the boundaries of the area to delete.
        int minX = Math.min(firstCorner.getBlockX(), secondCorner.getBlockX());
        int maxX = Math.max(firstCorner.getBlockX(), secondCorner.getBlockX());
        int minY = Math.min(firstCorner.getBlockY(), secondCorner.getBlockY());
        int maxY = Math.max(firstCorner.getBlockY(), secondCorner.getBlockY());
        int minZ = Math.min(firstCorner.getBlockZ(), secondCorner.getBlockZ());
        int maxZ = Math.max(firstCorner.getBlockZ(), secondCorner.getBlockZ());

        // Loop through the area and set all blocks to air.
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    world.getBlockAt(x, y, z).setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    /**
     * Gets the borders of the existing build at the specified location.
     *
     * @param file     The schematic file to load.
     * @param location The location to get the borders of the existing build.
     * @return An array containing the minimum and maximum coordinates of the build.
     */
    public static int[] getBuildBorders(File file, Location location) {
        // Get the format of the schematic file.
        ClipboardFormat format = ClipboardFormats.findByFile(file);

        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName());
            return null;
        }
        //
        try (FileInputStream fis = new FileInputStream(file);
             // Get a reader for the schematic.
             ClipboardReader reader = format.getReader(fis)) {
            Clipboard clipboard = reader.read();
            // Get the dimensions of the clipboard.
            BlockVector3 dimensions = clipboard.getDimensions();
            int minX = location.getBlockX(),minY = location.getBlockY(),
                minZ = location.getBlockZ() ,maxX = minX + dimensions.getX(),
                maxY = minY + dimensions.getY() ,maxZ = minZ + dimensions.getZ();

            // Store the borders in an array.
            return new int[]{minX, maxX, minY, maxY, minZ, maxZ};
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.getMessage());
            return null;
        }
    }

    public static Location getBottomCornerOfBuild(File file, Location location) {
        // Get the borders of the build.
        int[] borders = getBuildBorders(file, location);
        if (borders == null) {
            throw new IllegalArgumentException("Borders could not be determined for the build at " + location);
        }
        // Create a new location with the minimum coordinates of the build.
        return new Location(location.getWorld(), borders[0], borders[2], borders[4]);
    }
    public static Location getTopCornerOfBuild(File file, Location location) {
        // Get the borders of the build.
        int[] borders = getBuildBorders(file, location);
        if (borders == null) {
            throw new IllegalArgumentException("Borders could not be determined for the build at " + location);
        }
        // Create a new location with the maximum coordinates of the build.
        return new Location(location.getWorld(), borders[1], borders[3], borders[5]);
    }

    /**
     * Disables gravity for all falling blocks in a certain radius around the center.
     *
     * @param center The center of the area to disable gravity in.
     * @param radius The radius of the area to disable gravity in.
     */
    private static void disableGravity(Location center, int radius) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof FallingBlock) {
                entity.setGravity(false);
                entity.setVelocity(new Vector(0, 0, 0));
            }
        }
    }

    /**
     * Enables gravity for all falling blocks in a certain radius around the center.
     *
     * @param center The center of the area to enable gravity in.
     * @param radius The radius of the area to enable gravity in.
     */
    private static void enableGravity(Location center, int radius) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof FallingBlock) {
                entity.setGravity(true);
            }
        }
    }


}
