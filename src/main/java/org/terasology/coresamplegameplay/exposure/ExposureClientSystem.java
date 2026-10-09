// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;

/**
 * Hangs the exposure overlay on the heads-up display. It draws nothing while both gauges are at rest and the
 * character is out of the extreme regions.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ExposureClientSystem extends BaseComponentSystem {

    private static final String OVERLAY = "CoreSampleGameplay:exposureOverlay";
    private static final Logger logger = LoggerFactory.getLogger(ExposureClientSystem.class);

    @In
    private NUIManager nuiManager;

    @Override
    public void initialise() {
        if (nuiManager.getHUD().addHUDElement(OVERLAY) == null) {
            logger.warn("The exposure overlay {} did not attach to the HUD; no gauge nor veil will show.", OVERLAY);
        }
    }
}
