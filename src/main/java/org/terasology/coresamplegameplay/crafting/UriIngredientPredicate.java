// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import com.google.common.base.Predicate;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.gestalt.assets.ResourceUrn;

import java.util.Set;

/**
 * Accepts an item by what it is: the prefab it was made from, or the block family it carries.
 * <p>
 * This is what lets a recipe name {@code CoreAssets:OakTrunk} without every trunk and every material
 * having to carry a crafting marker. A shaped block — a plank stair — is not a plank.
 */
public final class UriIngredientPredicate implements Predicate<EntityRef> {
    private final Set<ResourceUrn> accepted;

    public UriIngredientPredicate(Set<ResourceUrn> accepted) {
        this.accepted = Set.copyOf(accepted);
    }

    @Override
    public boolean apply(EntityRef item) {
        BlockItemComponent blockItem = item.getComponent(BlockItemComponent.class);
        if (blockItem != null) {
            if (blockItem.blockFamily == null) {
                return false;
            }
            BlockUri uri = blockItem.blockFamily.getURI();
            return uri.getShapeUrn().isEmpty() && accepted.contains(uri.getBlockFamilyDefinitionUrn());
        }
        Prefab prefab = item.getParentPrefab();
        return prefab != null && accepted.contains(prefab.getUrn());
    }
}
