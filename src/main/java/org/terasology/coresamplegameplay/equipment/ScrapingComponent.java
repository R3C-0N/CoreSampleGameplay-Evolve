// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * How far a shovel has scraped a soil block. Lives on the block entity while someone scrapes it, and keeps that
 * entity from being cleaned up as temporary in the meantime.
 */
@ForceBlockActive
public class ScrapingComponent implements Component<ScrapingComponent> {
    /** Strength put in since the last pass, out of {@link #hardness}. */
    @Replicate
    public int progress;

    @Replicate
    public int hardness;

    public long lastScrapeTime;

    @Override
    public void copyFrom(ScrapingComponent other) {
        this.progress = other.progress;
        this.hardness = other.hardness;
        this.lastScrapeTime = other.lastScrapeTime;
    }
}
