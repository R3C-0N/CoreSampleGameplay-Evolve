// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockFamily;

import java.util.Map;

/**
 * What a block asks of the tool that breaks it before it gives anything back.
 * <p>
 * Below the grade the block still breaks, and nothing falls: stone is a pickaxe's work from flint on. Wood,
 * soil and plants ask for nothing. Each tier raises its own ores here when it arrives.
 */
public final class HarvestGrades {
    public static final int FLINT = 1;
    public static final int COPPER = 2;
    public static final int BRONZE = 3;
    public static final int IRON = 4;
    public static final int STEEL = 5;

    private static final Map<String, String> FAMILY_BY_CATEGORY = Map.of("rock", "pickaxe", "mineral", "pickaxe");

    /** Tin comes out with copper tools, iron with bronze ones; every other stone asks for flint. */
    private static final Map<BlockUri, Integer> GRADE_BY_BLOCK = Map.of(
            new BlockUri("CoreAssets:TinOre"), COPPER,
            new BlockUri("CoreAssets:IronOre"), BRONZE);

    private HarvestGrades() {
    }

    /** Whether breaking this block with that held item — or that bare hand — gives the block's drops. */
    public static boolean canHarvest(Block block, EntityRef heldItem) {
        BlockFamily family = block.getBlockFamily();
        String requiredFamily = null;
        for (String category : family.getCategories()) {
            requiredFamily = FAMILY_BY_CATEGORY.get(category.toLowerCase());
            if (requiredFamily != null) {
                break;
            }
        }
        if (requiredFamily == null) {
            return true;
        }
        int requiredGrade = GRADE_BY_BLOCK.getOrDefault(family.getURI(), FLINT);
        ToolComponent tool = heldItem.getComponent(ToolComponent.class);
        return tool != null && requiredFamily.equals(tool.family) && tool.grade >= requiredGrade;
    }
}
