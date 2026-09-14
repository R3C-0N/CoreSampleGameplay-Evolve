// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

/**
 * How the player's single inventory is cut up. Equipment is not a second inventory: it is the tail of the
 * first, so that every inventory tool — cells, transfers, drops — already knows how to move items in and out.
 * <pre>
 *     0 .. 9     toolbar
 *     10 .. 45   backpack grid, nine by four
 *     46 .. 53   head, chest, legs, feet, cape, amulet, ring, ring
 * </pre>
 */
public final class EquipmentSlots {
    public static final int TOOLBAR = 10;
    public static final int GRID = 36;
    public static final int FIRST = TOOLBAR + GRID;

    /** The {@link ArmorComponent#slot} each equipment slot accepts, in slot order. */
    private static final String[] KINDS = {"head", "chest", "legs", "feet", "cape", "amulet", "ring", "ring"};

    public static final int TOTAL = FIRST + KINDS.length;

    private EquipmentSlots() {
    }

    public static boolean isEquipment(int slot) {
        return slot >= FIRST && slot < TOTAL;
    }

    public static String kindOf(int slot) {
        return KINDS[slot - FIRST];
    }
}
