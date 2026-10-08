// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Soil turned with a hoe, on its block entity. Whether it is watered shows in the block, dry or wet; how long it has
 * been without water is kept here.
 */
public class TilledSoilComponent implements Component<TilledSoilComponent> {

    /** Milliseconds spent without water nearby; back to nought as soon as water is found. */
    public long dry;

    @Override
    public void copyFrom(TilledSoilComponent other) {
        this.dry = other.dry;
    }
}
