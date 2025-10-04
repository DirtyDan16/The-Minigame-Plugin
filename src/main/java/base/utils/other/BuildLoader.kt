package base.utils.other

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.WorldEditException
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.regions.CuboidRegion
import com.sk89q.worldedit.regions.Region
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockState
import base.MinigamePlugin.Companion.world
import base.utils.additions.Direction
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.FallingBlock
import org.bukkit.util.Vector
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

object BuildLoader {
    //--region ---------------Helper methods for loading schematics -//
    fun getClipboardHolderFromFile(file: File,location: Location?): ClipboardHolder {
        val format: ClipboardFormat = ClipboardFormats.findByFile(file) ?: throw IllegalArgumentException("Unsupported schematic format: ${file.name}")

        try {
            FileInputStream(file).use { fis ->
                format.getReader(fis).use { reader ->

                    // Read the clipboard from the file.
                    val clipboard: Clipboard = reader.read()


                    // If no location is provided, return the ClipboardHolder without applying any translation.
                    if (location == null) return ClipboardHolder(clipboard)

                    // Otherwise, read the clipboard and apply the transform based on the provided location. We are going to make a new clipboard with the same blocks, but shifted to the location provided.

                    val newOrigin = BlockVector3.at(location.blockX, location.blockY, location.blockZ)
                    val offset: BlockVector3 = newOrigin.subtract(clipboard.origin)

                    val newRegion: Region = CuboidRegion(clipboard.region.world,
                        clipboard.region.minimumPoint.add(offset),
                        clipboard.region.maximumPoint.add(offset)
                    )

                    val newClipboard = BlockArrayClipboard(newRegion)

                    // Copy block data into the new clipboard at the shifted positions
                    for (pos in clipboard.region) {
                        val shiftedPos: BlockVector3 = pos.add(offset)
                        val block: BlockState = clipboard.getBlock(pos)
                        newClipboard.setBlock(shiftedPos, block)
                    }

                    // Set the new origin to the location provided
                    newClipboard.origin = newOrigin

                    return ClipboardHolder(newClipboard)
                }
            }
        } catch (e: Exception) {
        val reason = when (e) {
            is IOException -> "I/O error"
            is WorldEditException -> "WorldEdit error"
            else -> "Unexpected error"
        }
        Bukkit.getLogger().severe("Failed to load schematic (${file.name}): $reason: ${e.message}")
        throw IllegalArgumentException("Failed to load clipboard from file: ${file.name}", e)
        }
    }

    fun applyDirectionToClipboardHolder(clipboardHolder: ClipboardHolder, direction: Direction) {
        // get the current direction the schematic is already facing, and based on that and the desired direction, calculate by how much to rotate the schematic.
        val rotation = getRotationForDirection(direction)
        // Rotate clipboard - Convert degrees to WorldEdit's 2D Y-axis rotation
        clipboardHolder.transform = AffineTransform().rotateY(rotation.toDouble())
    }

    fun mirrorClipboardHolder(clipboardHolder: ClipboardHolder, facingDirection: Direction) {
        val region: Region = clipboardHolder.clipboard.region

        val wallLongestLength: Int = when (facingDirection) {
            Direction.NORTH, Direction.SOUTH -> region.width
            Direction.EAST, Direction.WEST -> region.length
        }

        val mirrorTransform = when (facingDirection) {
            Direction.NORTH, Direction.SOUTH -> AffineTransform().scale(-1.0, 1.0, 1.0)
            Direction.EAST, Direction.WEST -> AffineTransform().scale(1.0, 1.0, -1.0)
        }

        // If the wall's length is even, it doesn't have a proper center. we need to move it so when we paste it, it won't have an offset.
        if (wallLongestLength % 2 == 0) {
            val offsetCorrection = when (facingDirection) {
                Direction.NORTH -> AffineTransform().translate(1.0, 0.0, 0.0)
                Direction.SOUTH -> AffineTransform().translate(-1.0, 0.0, 0.0)
                Direction.EAST -> AffineTransform().translate(0.0, 0.0, 1.0)
                Direction.WEST -> AffineTransform().translate(0.0, 0.0, -1.0)
            }


            clipboardHolder.transform = mirrorTransform.combine(offsetCorrection).combine(clipboardHolder.transform)
        } else {
            clipboardHolder.transform = mirrorTransform.combine(clipboardHolder.transform)
        }
    }

