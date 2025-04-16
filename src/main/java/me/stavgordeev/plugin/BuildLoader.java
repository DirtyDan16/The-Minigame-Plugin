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

        Location location = new Location(world, x, y, z);

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

    /**
     * Gets the borders of the existing build at the specified location.
     *
     * @param file     The schematic file to load.
     * @param location The location to get the borders of the existing build.
     * @return An array containing the minimum and maximum coordinates of the build.
     */
    public static int @Nullable [] getBuildBorders(File file, Location location) {
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
