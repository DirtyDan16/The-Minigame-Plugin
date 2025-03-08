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
import com.sk89q.worldedit.EditSessionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
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
    public void loadSchematic(File file, World world, int x, int y, int z) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName());
            return;
        }

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
}
