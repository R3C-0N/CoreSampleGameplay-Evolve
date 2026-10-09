// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.GetMaxSpeedEvent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Slows down a character the cold or the thin air has got to.
 * <p>
 * Registered on both sides, not on the authority alone: the client predicts its own movement through the same
 * event, and a slowdown only the server knew of would snap the character back at every step. Both read the
 * replicated gauges, never the position, which the event does not carry.
 */
@RegisterSystem
public class ExposureSpeedSystem extends BaseComponentSystem {

    @ReceiveEvent
    public void slow(GetMaxSpeedEvent event, EntityRef character, ExposureComponent exposure) {
        float factor = ExposureRules.speedFactor(exposure.temperature, exposure.pressure);
        if (factor < 1f) {
            event.multiply(factor);
        }
    }
}
