// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.joml.Vector2i;
import org.terasology.coresamplegameplay.crafting.RecipeView;
import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.HorizontalAlign;
import org.terasology.nui.VerticalAlign;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.itemRendering.AbstractItemRenderer;

/**
 * A recipe row: what it makes, and under it what it takes. Two lines rather than the design's two columns —
 * at 1280 pixels a panel is too narrow to hold "Plastron en rondins" and its three ingredients side by side.
 * A recipe the character cannot make yet keeps its place in the list, muted.
 */
public class RecipeRenderer extends AbstractItemRenderer<RecipeView> {
    private static final Color NAME = new Color(0xFFF3DCFF);
    private static final Color NAME_UNAVAILABLE = new Color(0xB49E7CFF);
    private static final Color SUMMARY = new Color(0xB49E7CFF);
    private static final int GAP = 2;

    @Override
    public void draw(RecipeView value, Canvas canvas) {
        Rectanglei region = canvas.getRegion();
        Font name = font("engine:Grenze-Body");
        Font summary = font("engine:Grenze-Small");
        int nameHeight = name.getLineHeight();
        canvas.drawTextRaw(value.getName(), name, value.isCraftable() ? NAME : NAME_UNAVAILABLE,
                new Rectanglei(region.minX, region.minY, region.maxX, region.minY + nameHeight),
                HorizontalAlign.LEFT, VerticalAlign.TOP);
        canvas.drawTextRaw(value.getSummary(), summary, SUMMARY,
                new Rectanglei(region.minX, region.minY + nameHeight + GAP, region.maxX, region.maxY),
                HorizontalAlign.LEFT, VerticalAlign.TOP);
    }

    @Override
    public Vector2i getPreferredSize(RecipeView value, Canvas canvas) {
        Font name = font("engine:Grenze-Body");
        Font summary = font("engine:Grenze-Small");
        return new Vector2i(Math.max(name.getWidth(value.getName()), summary.getWidth(value.getSummary())),
                name.getLineHeight() + GAP + summary.getLineHeight());
    }

    private static Font font(String urn) {
        return Assets.getFont(urn).orElseThrow(() -> new IllegalStateException("Missing interface font " + urn));
    }
}
