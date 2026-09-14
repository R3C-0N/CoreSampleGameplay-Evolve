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
 * widening on its own, then fill with loose soil. The four frames of opening are spread evenly over a pass, so a
 * slower shovel digs them slower; the filling lasts from the end of the pass to the start of the next. On letting
 * go the holes are gone at once, and the next scraping opens them again from the first frame.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ScrapeRenderer extends BaseComponentSystem implements RenderSystem {
    private static final String MARKS = "CoreSampleGameplay:scrapeMarks#";
    private static final int OPENING_FRAMES = 4;
    private static final int FILLING_FRAME = 5;

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
            byFrame.put(frameOf(scraping, now), entity.getComponent(BlockComponent.class).getPosition(new Vector3i()));
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

    private static int frameOf(ScrapingComponent scraping, long now) {
        if (scraping.passEndTime > 0 && scraping.passEndTime >= scraping.passStartTime) {
            return FILLING_FRAME;
        }
        float done = scraping.passLength > 0 ? (float) (now - scraping.passStartTime) / scraping.passLength : 1f;
        return 1 + Math.max(0, Math.min(OPENING_FRAMES - 1, (int) (done * OPENING_FRAMES)));
    }
}
