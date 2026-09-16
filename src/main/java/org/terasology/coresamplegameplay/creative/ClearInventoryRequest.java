// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.network.ServerEvent;
import org.terasology.gestalt.entitysystem.event.Event;

/**
 * The bin. Emptying an inventory is a change to it, and changes to inventories belong to the server.
 */
@ServerEvent
public class ClearInventoryRequest implements Event {
    private boolean everything;

    protected ClearInventoryRequest() {
    }

    public ClearInventoryRequest(boolean everything) {
        this.everything = everything;
    }

    /** True empties every slot; false destroys only what is held on the cursor. */
    public boolean isEverything() {
        return everything;
    }
}
