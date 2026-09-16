// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Marks a character as building rather than surviving: it takes no damage, places without spending, breaks in
 * one blow and opens the catalogue instead of its backpack.
 * <p>
 * It sits on the character entity, which is persisted, so the mode survives a save. It is replicated because
 * the client alone decides which screen the inventory key opens.
 */
public class CreativeModeComponent implements Component<CreativeModeComponent> {
    /** Whether the character is currently flying; remembered so that a screen or a respawn does not ground it. */
    @Replicate
    public boolean flying;

    @Override
    public void copyFrom(CreativeModeComponent other) {
        this.flying = other.flying;
    }
}
