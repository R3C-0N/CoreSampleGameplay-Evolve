// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.joml.Vector3f;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.world.WorldProvider;
import org.terasology.module.inventory.ui.ItemIcon;
import org.terasology.workstationCrafting.system.recipe.hand.CraftInHandRecipe;
import org.terasology.workstationCrafting.system.recipe.render.CraftIngredientRenderer;

import java.util.List;

/**
 * A recipe that is only listed, and only made, next to a station of its type.
 * <p>
 * It reuses the hand-crafting window as it is: that window already lists what the player's own
 * inventory allows, which is exactly what a station must do. The station only adds recipes to it.
 * The check runs twice — when the client lists, and when the server crafts — because the listing is
 * the client's and the decision is the server's.
 */
public final class StationRecipe implements CraftInHandRecipe {
    /** How far, in blocks, a character may stand from the station it activated. */
    public static final float REACH = 5f;

    private final CraftInHandRecipe delegate;
    private final String stationType;
    private final WorldProvider worldProvider;

    public StationRecipe(CraftInHandRecipe delegate, String stationType, WorldProvider worldProvider) {
        this.delegate = delegate;
        this.stationType = stationType;
        this.worldProvider = worldProvider;
    }

    @Override
    public List<CraftInHandResult> getMatchingRecipeResults(EntityRef character) {
        return isAtStation(character) ? delegate.getMatchingRecipeResults(character) : null;
    }

    @Override
    public CraftInHandResult getResultByParameters(List<String> parameters) {
        CraftInHandResult result = delegate.getResultByParameters(parameters);
        return result == null ? null : new GatedResult(result);
    }

    /**
     * The character remembers a station of this type, stands within reach of it, and the block there
     * still is one. The memory alone would outlive a broken workbench.
     */
    private boolean isAtStation(EntityRef character) {
        AtStationComponent at = character.getComponent(AtStationComponent.class);
        LocationComponent location = character.getComponent(LocationComponent.class);
        if (at == null || location == null || !stationType.equals(at.type)) {
            return false;
        }
        Vector3f position = location.getWorldPosition(new Vector3f());
        if (!position.isFinite()
                || position.distance(at.position.x + 0.5f, at.position.y + 0.5f, at.position.z + 0.5f) > REACH) {
            return false;
        }
        return worldProvider.getBlock(at.position).getPrefab()
                .map(prefab -> prefab.getComponent(StationComponent.class))
                .map(station -> stationType.equals(station.type))
                .orElse(false);
    }

    private final class GatedResult implements CraftInHandResult {
        private final CraftInHandResult result;

        private GatedResult(CraftInHandResult result) {
            this.result = result;
        }

        @Override
        public List<String> getParameters() {
            return result.getParameters();
        }

        @Override
        public EntityRef craft(EntityRef character, int count) {
            return isAtStation(character) ? result.craft(character, count) : EntityRef.NULL;
        }

        @Override
        public List<CraftIngredientRenderer> getIngredientRenderers(EntityRef entity) {
            return result.getIngredientRenderers(entity);
        }

        @Override
        public boolean isValidForCrafting(EntityRef entity, int multiplier) {
            return isAtStation(entity) && result.isValidForCrafting(entity, multiplier);
        }

        @Override
        public int getMaxMultiplier(EntityRef entity) {
            return result.getMaxMultiplier(entity);
        }

        @Override
        public int getResultQuantity() {
            return result.getResultQuantity();
        }

        @Override
        public void setupResultDisplay(ItemIcon itemIcon) {
            result.setupResultDisplay(itemIcon);
        }

        @Override
        public long getProcessDuration() {
            return result.getProcessDuration();
        }
    }
}
