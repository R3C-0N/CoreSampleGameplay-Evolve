// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.terasology.engine.network.OwnerEvent;
import org.terasology.gestalt.entitysystem.event.Event;

/**
 * Sent by the server to the character who activated a station, so that its own client opens the
 * crafting list. Activation is decided on the server; screens only exist on the client.
 */
@OwnerEvent
public class OpenStationEvent implements Event {
}
