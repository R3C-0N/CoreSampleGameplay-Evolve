// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.food;

import org.terasology.gestalt.entitysystem.component.Component;

/**
 * An item that is eaten on the right button, and how much of the hunger gauge it fills.
 * <p>
 * The item must also carry {@code Item.consumedOnUse}: the Inventory module takes one from the stack once the
 * use has gone through, and {@link HungerAuthoritySystem} consumes the event whenever the meal is refused, so a
 * refused meal is never eaten.
 */
public class FoodComponent implements Component<FoodComponent> {

    /** Points of the hunger gauge, out of a hundred. */
    public float nourishment;

    @Override
    public void copyFrom(FoodComponent other) {
        this.nourishment = other.nourishment;
    }
}
