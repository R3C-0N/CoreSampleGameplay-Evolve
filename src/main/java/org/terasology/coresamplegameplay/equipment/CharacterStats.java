// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.components.SelectedInventorySlotComponent;

/**
 * The numbers the character panel shows, read from the entity every time — there is no stat to keep in sync.
 */
public final class CharacterStats {
    /** What a bare hand hits for, and how often: the defaults of an item with nothing set. */
    public static final int UNARMED_DAMAGE = 1;
    public static final int UNARMED_COOLDOWN_MS = 200;

    /**
     * Defence at which damage is halved. The curve {@code k / (k + defence)} never reaches zero, so no armour
     * makes a character immune, and each point is worth less than the one before.
     */
    public static final float ARMOR_CURVE = 20f;

    private CharacterStats() {
    }

    public static int defense(EntityRef character) {
        int total = 0;
        for (int slot = EquipmentSlots.FIRST; slot < EquipmentSlots.TOTAL; slot++) {
            ArmorComponent armor = itemAt(character, slot).getComponent(ArmorComponent.class);
            if (armor != null) {
                total += armor.protection;
            }
        }
        return total;
    }

    public static float damageFactor(int defense) {
        return ARMOR_CURVE / (ARMOR_CURVE + Math.max(0, defense));
    }

    public static int attack(EntityRef character) {
        ItemComponent item = heldItem(character);
        return item == null ? UNARMED_DAMAGE : item.baseDamage;
    }

    /** Hits per second with what is in hand. */
    public static float attackSpeed(EntityRef character) {
        ItemComponent item = heldItem(character);
        int cooldown = item == null ? UNARMED_COOLDOWN_MS : item.cooldownTime;
        return cooldown <= 0 ? 0f : 1000f / cooldown;
    }

    public static EntityRef itemAt(EntityRef character, int slot) {
        InventoryComponent inventory = character.getComponent(InventoryComponent.class);
        if (inventory == null || slot < 0 || slot >= inventory.itemSlots.size()) {
            return EntityRef.NULL;
        }
        return inventory.itemSlots.get(slot);
    }

    private static ItemComponent heldItem(EntityRef character) {
        SelectedInventorySlotComponent selected = character.getComponent(SelectedInventorySlotComponent.class);
        if (selected == null) {
            return null;
        }
        return itemAt(character, selected.slot).getComponent(ItemComponent.class);
    }
}
