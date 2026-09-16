// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.network.ServerEvent;
import org.terasology.gestalt.entitysystem.event.Event;

/**
 * A double tap on the jump key. The movement mode is the server's to set — {@code SetMovementModeEvent} is
 * only ever received on the authority — so the gesture is read on the client and the decision asked for here.
 */
@ServerEvent
public class ToggleFlightRequest implements Event {
}
