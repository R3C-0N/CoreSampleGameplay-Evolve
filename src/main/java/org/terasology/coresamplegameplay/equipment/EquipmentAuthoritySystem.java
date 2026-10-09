// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.coresamplegameplay.creative.CreativeModeComponent;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.players.PlayerCharacterComponent;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.events.BeforeDamagedEvent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.events.BeforeItemPutInInventory;

/**
 * What makes the equipment slots more than inventory slots: they only take what fits, and what they hold
 * protects the one wearing it, and wears for it.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class EquipmentAuthoritySystem extends BaseComponentSystem {
    /**
     * Points of damage absorbed per point of wear. A blow is counted in tens where a block is one: on the scale tools
     * share, one point per point absorbed broke a lone iron helmet in about 135 blows of 20.
     */
    private static final float DAMAGE_PER_WEAR = 4f;

    private final Random random = new FastRandom();

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
            float factor = CharacterStats.damageFactor(defense);
            event.multiply(factor);
            if (event.getInstigator().exists() && !character.hasComponent(CreativeModeComponent.class)) {
                wearArmor(character, defense, event.getBaseValue() * (1 - factor));
            }
        }
    }

    /**
     * Armour wears by the damage it takes: one point per {@link #DAMAGE_PER_WEAR} absorbed, shared between the pieces worn by what each protects.
     * Only blows wear it — something struck, not a fall. A share below one point is drawn at random, so that a
     * shower of small blows wears it as much as one heavy blow, on average.
     */
    private void wearArmor(EntityRef character, int defense, float absorbed) {
        for (int slot = EquipmentSlots.FIRST; slot < EquipmentSlots.TOTAL; slot++) {
            EntityRef item = CharacterStats.itemAt(character, slot);
            int protection = CharacterStats.protection(item);
            if (protection > 0) {
                float share = absorbed / DAMAGE_PER_WEAR * protection / defense;
                int points = (int) share;
                if (random.nextFloat() < share - points) {
                    points++;
                }
                Wear.wear(item, points);
            }
        }
    }
}
