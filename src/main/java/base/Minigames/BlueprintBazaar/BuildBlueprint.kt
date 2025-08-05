package base.Minigames.BlueprintBazaar

import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe

/** *
 * Represents a build in the Blueprint Bazaar minigame.
 * This Object holds data related to a specific build, and used to manage a build's logic within itself
 */
class BuildBlueprint(
    val region: CuboidRegion,
) {
    val materialList: MutableSet<Material> = mutableSetOf() // List of materials used in the build

    init {
        //region -- Initialize the material list with the materials used in the build
        for (block in region) {
            // get the material of the block and compare it to existing materials

            // If the block is not air and not already in the material list, add it
            val blockMaterial = BPBConst.WORLD.getBlockAt(
                block.x,
                block.y,
                block.z
            ).type

            if (blockMaterial != Material.AIR && !materialList.contains(blockMaterial)) {

                // Now, before we add the material, we want to see if this material can be crafted from a more basic material
                getRawMaterialsFromThisMaterial(blockMaterial)

            }
        }
        //endregion
    }

    fun getRawMaterialsFromThisMaterial(blockMaterial: Material,hasVisitedThisMaterial: MutableSet<Material> = mutableSetOf<Material>()) {
        println(Thread.currentThread().stackTrace.size)

        // if we have reached a basic material, return that.
        if (BPBConst.BasicMaterials.contains(blockMaterial)) {
            materialList += blockMaterial
            return
        }

        // list all recipes that yield to this material
        val recipes: Set<Recipe> = Bukkit.recipeIterator().asSequence()
            .filter { recipe -> recipe.result.type == blockMaterial }
            .toSet()


        // put into here all the ingredients that help towards making said material
        val potentialMaterialsToInclude: MutableSet<Material> = mutableSetOf()

        for (recipe in recipes) {
            potentialMaterialsToInclude += recipe.ingredients()
        }

        // if we managed to get into a material that can't be crafted from anything at all, then it can NOT be a basic material *which* is from *our* basic material list.
        if (potentialMaterialsToInclude.none()) return

        if (hasVisitedThisMaterial.containsAll(potentialMaterialsToInclude.toSet())) {
            return
        }

        //tick into the hasVisited set this material, so we won't get into infinite loops
        for (potentialMaterial in potentialMaterialsToInclude) {
            hasVisitedThisMaterial.add(potentialMaterial)
        }

        //now delve one step deeper and search for that material's ancestors...
        for (potentialMaterial in potentialMaterialsToInclude) {
            getRawMaterialsFromThisMaterial(potentialMaterial, hasVisitedThisMaterial)
        }

    }

    /**
     * Get the ingredients as Materials that this recipe includes.
     * Only checks for Crafting Recipes and Smelting Recipes.
     */
    fun Recipe.ingredients() : MutableSet<Material> {
        val setOfMaterials = mutableSetOf<Material>()

        when (this) {
            is ShapedRecipe, is ShapelessRecipe -> {
                val choices: Collection<RecipeChoice> = when (this) {
                    is ShapedRecipe -> this.choiceMap.values
                    is ShapelessRecipe -> this.choiceList
                    else -> listOf()
                }

                val matChoices: List<RecipeChoice.MaterialChoice> = choices.filterIsInstance<RecipeChoice.MaterialChoice>()

                val materials = matChoices.map {
                    materialChoice -> materialChoice.choices[0]
                }

                setOfMaterials.addAll(materials)
            }
            is FurnaceRecipe -> {
                val matChoice = this.inputChoice as RecipeChoice.MaterialChoice

                setOfMaterials.add(matChoice.choices[0])
            }
            else -> {/*nothing*/}
        }

        return setOfMaterials
    }
}

