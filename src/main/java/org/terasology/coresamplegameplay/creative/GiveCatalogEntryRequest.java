// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.network.ServerEvent;
import org.terasology.gestalt.entitysystem.event.Event;

/**
 * A click in the catalogue: the client names what it wants, the server decides whether it gets it.
 * <p>
 * Whether the uri names a block family or an item prefab is carried rather than guessed — a block family and
 * an item prefab are both spelled {@code Module:Name}, and only the catalogue knows which it built.
 */
@ServerEvent
public class GiveCatalogEntryRequest implements Event {
    private String uri;
    private boolean block;
    private boolean wholeStack;

    protected GiveCatalogEntryRequest() {
    }

    public GiveCatalogEntryRequest(String uri, boolean block, boolean wholeStack) {
        this.uri = uri;
        this.block = block;
        this.wholeStack = wholeStack;
    }

    public String getUri() {
        return uri;
    }

    public boolean isBlock() {
        return block;
    }

    /** A plain click asks for a full stack; a shift-click for a single one. */
    public boolean isWholeStack() {
        return wholeStack;
    }
}
