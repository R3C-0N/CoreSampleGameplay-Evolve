// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.food;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * How full a character is. It empties with time and fills with what is eaten.
 * <p>
 * A float, not an int: at the default rate the gauge loses a point every twenty-four seconds, and a gauge kept in
 * whole points would either stand still or need a remainder kept somewhere else.
 */
public class HungerComponent implements Component<HungerComponent> {

    @Replicate
    public float maxFood = 100f;

    @Replicate
    public float currentFood = 100f;

    /** Points lost a second of game time: a full gauge lasts forty minutes. */
    public float decayPerSecond = 100f / 2400f;

    /** Below this share of the gauge the body stops mending: health no longer regenerates. */
    public float regenThreshold = 0.25f;

    /** Seconds between two points of damage once the gauge is empty. */
    public float starvationPeriod = 4f;

    @Override
    public void copyFrom(HungerComponent other) {
        this.maxFood = other.maxFood;
        this.currentFood = other.currentFood;
        this.decayPerSecond = other.decayPerSecond;
        this.regenThreshold = other.regenThreshold;
        this.starvationPeriod = other.starvationPeriod;
    }
}
