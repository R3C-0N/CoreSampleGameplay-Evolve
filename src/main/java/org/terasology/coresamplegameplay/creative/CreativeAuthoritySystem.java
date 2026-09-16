// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.AffectItemUseCooldownTimeEvent;
import org.terasology.engine.logic.characters.events.AttackEvent;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.health.DestroyEvent;
import org.terasology.engine.logic.health.EngineDamageTypes;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.CreateBlockDropsEvent;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.events.BeforeDamagedEvent;

/**
 * The five rules of creative mode, each one slotted into an existing consumable event.
 * <p>
 * Priorities run {@code CRITICAL 200 > HIGH 150 > NORMAL 100 > LOW 50 > TRIVIAL 0}, and a consumed event stops
 * being propagated — which is the whole mechanism here. Two of the five events are received on the character,
 * and can be filtered by component; the other three are received on an item or on a block, so they have to ask
 * their instigator.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CreativeAuthoritySystem extends BaseComponentSystem {

    /** Fall damage comes through this event too, so nothing else is needed to survive a drop. */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void takeNoDamage(BeforeDamagedEvent event, EntityRef character) {
        event.consume();
    }

    /** Breaking a block has no cooldown, so blocks can be cleared as fast as the button is clicked. */
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void useWithoutWaiting(AffectItemUseCooldownTimeEvent event, EntityRef character) {
        event.multiply(0f);
    }

    /**
     * The block is placed by {@code BlockItemSystem} at {@code PRIORITY_NORMAL}; consuming here, below it, only
     * keeps {@code ItemAuthoritySystem.usedItem} at {@code PRIORITY_TRIVIAL} from spending the stack.
     * <p>
     * Anything else listening at {@code TRIVIAL} would be cut off as well. Today nothing else is there, and the
     * scraping of soil sits at {@code NORMAL}, so it still runs.
     */
    @Priority(EventPriority.PRIORITY_LOW)
    @ReceiveEvent(components = BlockItemComponent.class)
    public void placeWithoutSpending(ActivateEvent event, EntityRef item) {
        if (isCreative(event.getInstigator())) {
            event.consume();
        }
    }

    /**
     * One blow, one block. Destroying it here, above {@code BlockDamageAuthoritySystem}, means the block never
     * gets the {@code HealthComponent} that would have made it take several hits.
     */
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = BlockComponent.class)
    public void breakInOneBlow(AttackEvent event, EntityRef blockEntity) {
        if (isCreative(event.getInstigator())) {
            blockEntity.send(new DestroyEvent(event.getInstigator(), event.getDirectCause(),
                    EngineDamageTypes.PHYSICAL.get()));
            event.consume();
        }
    }

    /**
     * Nothing falls in creative: the catalogue gives what is wanted, and a floor covered in what was cleared is
     * only in the way. This runs above {@code HarvestAuthoritySystem}, whose tool grades no longer apply.
     */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent(components = BlockComponent.class)
    public void dropNothing(CreateBlockDropsEvent event, EntityRef blockEntity) {
        if (isCreative(event.getInstigator())) {
            event.consume();
        }
    }

    private static boolean isCreative(EntityRef character) {
        return character.hasComponent(CreativeModeComponent.class);
    }
}
