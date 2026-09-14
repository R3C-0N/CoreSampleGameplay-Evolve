// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.players.FirstPersonHeldItemAnimationEvent;
import org.terasology.engine.registry.In;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

/**
 * Moves the shovel like a scraper while its holder scrapes: dragged back and forth, blade down, instead of the
 * swing of a hit. At the end of a pass it lifts and shakes off, for the length of the pause.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ScrapeAnimationClientSystem extends BaseComponentSystem {
    /** Uses come every cooldown; past this without one, the hand is at rest again. */
    private static final long STILL_SCRAPING_MS = 450;
    private static final float STROKE_MS = 380f;

    @In
    private EntityManager entityManager;
    @In
    private Time time;

    @ReceiveEvent
    public void scrapeMotion(FirstPersonHeldItemAnimationEvent event, EntityRef character) {
        long now = time.getGameTimeInMs();
        ScrapingComponent latest = null;
        for (EntityRef block : entityManager.getEntitiesWith(ScrapingComponent.class)) {
            ScrapingComponent scraping = block.getComponent(ScrapingComponent.class);
            if (character.equals(scraping.scraper) && (latest == null || scraping.lastScrapeTime > latest.lastScrapeTime)) {
                latest = scraping;
            }
        }
        if (latest == null || now - latest.lastScrapeTime > STILL_SCRAPING_MS) {
            return;
        }

        long sinceEnd = now - latest.passEndTime;
        if (latest.passEndTime > 0 && sinceEnd < ScrapeAuthoritySystem.PAUSE_MS) {
            float lift = (float) Math.sin(Math.PI * sinceEnd / ScrapeAuthoritySystem.PAUSE_MS);
            float shake = (float) Math.sin(6 * Math.PI * sinceEnd / ScrapeAuthoritySystem.PAUSE_MS);
            event.setPitch(-18f * lift);
            event.setYaw(8f * shake * lift);
            event.getOffset().set(0.04f * shake * lift, 0.12f * lift, 0f);
        } else {
            double phase = 2 * Math.PI * (now % (long) STROKE_MS) / STROKE_MS;
            float drag = (float) Math.sin(phase);
            event.setPitch(30f + 6f * (float) Math.cos(phase));
            event.setYaw(-10f * drag);
            event.getOffset().set(0.14f * drag, -0.18f, -0.08f * Math.abs(drag));
        }
    }
}
