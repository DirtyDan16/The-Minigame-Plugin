package me.stavgordeev.plugin

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.session.ClipboardHolder
import me.stavgordeev.plugin.MinigamePlugin.world
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.FallingBlock
import org.bukkit.util.Vector
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import kotlin.math.max
import kotlin.math.min

object BuildLoader {
    /**
     * Load a schematic file into the world at the specified location.
     *
     * @param file        The schematic file to load.
     * @param world The world to load the schematic into.
     * @param x           The x-coordinate to paste the schematic at.
     * @param y           The y-coordinate to paste the schematic at.
     * @param z           The z-coordinate to paste the schematic at.
     */
    @JvmStatic
    fun loadSchematic(file: File, world: World, x: Int, y: Int, z: Int) {
        //fixme: make sure that pasted blocks are not affected by gravity
        val format = ClipboardFormats.findByFile(file)
        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName())
            return
        }

        //Location location = new Location(world, x, y, z);

        // Load the schematic. We'll wrap it in a try-resource to make sure it's closed properly.
        try {
            FileInputStream(file).use { fis ->
                format.getReader(fis).use { reader ->
                    val clipboard = reader.read() // Load the schematic into a clipboard.
                    // Create an edit session and paste the schematic.
                    val editSession = WorldEdit.getInstance()
                        .newEditSessionBuilder()
                        .world(BukkitAdapter.adapt(world))
                        .build()
                    // Create an operation to paste the schematic.
                    val operation = ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(BlockVector3.at(x, y, z)) // Paste location
                        .ignoreAirBlocks(false)
                        .build()


                    //disableGravity(location,10); // Disable gravity for the blocks in the schematic.

                    // Execute the operation.
                    Operations.complete(operation)
                    // Close the edit session.
                    editSession.close()

                    //enableGravity(location,10); // Re-enable gravity at the area where the schematic was pasted. However, the current pasted blocks will not be affected by this.
                    Bukkit.getLogger().info("Successfully pasted schematic: " + file.getName())
                }
            }
        } catch (e: IOException) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.message)
        } catch (e: WorldEditException) {
            Bukkit.getLogger().severe("WorldEdit error while pasting schematic: " + e.message)
        }
    }

    @JvmStatic
    fun loadSchematic(schematic: File, world: World, location: Location) {
        loadSchematic(schematic, world, location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    fun loadSchematic(schematic: File, location: Location) {
        loadSchematic(schematic, location.getWorld(), location.x().toInt(), location.y().toInt(), location.z().toInt())
    }

    fun loadSchematicByDirection(file: File, location: Location, direction: String) {
        val format: ClipboardFormat = ClipboardFormats.findByFile(file) ?: run {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName())
            return
        }

        // get the current direction the schematic is already facing, and based on that and the desired direction, calculate by how much to rotate the schematic.
        val rotation = getRotationForDirection(direction)
        val world = location.getWorld()
        val x = location.blockX
        val y = location.blockY
        val z = location.blockZ


        // Load the schematic. We'll wrap it in a try-resource to make sure it's closed properly.
        try {
            format.getReader(FileInputStream(file)).use { reader ->
                // have a ClipboardHolder to hold the clipboard to apply the rotation.
                val holder = ClipboardHolder(reader.read())


                // Create an edit session to paste the schematic.
                val editSession = WorldEdit.getInstance()
                    .newEditSessionBuilder()
                    .world(BukkitAdapter.adapt(world))
                    .build()

                // Rotate clipboard - Convert degrees to WorldEdit's 2D Y-axis rotation
                holder.setTransform(AffineTransform().rotateY(rotation.toDouble()))

                // paste the schematic with the applied rotation that 'holder' has.
                val operation = holder
                    .createPaste(editSession)
                    .to(BlockVector3.at(x, y, z)) // Paste location
                    .ignoreAirBlocks(false)
                    .build()

                // Execute the operation.
                Operations.complete(operation)
                // Close the edit session.
                editSession.close()
                Bukkit.getLogger().info("Successfully pasted schematic: " + file.getName())
            }
        } catch (e: IOException) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.message)
        } catch (e: WorldEditException) {
            Bukkit.getLogger().severe("WorldEdit error while pasting schematic: " + e.message)
        }
    }

    fun getRotatedRegion(wallFile: File, pasteLocation: Location, direction: String): CuboidRegion {
        val format = ClipboardFormats.findByFile(wallFile) ?: error("Unsupported format")
        FileInputStream(wallFile).use { fis ->
            format.getReader(fis).use { reader ->
                val clipboard = reader.read()
                val rotation = getRotationForDirection(direction).toDouble()
                val transform = AffineTransform().rotateY(rotation)
                val region = clipboard.region
                val origin = clipboard.origin

                // Transform all corners of the region
                val points: List<Location> = listOf(
                    region.minimumPoint,
                    region.maximumPoint
                ).map { point ->
                    // Calculate the relative position from the origin
                    val rel: BlockVector3 = point.subtract(origin)
                    // Apply the rotation transform to the relative position
                    val transformed: Vector3 = transform.apply(rel.toVector3())
                    // Convert back to a Location relative to the paste location
                    pasteLocation.clone().add(transformed.x, transformed.y, transformed.z)
                }

                // find the minimum and maximum coordinates from the transformed points
                val minX = points.minOf { it.x }
                val minY = points.minOf { it.y }
                val minZ = points.minOf { it.z }
                val maxX = points.maxOf { it.x }
                val maxY = points.maxOf { it.y }
                val maxZ = points.maxOf { it.z }

                val min = BlockVector3.at(minX, minY, minZ)
                val max = BlockVector3.at(maxX, maxY, maxZ)
                return CuboidRegion(min, max)
            }
        }
    }


    /**
     * Returns the rotation in degrees for the given direction.
     */
    private fun getRotationForDirection(direction: String): Int {
        return when (direction.uppercase(Locale.getDefault())) {
            "SOUTH" -> 0
            "EAST" -> 90
            "NORTH" -> 180
            "WEST" -> 270
            else -> {
                Bukkit.getLogger().warning("Unknown direction: " + direction + ", defaulting to NORTH (0°)")
                0
            }
        }
    }

    fun deleteSchematic(firstCorner: BlockVector3, secondCorner: BlockVector3) {
        // Calculate the boundaries of the area to delete.
        val minX = min(firstCorner.blockX, secondCorner.blockX)
        val maxX = max(firstCorner.blockX, secondCorner.blockX)
        val minY = min(firstCorner.blockY, secondCorner.blockY)
        val maxY = max(firstCorner.blockY, secondCorner.blockY)
        val minZ = min(firstCorner.blockZ, secondCorner.blockZ)
        val maxZ = max(firstCorner.blockZ, secondCorner.blockZ)

        // Loop through the area and set all blocks to air.
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    world.getBlockAt(x, y, z).type = Material.AIR
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
    @JvmStatic
    fun getBuildBorders(file: File, location: Location): IntArray? {
        // Get the format of the schematic file.
        val format = ClipboardFormats.findByFile(file)

        if (format == null) {
            Bukkit.getLogger().warning("Unsupported schematic format: " + file.getName())
            return null
        }
        //
        try {
            FileInputStream(file).use { fis ->
                format.getReader(fis).use { reader ->
                    val clipboard = reader.read()
                    // Get the dimensions of the clipboard.
                    val dimensions = clipboard.dimensions
                    val minX = location.blockX
                    val minY = location.blockY
                    val minZ = location.blockZ
                    val maxX = minX + dimensions.x
                    val maxY = minY + dimensions.y
                    val maxZ = minZ + dimensions.z

                    // Store the borders in an array.
                    return intArrayOf(minX, maxX, minY, maxY, minZ, maxZ)
                }
            }
        } catch (e: IOException) {
            Bukkit.getLogger().severe("Failed to load schematic: " + e.message)
            return null
        }
    }

    /**
     * Disables gravity for all falling blocks in a certain radius around the center.
     *
     * @param center The center of the area to disable gravity in.
     * @param radius The radius of the area to disable gravity in.
     */
    private fun disableGravity(center: Location, radius: Int) {
        for (entity in center.getWorld()
            .getNearbyEntities(center, radius.toDouble(), radius.toDouble(), radius.toDouble())) {
            if (entity is FallingBlock) {
                entity.setGravity(false)
                entity.velocity = Vector(0, 0, 0)
            }
        }
    }

    /**
     * Enables gravity for all falling blocks in a certain radius around the center.
     *
     * @param center The center of the area to enable gravity in.
     * @param radius The radius of the area to enable gravity in.
     */
    private fun enableGravity(center: Location, radius: Int) {
        for (entity in center.getWorld()
            .getNearbyEntities(center, radius.toDouble(), radius.toDouble(), radius.toDouble())) {
            if (entity is FallingBlock) {
                entity.setGravity(true)
            }
        }
    }
}
