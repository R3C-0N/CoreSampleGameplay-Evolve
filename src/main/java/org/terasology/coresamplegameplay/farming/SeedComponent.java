// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.terasology.gestalt.entitysystem.component.Component;

/**
 * An item that is sown on tilled soil on the right button, and the plant it comes up as.
 * <p>
 * The item must also carry {@code Item.consumedOnUse}: the Inventory module takes one from the stack once the use
 * has gone through, and {@link FarmingAuthoritySystem} consumes the event whenever the sowing is refused.
 */
public class SeedComponent implements Component<SeedComponent> {

    /** The first stage of the plant, as a block URI. */
    public String plant = "";

    @Override
    public void copyFrom(SeedComponent other) {
        this.plant = other.plant;
    }
}
