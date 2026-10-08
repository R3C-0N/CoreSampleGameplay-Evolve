// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.terasology.gestalt.entitysystem.component.Component;

/**
 * One stage of a growing plant, on its block entity.
 * <p>
 * The two time fields start at nought with each stage: the block entity is rebuilt from the next stage's prefab
 * when the block changes, and that is what resets them. Growth is counted rather than dated, so that only the
 * time spent in the light, on watered soil, counts towards the next stage.
 */
public class CropComponent implements Component<CropComponent> {

    /** The next stage, as a block URI. Empty once the plant is ripe. */
    public String next = "";
    /** The seed the plant is sown from, which is what is left of it when its soil dries out. */
    public String seed = "";
    /** Milliseconds this stage needs, drawn when it starts growing; nought until then. */
    public long needed;
    /** Milliseconds this stage has grown so far. */
    public long grown;

    @Override
    public void copyFrom(CropComponent other) {
        this.next = other.next;
        this.seed = other.seed;
        this.needed = other.needed;
        this.grown = other.grown;
    }
}
