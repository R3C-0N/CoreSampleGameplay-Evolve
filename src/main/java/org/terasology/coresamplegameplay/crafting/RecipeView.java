// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.family.BlockFamily;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One recipe as a given character sees it right now: what it makes, and how much of each ingredient that
 * character carries. Immutable; the crafting panel asks for fresh ones rather than watching these change.
 */
public final class RecipeView {
    private final String id;
    private final String name;
    private final String description;
    private final boolean craftable;
    private final List<Ingredient> ingredients;

    RecipeView(String id, String name, String description, List<Ingredient> ingredients) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.ingredients = List.copyOf(ingredients);
        this.craftable = ingredients.stream().allMatch(Ingredient::isSatisfied);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCraftable() {
        return craftable;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    /** {@code "4 Planche · 2 Corde"}. */
    public String getSummary() {
        return ingredients.stream().map(i -> i.getNeeded() + " " + i.getName()).collect(Collectors.joining(" · "));
    }

    /** Empty when nothing is missing. */
    public String getMissingText() {
        List<Ingredient> missing = ingredients.stream().filter(i -> !i.isSatisfied()).collect(Collectors.toList());
        if (missing.isEmpty()) {
            return "";
        }
        if (missing.size() == 1) {
            Ingredient only = missing.get(0);
            return "Il manque " + (only.getNeeded() - only.getOwned()) + " " + only.getName();
        }
        return "Il manque " + missing.size() + " composants";
    }

    /** Changes whenever anything the panel draws would. */
    public String getSignature() {
        StringBuilder signature = new StringBuilder(id).append(craftable ? '+' : '-');
        for (Ingredient ingredient : ingredients) {
            signature.append(ingredient.getOwned()).append(',');
        }
        return signature.toString();
    }

    public static final class Ingredient {
        private final String name;
        private final int needed;
        private final int owned;
        private final Prefab itemPrefab;
        private final BlockFamily blockFamily;

        Ingredient(String name, int needed, int owned, Prefab itemPrefab, BlockFamily blockFamily) {
            this.name = name;
            this.needed = needed;
            this.owned = owned;
            this.itemPrefab = itemPrefab;
            this.blockFamily = blockFamily;
        }

        public String getName() {
            return name;
        }

        public int getNeeded() {
            return needed;
        }

        public int getOwned() {
            return owned;
        }

        public boolean isSatisfied() {
            return owned >= needed;
        }

        /** What to draw as its icon: an item prefab, or failing that a block family. Either may be null. */
        public Prefab getItemPrefab() {
            return itemPrefab;
        }

        public BlockFamily getBlockFamily() {
            return blockFamily;
        }
    }
}
