// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.CreateBlockDropsEvent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Keeps a block's drops from a tool below its grade. The block is already gone by then: the loss is the
 * lesson, a block that would not break would teach nothing.
 * <p>
 * Only the engine's default drops go through this event. A demanding block given a {@code DropGrammar} one
 * day drops on {@code DoDestroyEvent} instead, and this check would have to follow it there.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class HarvestAuthoritySystem extends BaseComponentSystem {

    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent
    public void dropOnlyForTheRightTool(CreateBlockDropsEvent event, EntityRef blockEntity, BlockComponent block) {
        if (!HarvestGrades.canHarvest(block.getBlock(), event.getDirectCause())) {
            event.consume();
        }
    }
}
