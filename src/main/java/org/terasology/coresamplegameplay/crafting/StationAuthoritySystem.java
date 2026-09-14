// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Activating a station records it on the character, then asks that character's client to open the list.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class StationAuthoritySystem extends BaseComponentSystem {

    /**
     * {@link BlockComponent} is required on purpose: a station held as an item inherits the components
     * of the block's prefab, and using it from the hand must not open anything.
     */
    @ReceiveEvent(components = BlockComponent.class)
    public void onActivate(ActivateEvent event, EntityRef station, StationComponent stationComponent) {
        EntityRef character = event.getInstigator();
        AtStationComponent at = character.getComponent(AtStationComponent.class);
        if (at == null) {
            at = new AtStationComponent();
        }
        at.type = stationComponent.type;
        at.position.set(station.getComponent(BlockComponent.class).getPosition());
        character.addOrSaveComponent(at);
        character.send(new OpenStationEvent());
    }
}
