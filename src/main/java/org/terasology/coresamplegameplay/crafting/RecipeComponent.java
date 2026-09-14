// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.gestalt.entitysystem.component.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A recipe is a list of what it takes, and what it gives. Nothing is laid out in a grid.
 * <p>
 * Lives on a prefab of its own under {@code prefabs/recipes/}; {@link RecipeRegistrationSystem} reads
 * every one of them at startup.
 */
public class RecipeComponent implements Component<RecipeComponent> {
    /**
     * {@code "count*uri"}, the uri of an item prefab or of a block family. Several uris separated by
     * {@code |} are alternatives: {@code "1*CoreAssets:OakTrunk|CoreAssets:PineTrunk"} takes either.
     */
    public List<String> ingredients = new ArrayList<>();

    /** The uri of the item prefab or block family made. */
    public String result;

    public int count = 1;

    /** The {@link StationComponent#type} that must be nearby, or null for a recipe made by hand. */
    public String station;

    @Override
    public void copyFrom(RecipeComponent other) {
        this.ingredients = new ArrayList<>(other.ingredients);
        this.result = other.result;
        this.count = other.count;
        this.station = other.station;
    }
}
