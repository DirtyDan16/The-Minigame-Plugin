package base.minigames.blueprint_bazaar

import base.MinigamePlugin.Companion.plugin
import base.minigames.blueprint_bazaar.BPBConst.WORLD
import base.utils.extensions_for_classes.getBlockAt
import base.utils.extensions_for_classes.getMaterialAt
import base.utils.extensions_for_classes.minus
import base.utils.extensions_for_classes.plus
import base.utils.extensions_for_classes.removeItemsInRegion
import base.utils.extensions_for_classes.toBlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.Bisected
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.*
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.time.Duration
import kotlin.math.floor

/** *
 * Represents a build in the Blueprint Bazaar minigame.
 * It tracks how complete the build is at.
 * This Object also holds data related to the build and used to manage a build's logic within itself
 */
class BuildBlueprint(
    val game: BlueprintBazaar,
    val regionOfBuildDisplayed: CuboidRegion,
) : Listener {
    val materialList: MutableSet<Material> = mutableSetOf() // List of materials used in the build
    val region: CuboidRegion
    var numOfBlocksBuildDisplayedHas = 0
    var correctNumOfBlocksInRegion: Int = 0
        set(blocks) {
            // if we have placed a block in the region, the message will be of the color green.
            // if we have removed a block from the region, the message will be of the color red
            val color: NamedTextColor = when {
                blocks > field -> NamedTextColor.GREEN
                blocks < field -> NamedTextColor.RED
                else -> NamedTextColor.YELLOW
            }

            field = 0.coerceAtLeast(blocks)
            completionPercentage = floor((field / numOfBlocksBuildDisplayedHas).toDouble() * 100)
            val title = Title.title(
                Component.empty(),
                Component.text("${field}/${numOfBlocksBuildDisplayedHas}").color(color),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofMillis(100))
            )

            game.players.forEach { player ->
                player.showTitle(title)
            }
        }

    var completionPercentage: Double = 0.0


    var curBuildTime = 0

    var timeElapsedRunnable: BukkitTask


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
            val block = WORLD.getBlockAt(vector)

            if (block.type != Material.AIR) {
                // we don't want to count multiple times blocks that occupy more than 1 block, such as doors,so let's check if the block is bisected.
                if (block.blockData is Bisected) {
                    val bisected = block.blockData as Bisected
                    if (bisected.half == Bisected.Half.TOP) {
                        // Skip the top half, we'll only count the bottom half
                        return@forEach
                    }
                }

                numOfBlocksBuildDisplayedHas++
            }
        }

        // A timer that tracks the time elapsed for the current build. gets called every second. it gets cancelled when the build is completed.
        timeElapsedRunnable = object : BukkitRunnable() {
            override fun run() {
                game.players.forEach { player ->
                    curBuildTime++
                    player.sendActionBar(Component
                        .text("Time Elapsed: $curBuildTime seconds")
                        .color(NamedTextColor.AQUA)
                    )
                }

            }
        }.runTaskTimer(plugin,20L,20L)
    }

    fun getRawMaterialsFromThisMaterial(blockMaterial: Material,hasVisitedThisMaterial: MutableSet<Material> = mutableSetOf()) {

        // if we have reached a basic material, return that.
        if (BPBConst.BasicMaterials.contains(blockMaterial)) {
            materialList += blockMaterial
            return
        // Special case: if the material is a striped log, we want to get the base log material. (since striped logs don't have a crafting recipe)
        } else if (blockMaterial in BPBConst.StripedLogsToLogs.keys) {
            materialList += BPBConst.StripedLogsToLogs[blockMaterial]!!
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

                val materials: Set<Material> = matChoices.flatMap {
                    matChoice -> matChoice.choices
                }.toSet()

                val basicMaterials = materials.filter { BPBConst.BasicMaterials.contains(it) }

                if (basicMaterials.isNotEmpty()) {
                    setOfMaterials.addAll(basicMaterials)
                } else {
                    // if we don't have any basic materials, then we will add the first material from each mat choice that is suggested.
                    setOfMaterials += matChoices.map { it.choices[0]}
                }
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
                prepareForCompletion(event.player)
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

        // check if the block was related to the progression of the build.
        if (block.type == WORLD.getMaterialAt(block.toBlockVector3() - BPBConst.Locations.CENTER_BUILD_PLOT_OFFSET))
            correctNumOfBlocksInRegion--

        // remove block manually at a delay
        Bukkit.getServer().scheduler.runTaskLater(plugin, Runnable{
            block.world.dropItemNaturally(block.location, ItemStack(block.type))
            block.type = Material.AIR
        },3L)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        event.isCancelled = true
    }

    /**
     * Prepares the build for completion.
     *
     * This method stops the timer for the current build and shows a title to the player who completed the build if a player is designated as the finisher, otherwise it just completes the build.
     * Also clears the arena by removing items that are on the ground and unregisters the listener for this build.
     */
    fun prepareForCompletion(buildFinisher: Player? = null) {
        // let's first clear the arena by clearing items that are on the ground
        WORLD.removeItemsInRegion(BPBConst.Locations.ARENA_REGION)

        // stop the timer for the current build
        timeElapsedRunnable.cancel()

        // unregister the listener for this build
        HandlerList.unregisterAll(this)

        // if we have called this method with a player to designate as the finisher of the build, then we will show a title to that player and send a message to that player.
        if (buildFinisher != null) {
            // Show a title to all players the time elapsed for the build
            val title = Title.title(
                Component.text("Build completed in: ${curBuildTime}s!").color(NamedTextColor.GREEN),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1000), Duration.ofMillis(300))
            )

            buildFinisher.sendMessage("You have managed to complete the build!")
            buildFinisher.showTitle(title)
        }

        game.completeBuild(this)
    }
}


