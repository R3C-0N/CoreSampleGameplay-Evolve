// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.module.inventory.ui.InventoryCell;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.HorizontalAlign;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.VerticalAlign;

/**
 * The iron slot of the design system: the skin draws the well, this draws what depends on the content.
 * <ul>
 *     <li>an empty slot shows a small hairline square at its centre;</li>
 *     <li>a filled one gets a one-texel edge in its rarity colour — rarity never tints the well itself;</li>
 *     <li>a toolbar slot carries its key in the top-left corner.</li>
 * </ul>
 * The skin must give this cell no margin: the edge is drawn over the whole region.
 */
public class SlotCell extends InventoryCell {
    /** Every item is common until items carry a rarity. */
    private static final Color RARITY_COMMON = new Color(0x9AA0A6FF);
    private static final Color EMPTY_MARK = new Color(0x20120A99);
    private static final Color HOTKEY = new Color(0xB49E7CFF);
    private static final Color HOTKEY_SHADOW = new Color(0x20120AFF);
    private static final int TEXEL = 3;

    @LayoutConfig
    private String hotkey = "";

    public void setHotkey(String hotkey) {
        this.hotkey = hotkey == null ? "" : hotkey;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Rectanglei region = canvas.getRegion();
        if (getTargetItem().exists()) {
            if (!isSelected()) {
                Assets.getTextureRegion("engine:slotEdge")
                        .ifPresent(edge -> canvas.drawTextureRaw(edge, region, RARITY_COMMON, ScaleMode.STRETCH));
            }
        } else {
            int size = 4 * TEXEL;
            int x = region.minX + (region.lengthX() - size) / 2;
            int y = region.minY + (region.lengthY() - size) / 2;
            Assets.getTextureRegion("engine:white").ifPresent(white ->
                    canvas.drawTextureRaw(white, new Rectanglei(x, y, x + size, y + size), EMPTY_MARK, ScaleMode.STRETCH));
        }
        if (!hotkey.isEmpty()) {
            Assets.getFont("engine:Grenze-Small").ifPresent(font -> canvas.drawTextRawShadowed(hotkey, font, HOTKEY,
                    HOTKEY_SHADOW, new Rectanglei(region.minX + 2 * TEXEL, region.minY + TEXEL, region.maxX, region.maxY),
                    HorizontalAlign.LEFT, VerticalAlign.TOP));
        }
    }
}
