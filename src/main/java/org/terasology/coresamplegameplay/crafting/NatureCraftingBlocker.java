// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.network.ClientComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.workstationCrafting.event.NatureCraftingButton;

/**
 * Crafting lives in the character screen now, so WorkstationCrafting's own window must not open on N.
 * <p>
 * Swallowing the key is the only switch there is: {@code CraftInHandRecipeRegistry.disableCraftingInHand}
 * would also make the server refuse every craft request, including the ones the character screen sends.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class NatureCraftingBlocker extends BaseComponentSystem {

    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = ClientComponent.class)
    public void swallow(NatureCraftingButton event, EntityRef client) {
        event.consume();
    }
}
