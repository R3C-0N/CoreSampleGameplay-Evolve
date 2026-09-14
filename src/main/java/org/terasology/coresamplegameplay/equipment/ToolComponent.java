// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * A tool: what family it belongs to, and what grade of material it is made of.
 * <p>
 * The family and the item's damage type decide how fast a block breaks; the grade decides whether it gives
 * anything back, see {@link HarvestGrades}. Grades follow the material tiers: 0 bare hands and wood, 1 flint,
 * 2 copper, 3 bronze, 4 iron, 5 steel, 6 the fantastic metals, 7 etherium.
 */
public class ToolComponent implements Component<ToolComponent> {
    /** {@code pickaxe}, {@code axe}, {@code shovel}, {@code hoe} or {@code knife}. */
    @Replicate
    public String family;

    @Replicate
    public int grade;

    @Override
    public void copyFrom(ToolComponent other) {
        this.family = other.family;
        this.grade = other.grade;
    }
}
