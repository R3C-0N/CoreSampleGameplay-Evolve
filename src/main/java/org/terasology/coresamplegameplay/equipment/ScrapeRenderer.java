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
 * Shows how far a pass of scraping has gone: grooves raked across the face, one after another, never the cracks
 * of a block being broken. The grooves stay full through the pause that ends a pass, then are wiped.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ScrapeRenderer extends BaseComponentSystem implements RenderSystem {
    private static final String MARKS = "CoreSampleGameplay:scrapeMarks#";
    private static final int FULL = 10;

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
            int frame;
            if (scraping.passEndTime > 0 && now - scraping.passEndTime < ScrapeAuthoritySystem.PAUSE_MS) {
                frame = FULL;
            } else if (scraping.progress > 0 && scraping.hardness > 0) {
                frame = Math.max(1, Math.min(FULL - 1, Math.round((float) FULL * scraping.progress / scraping.hardness)));
            } else {
                continue;
            }
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
