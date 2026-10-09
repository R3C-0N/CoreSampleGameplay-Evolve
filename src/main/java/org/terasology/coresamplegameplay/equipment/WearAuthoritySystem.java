// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.coresamplegameplay.creative.CreativeModeComponent;
import org.terasology.durability.components.DurabilityComponent;
import org.terasology.durability.events.DurabilityExhaustedEvent;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.events.AttackEvent;
import org.terasology.engine.logic.health.DestroyEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.damage.BlockDamageModifierComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.components.HealthComponent;

/**
 * Wears what is held, and keeps what is broken from counting.
 * <p>
 * A tool wears by {@link Wear#USE} on a block of its own family and by {@link Wear#MISUSE} on anything else — a
 * pickaxe cutting wood wears twice as fast. A weapon wears by {@link Wear#USE} on a blow landed, by
 * {@link Wear#MISUSE} on a block. Armour wears where the damage is absorbed, see
 * {@link EquipmentAuthoritySystem}. Creative wears nothing.
 * <p>
 * The Durability module already takes one point off a tool that breaks a block of its family, and destroys an item
 * at zero. This system adds what the module does not count, and keeps the item: at zero it is broken, see
 * {@link Wear}.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class WearAuthoritySystem extends BaseComponentSystem {

    /**
     * A broken item hits like a bare hand: the blow is sent again without it, so that every system downstream —
     * block damage, creature damage, drops — sees no item at all rather than having to ask each time.
     */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent
    public void strike(AttackEvent event, EntityRef target) {
        EntityRef item = event.getDirectCause();
        if (Wear.isBroken(item)) {
            event.consume();
            target.send(new AttackEvent(event.getInstigator(), EntityRef.NULL));
            return;
        }
        // Blocks gain health once struck, so the block test comes first; they wear the item when they break.
        if (target.hasComponent(BlockComponent.class) || !target.hasComponent(HealthComponent.class)
                || isCreative(event.getInstigator())) {
            return;
        }
        Wear.wear(item, item.hasComponent(WeaponComponent.class) ? Wear.USE : Wear.MISUSE);
    }

    @ReceiveEvent
    public void wearOnBlockBroken(DestroyEvent event, EntityRef blockEntity, BlockComponent block) {
        EntityRef item = event.getDirectCause();
        if (!Wear.canWear(item) || isCreative(event.getInstigator())) {
            return;
        }
        boolean ownFamily = countedByDurabilityModule(block, event.getDamageType());
        int due = ownFamily && item.hasComponent(ToolComponent.class) ? Wear.USE : Wear.MISUSE;
        Wear.wear(item, due - (ownFamily ? Wear.USE : 0));
    }

    /** Runs before the module's own handler, which destroys the item. */
    @ReceiveEvent
    public void keepBrokenItem(DurabilityExhaustedEvent event, EntityRef item, DurabilityComponent durability,
                               ItemComponent itemComponent) {
        event.consume();
    }

    /**
     * The module's own test, word for word: the point it has already taken must not be taken twice. A tool's damage
     * type lists the block categories it is made for; a block with no category is no tool's.
     */
    private static boolean countedByDurabilityModule(BlockComponent block, Prefab damageType) {
        BlockDamageModifierComponent modifier = damageType == null ? null
                : damageType.getComponent(BlockDamageModifierComponent.class);
        if (modifier == null) {
            return false;
        }
        for (String category : block.getBlock().getBlockFamily().getCategories()) {
            if (modifier.materialDamageMultiplier.containsKey(category)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCreative(EntityRef character) {
        return character.hasComponent(CreativeModeComponent.class);
    }
}
