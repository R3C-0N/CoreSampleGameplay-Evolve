// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.entitySystem.entity.EntityRef;

import java.util.List;

/**
 * What the crafting panel needs to know about recipes, and nothing about how they are registered.
 */
public interface RecipeBook {
    /**
     * The recipes available to this character where it stands — hand recipes always, a station's only near
     * it — the ones it can make first.
     */
    List<RecipeView> recipesFor(EntityRef character);

    /**
     * Asks the server to make one. Crafting is still decided there; this only picks the ingredients to use.
     *
     * @return false when the character does not have what it takes, so nothing was sent
     */
    boolean craft(EntityRef character, String recipeId);
}
