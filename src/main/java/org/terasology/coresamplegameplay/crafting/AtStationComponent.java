// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.joml.Vector3i;
import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * The last station a character activated. Set by the server and replicated, so that the client lists
 * the same recipes the server will accept.
 * <p>
 * It is a memory, not a proof: {@link StationRecipe} still checks the distance and that the block is
 * still there, on both sides.
 */
public class AtStationComponent implements Component<AtStationComponent> {
    @Replicate
    public String type;

    @Replicate
    public Vector3i position = new Vector3i();

    @Override
    public void copyFrom(AtStationComponent other) {
        this.type = other.type;
        this.position = new Vector3i(other.position);
    }
}
