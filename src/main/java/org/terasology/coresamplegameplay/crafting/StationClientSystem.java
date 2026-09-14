// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Opens the character screen when the server says a station was activated. Its crafting panel lists what
 * the station adds on its own, through {@link StationRecipe}.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class StationClientSystem extends BaseComponentSystem {
    private static final String SCREEN = "Inventory:inventoryScreen";

    @In
    private NUIManager nuiManager;

    @ReceiveEvent
    public void onOpenStation(OpenStationEvent event, EntityRef character) {
        if (!nuiManager.isOpen(SCREEN)) {
            nuiManager.pushScreen(SCREEN);
        }
    }
}
