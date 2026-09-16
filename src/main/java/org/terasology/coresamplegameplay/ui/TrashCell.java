// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.joml.Vector2i;
import org.terasology.engine.utilities.Assets;
import org.terasology.input.MouseInput;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.BaseInteractionListener;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.InteractionListener;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.events.NUIMouseClickEvent;

/**
 * The bin, drawn rather than imported: a lid, a handle and a ribbed body, laid out in the cell with plain
 * rectangles. A texture would have meant a new asset for six shapes.
 * <p>
 * The cell reports the click and nothing more. Whether it destroys what is on the cursor or empties the whole
 * inventory is decided by {@code CreativeScreen}, which alone knows what the cursor is carrying.
 */
public class TrashCell extends CoreWidget {
    private static final Color METAL = new Color(0xB49E7CFF);
    private static final Color SHADOW = new Color(0x20120AFF);

    private Runnable action;

    private final InteractionListener listener = new BaseInteractionListener() {
        @Override
        public boolean onMouseClick(NUIMouseClickEvent event) {
            if (action == null || event.getMouseButton() != MouseInput.MOUSE_LEFT) {
                return false;
            }
            action.run();
            return true;
        }
    };

    public void setAction(Runnable newAction) {
        action = newAction;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rectanglei region = canvas.getRegion();
        Assets.getTextureRegion("engine:white").ifPresent(white -> {
            int w = region.lengthX();
            int h = region.lengthY();
            // Proportions of the cell, so the bin follows whatever size the skin gives it.
            box(canvas, white, region, w * 40 / 100, h * 18 / 100, w * 60 / 100, h * 26 / 100, METAL);
            box(canvas, white, region, w * 20 / 100, h * 26 / 100, w * 80 / 100, h * 36 / 100, METAL);
            box(canvas, white, region, w * 26 / 100, h * 40 / 100, w * 74 / 100, h * 82 / 100, METAL);
            for (int rib = 0; rib < 3; rib++) {
                int x = w * (36 + rib * 14) / 100;
                box(canvas, white, region, x, h * 48 / 100, x + Math.max(2, w / 24), h * 74 / 100, SHADOW);
            }
        });
        canvas.addInteractionRegion(listener, "Vider l'inventaire — ou y jeter ce qu'on tient", region);
    }

    private static void box(Canvas canvas, org.terasology.nui.UITextureRegion white, Rectanglei region,
                            int left, int top, int right, int bottom, Color color) {
        canvas.drawTextureRaw(white,
                new Rectanglei(region.minX + left, region.minY + top, region.minX + right, region.minY + bottom),
                color, ScaleMode.STRETCH);
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return new Vector2i(sizeHint);
    }

    @Override
    public float getTooltipDelay() {
        return 0;
    }
}
