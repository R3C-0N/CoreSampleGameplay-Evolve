// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Sits on a character standing in a liquid hot enough to set it alight.
 * <p>
 * The damage itself needs no component - it is dealt straight from the authority system's sweep. This is
 * here for the look of the thing: the flames in the world and the veil on the burning player's screen.
 */
public class BurningComponent implements Component<BurningComponent> {

    /**
     * The warmth of the liquid doing it, so the veil can burn brighter in something hotter.
     */
    @Replicate
    public int warmth;

    @Override
    public void copyFrom(BurningComponent other) {
        this.warmth = other.warmth;
    }
}
