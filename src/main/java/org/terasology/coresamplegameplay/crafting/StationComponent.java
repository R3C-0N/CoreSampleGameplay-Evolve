// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.gestalt.entitysystem.component.Component;

/**
 * Marks a block as a crafting station. Activating it opens the list of what can be made there;
 * nothing is ever put into it, the ingredients come from the player's own inventory.
 */
public class StationComponent implements Component<StationComponent> {
    /** Matched against {@link RecipeComponent#station}: {@code "workbench"}, later the furnaces. */
    public String type;

    @Override
    public void copyFrom(StationComponent other) {
        this.type = other.type;
    }
}
