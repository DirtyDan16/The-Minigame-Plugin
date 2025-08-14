package base.utils.extensions_for_classes

import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BlockVector
import kotlin.collections.iterator
import kotlin.random.Random

fun <Type> Random.getNextWeighted(weights: Map<Type, Int>): Type {
    val totalWeight = weights.values.sum()
    var randomValue = nextInt(totalWeight)

    for ((item, weight) in weights) {
        if (randomValue < weight) {
            return item
        }
        randomValue -= weight
    }

    throw IllegalStateException("Should not reach here, weights are not set up correctly.")
}

fun World.getBlockAt(vector: BlockVector3): Block {
    return this.getBlockAt(vector.x,vector.y, vector.z)
}

fun World.getMaterialAt(vector: BlockVector3): Material {
    return this.getBlockAt(vector.x,vector.y, vector.z).type
}

operator fun BlockVector3.plus(other: BlockVector3): BlockVector3 {
    return BlockVector3.at(
        this.x + other.x,
        this.y + other.y,
        this.z + other.z
    )
}

operator fun BlockVector3.minus(other: BlockVector3): BlockVector3 {
    return BlockVector3.at(
        this.x - other.x,
        this.y - other.y,
        this.z - other.z
    )
}

fun Block.toBlockVector3() : BlockVector3 {
    return BlockVector3.at(this.x,this.y,this.z)
}

fun Player.clearInvAndGiveItems(
    materialList: Collection<Material>,
    sizeForEachItemSlot: Int = 1
) {
    this.inventory.clear()

    if (materialList.size >= this.inventory.size)
        this.sendMessage("The amount of items that were attempted to give to u exceed the space u have. Giving only what could have been given")

    materialList.forEachIndexed { index, material ->

        //if we have filled in the entire inventory of the player, stop the action.
        if (index >= this.inventory.size) return

        this.inventory.setItem(index, ItemStack(material,sizeForEachItemSlot))
    }
}