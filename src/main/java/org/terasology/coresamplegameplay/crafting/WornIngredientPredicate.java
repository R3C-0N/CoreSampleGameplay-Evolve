// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import com.google.common.base.Predicate;
import org.terasology.durability.components.DurabilityComponent;
import org.terasology.engine.entitySystem.entity.EntityRef;

/**
 * Accepts an item that a repair can do something for: the right one, and worn. Carrying a new pickaxe and a broken
 * one, the repair takes the broken one — a predicate that only knew the prefab could take either.
 */
public final class WornIngredientPredicate implements Predicate<EntityRef> {
    private final UriIngredientPredicate what;

    public WornIngredientPredicate(UriIngredientPredicate what) {
        this.what = what;
    }

    @Override
    public boolean apply(EntityRef item) {
        DurabilityComponent durability = item.getComponent(DurabilityComponent.class);
        return durability != null && durability.durability < durability.maxDurability && what.apply(item);
    }
}
