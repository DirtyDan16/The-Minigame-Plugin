package base.minigames.blueprint_bazaar

import base.MinigamePlugin
import base.minigames.blueprint_bazaar.BPBConst.WORLD
import base.utils.extensions_for_classes.getMaterialAt
import base.utils.extensions_for_classes.minus
import base.utils.extensions_for_classes.plus
import base.utils.extensions_for_classes.toBlockVector3
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.Vector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.FurnaceRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import kotlin.math.floor

/** *
 * Represents a build in the Blueprint Bazaar minigame.
 * It tracks how complete the build is at.
 * This Object also holds data related to the build, and used to manage a build's logic within itself
 */
class BuildBlueprint(
    val game: BlueprintBazaar,
    regionOfBuildDisplayed: CuboidRegion
) : Listener {
    val materialList: MutableSet<Material> = mutableSetOf() // List of materials used in the build
    val region: CuboidRegion
    var numOfBlocksBuildDisplayedHas = 0
    var correctNumOfBlocksInRegion = 0
        set(blocks) {
            field = blocks
            completionPercentage = floor((field / numOfBlocksBuildDisplayedHas).toDouble() * 100)
            game.sender!!.sendMessage("correctNumOfBlocksInRegion = $correctNumOfBlocksInRegion")
        }

    var completionPercentage: Double = 0.0

    init {
        //region -- Initialize the material list with the materials used in the build
        for (block in regionOfBuildDisplayed) {
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

        region = CuboidRegion(
            regionOfBuildDisplayed.minimumPoint + BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET,
            regionOfBuildDisplayed.maximumPoint + BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET
        )

        // calc the total number of blocks the build to copy from has.
        regionOfBuildDisplayed.forEach { vector ->
            if (BPBConst.WORLD.getMaterialAt(vector) != Material.AIR) numOfBlocksBuildDisplayedHas++
        }
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

    @EventHandler
    fun onBlockPlaced(event: BlockPlaceEvent) {
        if (!game.isGameRunning || !game.isPlayerInGame(event.player)) return

        val block = event.block
        if (block.type == WORLD.getMaterialAt(block.toBlockVector3() - BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET)) {
            correctNumOfBlocksInRegion++
            if (completionPercentage == 100.0) {
                event.player.sendMessage("You have managed to complete the build!")
            }
        }
    }

    @EventHandler
    fun onBlockDestroyed(event: BlockBreakEvent) {
        if (!game.isGameRunning || !game.isPlayerInGame(event.player)) return

        val block = event.block
        if (block.type == WORLD.getMaterialAt(block.toBlockVector3() - BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET))
            correctNumOfBlocksInRegion--
    }
}


