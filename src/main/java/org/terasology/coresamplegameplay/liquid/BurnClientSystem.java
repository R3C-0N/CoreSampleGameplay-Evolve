// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

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

    @In
    private NUIManager nuiManager;

    @Override
    public void initialise() {
        nuiManager.getHUD().addHUDElement("CoreSampleGameplay:burnOverlay");
    }
}
