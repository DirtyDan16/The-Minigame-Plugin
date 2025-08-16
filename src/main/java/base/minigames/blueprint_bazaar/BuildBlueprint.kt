package base.minigames.blueprint_bazaar

import base.minigames.blueprint_bazaar.BPBConst.WORLD
import base.utils.extensions_for_classes.getMaterialAt
import base.utils.extensions_for_classes.minus
import base.utils.extensions_for_classes.plus
import base.utils.extensions_for_classes.toBlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.*
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
            val blockMaterial = WORLD.getBlockAt(
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
            if (WORLD.getMaterialAt(vector) != Material.AIR) numOfBlocksBuildDisplayedHas++
        }
    }

    fun getRawMaterialsFromThisMaterial(blockMaterial: Material,hasVisitedThisMaterial: MutableSet<Material> = mutableSetOf()) {
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

        // Not allow block placing outside the designated plot.
        if (block.toBlockVector3() !in region) {
            event.isCancelled = true
            return
        }

        if (block.type == WORLD.getMaterialAt(block.toBlockVector3() - BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET)) {
            correctNumOfBlocksInRegion++
            if (completionPercentage == 100.0) {
                event.player.sendMessage("You have managed to complete the build!")
                game.completeBuild(this)
            }
        }
    }

    @EventHandler
    fun onBlockTouched(event: BlockDamageEvent) {
        if (!game.isGameRunning || !game.isPlayerInGame(event.player)) return

        val block = event.block

        // Not allow block breaking outside the designated plot.
        if (block.toBlockVector3() !in region) {
            event.isCancelled = true
            return
        }

        // check if the block was related to progression of the build.
        if (block.type == WORLD.getMaterialAt(block.toBlockVector3() - BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET))
            correctNumOfBlocksInRegion--

        block.world.dropItemNaturally(block.location, ItemStack(block.type))
        block.type = Material.AIR // remove block manually
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = true
    }

}