    fun loadSchematic(clipboardHolder: ClipboardHolder) {
        // Create an edit session to paste the schematic.
        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(world))
            .build()

        // Paste the schematic.
        val operation = clipboardHolder
            .createPaste(editSession)
            .to(clipboardHolder.clipboard.origin)
            .ignoreAirBlocks(false)
            .build()

        // Execute the operation.
        Operations.complete(operation)
        // Close the edit session.
        editSession.close()

        Bukkit.getLogger().info("Successfully pasted schematic at Location: ${clipboardHolder.clipboard.origin}. Minimum Point: ${clipboardHolder.clipboard.region.minimumPoint}, Maximum Point: ${clipboardHolder.clipboard.region.maximumPoint}")
    }

    fun getRotatedRegion(clipboardHolder: ClipboardHolder, pasteLocation: Location?, direction: Direction): CuboidRegion {
        if (pasteLocation == null) {
            return clipboardHolder.clipboard.region as CuboidRegion
        }

        val transform = AffineTransform().rotateY(getRotationForDirection(direction).toDouble())
        val region = clipboardHolder.clipboard.region
        val origin = clipboardHolder.clipboard.origin

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
            return@map pasteLocation.clone().add(transformed.x, transformed.y, transformed.z)
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

    //endregion -------------------------------------------------------------

    /**
     * Modify and load a schematic via this method.
     * @param file the schematic file you want to paste it
     * @param location an optional parameter to specify where this shcem should be pasted. if nto specified, it will be pasted in the world pos in was saved at.
     *
     */
    fun loadSchematicByFileAndDirection(
        file: File,
        location: Location? = null,
        direction: Direction,
        shouldBeMirrored: Boolean = false,
    ) : Region {
        val clipboardHolder = getClipboardHolderFromFile(file,location)

        applyDirectionToClipboardHolder(clipboardHolder, direction)

        if (shouldBeMirrored)
            mirrorClipboardHolder(clipboardHolder, direction)

        // Load the schematic into the world.
        loadSchematic(clipboardHolder)

        return getRotatedRegion(clipboardHolder,location,direction)
    }

    fun loadSchematicByFile(
        file: File,
        location: Location? = null,
    ) : Region {
        val clipboardHolder = getClipboardHolderFromFile(file,location)

        // Load the schematic into the world.
        loadSchematic(clipboardHolder)

        return clipboardHolder.clipboard.region
    }

    fun loadSchematicByFileAndCoordinates(file: File, x: Int, y: Int, z: Int) {
        val location = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
        loadSchematicByFile(file, location)
    }


    /**
     * Returns the rotation in degrees for the given direction.
     */
    private fun getRotationForDirection(direction: Direction): Int {
        return when (direction) {
            Direction.SOUTH -> 0
            Direction.EAST -> 90
            Direction.NORTH -> 180
            Direction.WEST -> 270
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

    fun deleteSchematic(region: Region) {
        // Loop through the region and set all blocks to air.
        for (x in region.minimumPoint.blockX..region.maximumPoint.blockX) {
            for (y in region.minimumPoint.blockY..region.maximumPoint.blockY) {
                for (z in region.minimumPoint.blockZ..region.maximumPoint.blockZ) {
                    world.getBlockAt(x, y, z).type = Material.AIR
                }
            }
        }
    }


    @Deprecated(
        "Should not be used when also using loadSchematicByFileAndLocation() with ANY SCHEMATIC, including others. instead, if u use loadSchematicByFileAndLocation(), prefer using its region parameter in order to get the region of the schematic. "
    )
    fun getRegionFromFile(file: File, location: Location): Region? {
        val format = ClipboardFormats.findByFile(file) ?: return null

        try {
            FileInputStream(file).use { fis ->
                format.getReader(fis).use { reader ->
                    val clipboard = reader.read()
                    // the offset is the difference between the clipboard's origin and the location's block coordinates.
                    val offset = BlockVector3.at(location.blockX, location.blockY, location.blockZ).subtract(clipboard.origin)
                    val region = CuboidRegion(clipboard.region.world,
                        clipboard.region.minimumPoint.add(offset),
                        clipboard.region.maximumPoint.add(offset)
                    )
                    return region
                }
            }
        } catch (e: IOException) {
            Bukkit.getLogger().severe("Failed to load schematic: ${e.message}")
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
