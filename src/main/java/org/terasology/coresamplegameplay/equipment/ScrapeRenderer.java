// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.joml.Vector3i;
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
 * Shows how far a pass of scraping has gone, with the same marks as a block being broken — the way Health's
 * {@code BlockDamageRenderer} shows damage. Without it, holding the button would look like doing nothing.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class ScrapeRenderer extends BaseComponentSystem implements RenderSystem {
    private static final String EFFECTS = "CoreAssets:blockDamageEffects#";

    @In
    private EntityManager entityManager;

    private BlockSelectionRenderer selectionRenderer;

    @Override
    public void renderOverlay() {
        Multimap<Integer, Vector3i> byEffect = ArrayListMultimap.create();
        for (EntityRef entity : entityManager.getEntitiesWith(ScrapingComponent.class, BlockComponent.class)) {
            ScrapingComponent scraping = entity.getComponent(ScrapingComponent.class);
            if (scraping.progress <= 0 || scraping.hardness <= 0) {
                continue;
            }
            int effect = Math.max(1, Math.min(10, Math.round(10f * scraping.progress / scraping.hardness)));
            byEffect.put(effect, entity.getComponent(BlockComponent.class).getPosition(new Vector3i()));
        }
        if (byEffect.isEmpty()) {
            return;
        }
        if (selectionRenderer == null) {
            selectionRenderer = new BlockSelectionRenderer(Assets.getTextureRegion(EFFECTS + 1).get().getTexture());
        }
        selectionRenderer.beginRenderOverlay();
        for (Integer effect : byEffect.keySet()) {
            Optional<TextureRegionAsset> texture = Assets.getTextureRegion(EFFECTS + effect);
            if (texture.isPresent()) {
                selectionRenderer.setEffectsTexture(texture.get());
                for (Vector3i position : byEffect.get(effect)) {
                    selectionRenderer.renderMark(position);
                }
            }
        }
        selectionRenderer.endRenderOverlay();
    }
}
