// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.durability.components.DurabilityComponent;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.rendering.assets.texture.Texture;
import org.terasology.engine.rendering.assets.texture.TextureUtil;
import org.terasology.engine.utilities.Assets;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.module.inventory.ui.GetItemTooltip;
import org.terasology.module.inventory.ui.InventoryCellRendered;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.widgets.TooltipLine;

/**
 * Says the wear in French, and shows a broken item for what it is. The bar under the icon is the Durability
 * module's; its English tooltip line is replaced here.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class WearClientSystem extends BaseComponentSystem {
    private static final String MODULE_LINE = "Durability:";
    private static final Color BROKEN_TEXT = new Color(220, 70, 60);
    private static final Color BROKEN_VEIL = new Color(0, 0, 0, 140);

    /** After the module's own line, which it is here to replace. */
    @Priority(EventPriority.PRIORITY_TRIVIAL)
    @ReceiveEvent
    public void sayWear(GetItemTooltip event, EntityRef item, DurabilityComponent durability) {
        event.getTooltipLines().removeIf(line -> line.getText() != null && line.getText().startsWith(MODULE_LINE));
        if (durability.durability <= 0) {
            event.getTooltipLines().add(new TooltipLine("Cassé — à réparer à son atelier", BROKEN_TEXT));
        } else {
            event.getTooltipLines().add(new TooltipLine("Solidité : " + durability.durability + " / "
                    + durability.maxDurability, Color.WHITE));
        }
    }

    @ReceiveEvent
    public void veilBroken(InventoryCellRendered event, EntityRef item, DurabilityComponent durability) {
        if (durability.durability > 0) {
            return;
        }
        Canvas canvas = event.getCanvas();
        Assets.get(TextureUtil.getTextureUriForColor(BROKEN_VEIL), Texture.class).ifPresent(veil ->
                canvas.drawTexture(veil, new Rectanglei(0, 0).setSize(canvas.size().x, canvas.size().y)));
    }
}
