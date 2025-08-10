package base.utils.extensions_for_classes

import com.sk89q.worldedit.math.BlockVector3
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
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