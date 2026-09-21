// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;

/**
 * Hangs the burning veil on the heads-up display. The widget draws nothing at all unless the local
 * player carries a {@link BurningComponent}, so it costs one empty draw call the rest of the time.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class BurnClientSystem extends BaseComponentSystem {

    private static final String OVERLAY = "CoreSampleGameplay:burnOverlay";
    private static final Logger logger = LoggerFactory.getLogger(BurnClientSystem.class);

    @In
    private NUIManager nuiManager;

    @Override
    public void initialise() {
        // addHUDElement returns null and says nothing at all when the asset will not load or its root
        // widget is of the wrong kind, which is a long way to look for a veil that never appears.
        if (nuiManager.getHUD().addHUDElement(OVERLAY) == null) {
            logger.warn("The burning overlay {} did not attach to the HUD; nobody will see themselves burn.",
                    OVERLAY);
        }
    }
}
