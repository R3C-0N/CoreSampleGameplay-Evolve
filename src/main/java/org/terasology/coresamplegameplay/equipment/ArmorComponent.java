// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * A piece of armour: its weight class, where it is worn, and how much it protects. Nothing reads this
 * yet — there is no armour system — but the vocabulary is fixed here so that recipes can already say it.
 */
public class ArmorComponent implements Component<ArmorComponent> {
    /** {@code light}, {@code medium} or {@code heavy}. */
    @Replicate
    public String weight;

    /** {@code head}, {@code chest}, {@code legs} or {@code feet}. */
    @Replicate
    public String slot;

    @Replicate
    public int protection;

    @Override
    public void copyFrom(ArmorComponent other) {
        this.weight = other.weight;
        this.slot = other.slot;
        this.protection = other.protection;
    }
}
