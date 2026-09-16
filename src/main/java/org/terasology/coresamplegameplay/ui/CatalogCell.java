// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.joml.Vector2i;
import org.terasology.coresamplegameplay.creative.CatalogEntry;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.rendering.assets.texture.TextureRegion;
import org.terasology.engine.utilities.Assets;
import org.terasology.input.Keyboard;
import org.terasology.input.MouseInput;
import org.terasology.module.inventory.ui.ItemIcon;
import org.terasology.nui.BaseInteractionListener;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.InteractionListener;
import org.terasology.nui.events.NUIMouseClickEvent;

import java.util.function.BiConsumer;

/**
 * One shelf slot of the catalogue: what an entry looks like, and what clicking it asks for.
 * <p>
 * Unlike {@code SlotCell} it descends from no inventory cell, because it points at nothing in an inventory —
 * an entry is a prefab or a block family, and the stack it stands for does not exist until it is asked for.
 * Cells are pooled and re-pointed by {@link CatalogGrid}, so one of them serves many entries over its life.
 */
public class CatalogCell extends CoreWidget {
    private final ItemIcon icon = new ItemIcon();

    private CatalogEntry entry;
    private BiConsumer<CatalogEntry, Boolean> picker;

    private final InteractionListener listener = new BaseInteractionListener() {
        @Override
        public boolean onMouseClick(NUIMouseClickEvent event) {
            if (entry == null || picker == null || event.getMouseButton() != MouseInput.MOUSE_LEFT) {
                return false;
            }
            boolean single = event.getKeyboard().isKeyDown(Keyboard.Key.LEFT_SHIFT.getId())
                    || event.getKeyboard().isKeyDown(Keyboard.Key.RIGHT_SHIFT.getId());
            picker.accept(entry, !single);
            return true;
        }
    };

    /** Re-points the cell. The icon is rebuilt only when the entry actually changes. */
    public void show(CatalogEntry newEntry, BiConsumer<CatalogEntry, Boolean> newPicker) {
        this.picker = newPicker;
        if (newEntry == entry) {
            return;
        }
        entry = newEntry;
        icon.setIcon(null);
        icon.setMesh(null);
        if (entry == null) {
            return;
        }
        if (entry.isBlock()) {
            icon.setMesh(entry.getBlockFamily().getArchetypeBlock().getMeshGenerator().getStandaloneMesh());
            Assets.getTexture("engine:terrain").ifPresent(icon::setMeshTexture);
        } else {
            ItemComponent item = entry.getItemPrefab().getComponent(ItemComponent.class);
            if (item != null && item.icon != null) {
                icon.setIcon((TextureRegion) item.icon);
            } else {
                Assets.getTextureRegion("engine:items#questionMark").ifPresent(icon::setIcon);
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (entry == null) {
            return;
        }
        // The well behind the icon comes from the skin: the canvas paints it before calling this.
        canvas.drawWidget(icon);
        canvas.addInteractionRegion(listener, entry.getName(), canvas.getRegion());
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return canvas.calculateRestrictedSize(icon, sizeHint);
    }

    @Override
    public float getTooltipDelay() {
        return 0;
    }
}
