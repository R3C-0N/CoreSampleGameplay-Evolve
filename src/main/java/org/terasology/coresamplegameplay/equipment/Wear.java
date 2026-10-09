// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.durability.components.DurabilityComponent;
import org.terasology.durability.events.ReduceDurabilityEvent;
import org.terasology.engine.entitySystem.entity.EntityRef;

/**
 * What wear does to an item. The count itself is the Durability module's: {@link DurabilityComponent} holds it,
 * {@link ReduceDurabilityEvent} lowers it, and its client system draws the bar.
 * <p>
 * <strong>An item at zero is broken, not gone.</strong> It stays where it is, and every system that reads a tool, a
 * weapon or a piece of armour asks {@link #isBroken} first: a broken one counts for nothing — a bare hand, an
 * empty slot — until it is repaired at its station. Nothing else marks it: the zero is the mark.
 */
public final class Wear {
    /** What a use the item was made for costs; any other use costs {@link #MISUSE}. */
    public static final int USE = 1;
    public static final int MISUSE = 2;

    private Wear() {
    }

    public static boolean isBroken(EntityRef item) {
        DurabilityComponent durability = item.getComponent(DurabilityComponent.class);
        return durability != null && durability.durability <= 0;
    }

    /** Whether the item wears at all, and still can. */
    public static boolean canWear(EntityRef item) {
        DurabilityComponent durability = item.getComponent(DurabilityComponent.class);
        return durability != null && durability.durability > 0;
    }

    public static void wear(EntityRef item, int points) {
        if (points > 0 && canWear(item)) {
            item.send(new ReduceDurabilityEvent(points));
        }
    }
}
