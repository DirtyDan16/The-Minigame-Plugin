package base.utils.extensions_for_classes

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.inventory.meta.Damageable
import java.util.WeakHashMap

// Backing store for the extension property
val duraRanges = WeakHashMap<ItemStack, IntRange>()

// Extension property
var ItemStack.duraRange: IntRange?
    get() = duraRanges[this]
    set(value) {
        if (value != null) {
            duraRanges[this] = value
        } else {
            duraRanges.remove(this)
        }
    }

fun ItemStack.hasDurability(): Boolean = type.maxDurability > 0

var ItemStack.remainingDurability: Int?
    get() {
        val meta = this.itemMeta as Damageable
        return type.maxDurability - meta.damage
    }
    set(value) {
        if (value == null) return

        val meta = this.itemMeta as Damageable
        meta.damage = (type.maxDurability - value).coerceIn(0, type.maxDurability.toInt())
        this.itemMeta = meta
    }

infix fun ItemStack.modifyDuraBy(dura: Int) {
    this.remainingDurability = this.remainingDurability?.plus(dura)
}

fun PlayerInventory.fullyContains(item: Material): Boolean {
    return getItemStackByMaterial(item) != null
}

/**
 * Checks and returns the ItemStack if the suggested [Material] exists in the inventory, *including* the armor slots of the player.
 * if it doesn't, the method returns null
 */
fun PlayerInventory.getItemStackByMaterial(material: Material) : ItemStack?{
    // Check main + offhand
    this.forEach {
        if (it != null && it.type == material) {
            return it
        }
    }

    // Check armor separately
    this.armorContents.forEach { armorItem ->
        if (armorItem != null && armorItem.type == material) {
            return armorItem
        }
    }

    return null
}
