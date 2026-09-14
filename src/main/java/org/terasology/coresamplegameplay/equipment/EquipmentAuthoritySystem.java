// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.players.PlayerCharacterComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.events.BeforeDamagedEvent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.events.BeforeItemPutInInventory;

/**
 * What makes the equipment slots more than inventory slots: they only take what fits, and what they hold
 * protects the one wearing it.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class EquipmentAuthoritySystem extends BaseComponentSystem {

    /**
     * Characters saved before equipment existed have 40 slots, and the prefab only reaches new ones. Growing
     * the list on activation keeps every item where it was: the new slots are appended, never inserted.
     */
    @ReceiveEvent(components = PlayerCharacterComponent.class)
    public void ensureEquipmentSlots(OnActivatedComponent event, EntityRef character, InventoryComponent inventory) {
        if (inventory.itemSlots.size() >= EquipmentSlots.TOTAL) {
            return;
        }
        while (inventory.itemSlots.size() < EquipmentSlots.TOTAL) {
            inventory.itemSlots.add(EntityRef.NULL);
        }
        character.saveComponent(inventory);
    }

    /** Covers every way in at once: a click, a shift-click, a pickup falling into the first free slot. */
    @ReceiveEvent(components = PlayerCharacterComponent.class)
    public void onlyWhatFits(BeforeItemPutInInventory event, EntityRef character) {
        if (!EquipmentSlots.isEquipment(event.getSlot())) {
            return;
        }
        ArmorComponent armor = event.getItem().getComponent(ArmorComponent.class);
        if (armor == null || !EquipmentSlots.kindOf(event.getSlot()).equals(armor.slot)) {
            event.consume();
        }
    }

    @ReceiveEvent(components = PlayerCharacterComponent.class)
    public void absorbDamage(BeforeDamagedEvent event, EntityRef character) {
        int defense = CharacterStats.defense(character);
        if (defense > 0) {
            event.multiply(CharacterStats.damageFactor(defense));
        }
    }
}
