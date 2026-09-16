// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.joml.Vector2i;
import org.terasology.coresamplegameplay.creative.CatalogEntry;
import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.BaseInteractionListener;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.InteractionListener;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.events.NUIMouseWheelEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The catalogue's shelves: a grid that draws only the rows it can show.
 * <p>
 * This is the one widget the creative mode could not borrow. {@code UIList} draws every entry it holds,
 * visible or not — {@code ScrollableArea} merely clips afterwards — and a good third of this catalogue is
 * blocks, each of which costs a mesh draw with its own render state. Four hundred of those per frame is not
 * payable; the twenty or so on screen are free.
 * <p>
 * Cells are pooled to the visible capacity and re-pointed each frame, so scrolling allocates nothing.
 */
public class CatalogGrid extends CoreWidget {
    /** Matches the slot art, as {@code SlotCell} does in the skin. */
    private static final int CELL = 48;
    private static final int GAP = 4;
    private static final int BAR_WIDTH = 6;

    private static final Color BAR_TRACK = new Color(0x20120A66);
    private static final Color BAR_THUMB = new Color(0xB49E7CFF);

    private final List<CatalogCell> pool = new ArrayList<>();

    private List<CatalogEntry> entries = List.of();
    private BiConsumer<CatalogEntry, Boolean> picker;
    private int firstRow;

    private final InteractionListener wheel = new BaseInteractionListener() {
        @Override
        public boolean onMouseWheel(NUIMouseWheelEvent event) {
            // A turn towards the user is negative, and moves the shelves up.
            firstRow = Math.max(0, firstRow - event.getWheelTurns());
            return true;
        }
    };

    /** Replaces what is shown and returns to the top: a new filter always starts at its first result. */
    public void setEntries(List<CatalogEntry> newEntries) {
        entries = newEntries == null ? List.of() : newEntries;
        firstRow = 0;
    }

    public void setPicker(BiConsumer<CatalogEntry, Boolean> newPicker) {
        picker = newPicker;
    }

    @Override
    public void onDraw(Canvas canvas) {
        Rectanglei region = canvas.getRegion();
        boolean scrollable = entries.size() > 0;
        int usableWidth = region.lengthX() - (scrollable ? BAR_WIDTH + GAP : 0);

        int columns = Math.max(1, (usableWidth + GAP) / (CELL + GAP));
        int rows = Math.max(1, (region.lengthY() + GAP) / (CELL + GAP));
        int totalRows = (entries.size() + columns - 1) / columns;

        // Clamped on draw rather than on scroll: the column count is only known here, and it changes with size.
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, totalRows - rows)));

        canvas.addInteractionRegion(wheel, region);

        int capacity = columns * rows;
        while (pool.size() < capacity) {
            pool.add(new CatalogCell());
        }

        for (int i = 0; i < capacity; i++) {
            int index = firstRow * columns + i;
            if (index >= entries.size()) {
                break;
            }
            CatalogCell cell = pool.get(i);
            cell.show(entries.get(index), picker);
            int x = region.minX + (i % columns) * (CELL + GAP);
            int y = region.minY + (i / columns) * (CELL + GAP);
            canvas.drawWidget(cell, new Rectanglei(x, y, x + CELL, y + CELL));
        }

        if (totalRows > rows) {
            drawScrollBar(canvas, region, firstRow, rows, totalRows);
        }
    }

    /** A plain indicator, not a control: the wheel scrolls, this only says where one is. */
    private static void drawScrollBar(Canvas canvas, Rectanglei region, int top, int rows, int totalRows) {
        int x = region.maxX - BAR_WIDTH;
        int height = region.lengthY();
        int thumbHeight = Math.max(16, height * rows / totalRows);
        int thumbTop = region.minY + (height - thumbHeight) * top / Math.max(1, totalRows - rows);
        Assets.getTextureRegion("engine:white").ifPresent(white -> {
            canvas.drawTextureRaw(white, new Rectanglei(x, region.minY, x + BAR_WIDTH, region.maxY),
                    BAR_TRACK, ScaleMode.STRETCH);
            canvas.drawTextureRaw(white, new Rectanglei(x, thumbTop, x + BAR_WIDTH, thumbTop + thumbHeight),
                    BAR_THUMB, ScaleMode.STRETCH);
        });
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return new Vector2i(sizeHint);
    }
}
