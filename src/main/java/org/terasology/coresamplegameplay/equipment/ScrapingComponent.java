// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.network.Replicate;
import org.terasology.engine.world.block.ForceBlockActive;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * How far a shovel has scraped a soil block. Lives on the block entity while someone scrapes it, and keeps that
 * entity from being cleaned up as temporary in the meantime.
 * <p>
 * Everything is replicated: the client draws the marks, and moves the shovel of whoever is scraping.
 */
@ForceBlockActive
public class ScrapingComponent implements Component<ScrapingComponent> {
    /** Strength put in since the last pass, out of {@link #hardness}. */
    @Replicate
    public int progress;

    @Replicate
    public int hardness;

    /** Game time the scraping began, without letting go: the holes open from there. */
    @Replicate
    public long startTime;

    /** Game time of the last use on this block, pauses included. */
    @Replicate
    public long lastScrapeTime;

    /** Game time the last pass ended; the next one waits a short pause after it. */
    @Replicate
    public long passEndTime;

    @Replicate
    public EntityRef scraper = EntityRef.NULL;

    @Override
    public void copyFrom(ScrapingComponent other) {
        this.progress = other.progress;
        this.hardness = other.hardness;
        this.startTime = other.startTime;
        this.lastScrapeTime = other.lastScrapeTime;
        this.passEndTime = other.passEndTime;
        this.scraper = other.scraper;
    }
}
