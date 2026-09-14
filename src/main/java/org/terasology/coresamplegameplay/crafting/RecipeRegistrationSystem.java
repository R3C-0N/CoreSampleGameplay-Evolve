// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.workstationCrafting.system.CraftInHandRecipeRegistry;
import org.terasology.workstationCrafting.system.recipe.behaviour.ConsumeItemCraftBehaviour;
import org.terasology.workstationCrafting.system.recipe.hand.CompositeTypeBasedCraftInHandRecipe;
import org.terasology.workstationCrafting.system.recipe.hand.CraftInHandRecipe;
import org.terasology.workstationCrafting.system.recipe.hand.PlayerInventorySlotResolver;
import org.terasology.workstationCrafting.system.recipe.render.RecipeResultFactory;
import org.terasology.workstationCrafting.system.recipe.render.result.BlockRecipeResultFactory;
import org.terasology.workstationCrafting.system.recipe.render.result.ItemRecipeResultFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Turns every {@link RecipeComponent} prefab into a recipe of the hand-crafting registry.
 * <p>
 * WorkstationCrafting reads no recipe prefab by itself — its content modules each register theirs in
 * code — so without this system a recipe file would be inert. A malformed recipe is logged and skipped,
 * never allowed to take the others down with it.
 */
@RegisterSystem
public class RecipeRegistrationSystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(RecipeRegistrationSystem.class);

    @In
    private CraftInHandRecipeRegistry recipeRegistry;
    @In
    private PrefabManager prefabManager;
    @In
    private BlockManager blockManager;
    @In
    private WorldProvider worldProvider;

    @Override
    public void initialise() {
        int registered = 0;
        for (Prefab prefab : prefabManager.listPrefabs(RecipeComponent.class)) {
            try {
                recipeRegistry.addCraftInHandRecipe(prefab.getUrn().toString(),
                        build(prefab.getComponent(RecipeComponent.class)));
                registered++;
            } catch (RuntimeException e) {
                logger.error("Recipe {} skipped: {}", prefab.getUrn(), e.getMessage());
            }
        }
        logger.info("Registered {} recipes", registered);
    }

    private CraftInHandRecipe build(RecipeComponent definition) {
        CompositeTypeBasedCraftInHandRecipe recipe = new CompositeTypeBasedCraftInHandRecipe(resultFactory(definition));
        for (String ingredient : definition.ingredients) {
            String[] countAndUris = ingredient.split("\\*", 2);
            if (countAndUris.length != 2) {
                throw new IllegalArgumentException("ingredient '" + ingredient + "' is not count*uri");
            }
            int count = Integer.parseInt(countAndUris[0].trim());
            Set<ResourceUrn> accepted = new HashSet<>();
            for (String uri : countAndUris[1].split("\\|")) {
                accepted.add(new ResourceUrn(uri.trim()));
            }
            recipe.addItemCraftBehaviour(new ConsumeItemCraftBehaviour(new UriIngredientPredicate(accepted), count,
                    PlayerInventorySlotResolver.singleton()));
        }
        return definition.station == null ? recipe : new StationRecipe(recipe, definition.station, worldProvider);
    }

    /** An item prefab first: asking the block manager for a name that is not a block logs an error. */
    private RecipeResultFactory resultFactory(RecipeComponent definition) {
        Prefab item = prefabManager.getPrefab(definition.result);
        if (item != null) {
            return new ItemRecipeResultFactory(item, definition.count);
        }
        BlockFamily block = blockManager.getBlockFamily(definition.result);
        if (block != null) {
            return new BlockRecipeResultFactory(block.getArchetypeBlock(), definition.count);
        }
        throw new IllegalArgumentException("result '" + definition.result + "' is neither an item nor a block");
    }
}
