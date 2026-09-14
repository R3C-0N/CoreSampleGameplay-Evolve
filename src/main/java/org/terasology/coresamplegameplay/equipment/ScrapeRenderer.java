// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.joml.Vector3i;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.RenderSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.assets.texture.TextureRegionAsset;
import org.terasology.engine.rendering.world.selection.BlockSelectionRenderer;
import org.terasology.engine.utilities.Assets;
import org.terasology.engine.world.block.BlockComponent;

import java.util.Optional;

/**
 * Shows a block being dug into, never the cracks of one being broken: up to four holes open in the face, each
 * widening on its own, then fill with loose soil. Five frames of 110 ms, looped for as long as the scraping lasts;
 * on letting go the holes are gone at once, and the next scraping opens them again from the first frame.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ScrapeRenderer extends BaseComponentSystem implements RenderSystem {
    private static final String MARKS = "CoreSampleGameplay:scrapeMarks#";
    private static final int FRAMES = 5;
    private static final long FRAME_MS = 110;

    @In
    private EntityManager entityManager;
    @In
    private Time time;

    private BlockSelectionRenderer selectionRenderer;

    @Override
    public void renderOverlay() {
        long now = time.getGameTimeInMs();
        Multimap<Integer, Vector3i> byFrame = ArrayListMultimap.create();
        for (EntityRef entity : entityManager.getEntitiesWith(ScrapingComponent.class, BlockComponent.class)) {
            ScrapingComponent scraping = entity.getComponent(ScrapingComponent.class);
            if (now - scraping.lastScrapeTime > ScrapeAuthoritySystem.STILL_SCRAPING_MS) {
                continue;
            }
            int frame = 1 + (int) (Math.max(0, now - scraping.startTime) / FRAME_MS % FRAMES);
            byFrame.put(frame, entity.getComponent(BlockComponent.class).getPosition(new Vector3i()));
        }
        if (byFrame.isEmpty()) {
            return;
        }
        if (selectionRenderer == null) {
            selectionRenderer = new BlockSelectionRenderer(Assets.getTextureRegion(MARKS + 1).get().getTexture());
        }
        selectionRenderer.beginRenderOverlay();
        for (Integer frame : byFrame.keySet()) {
            Optional<TextureRegionAsset> texture = Assets.getTextureRegion(MARKS + frame);
            if (texture.isPresent()) {
                selectionRenderer.setEffectsTexture(texture.get());
                for (Vector3i position : byFrame.get(frame)) {
                    selectionRenderer.renderMark(position);
                }
            }
        }
        selectionRenderer.endRenderOverlay();
    }
}
