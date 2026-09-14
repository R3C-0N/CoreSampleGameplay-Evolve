// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.joml.Vector2i;
import org.terasology.engine.rendering.assets.texture.TextureRegionAsset;
import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.HorizontalAlign;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.VerticalAlign;
import org.terasology.nui.asset.font.Font;
import org.terasology.nui.databinding.Binding;
import org.terasology.nui.databinding.DefaultBinding;

import java.util.Locale;
import java.util.Optional;

/**
 * The resource globe of the design system: a riveted iron ring around a flat liquid. There is no curved glass —
 * the light is in steps, baked into the ring. The liquid is one white disc, cut from the bottom to the level
 * and tinted by the resource, so a single texture serves health, stamina and mana.
 */
public class ResourceOrb extends CoreWidget {
    private static final Color HEALTH = new Color(0xC13B27FF);
    private static final Color STAMINA = new Color(0x7CC452FF);
    private static final Color MANA = new Color(0x48C8F5FF);
    private static final Color TEXT = new Color(0xFFF3DCFF);
    private static final Color MUTED = new Color(0xB49E7CFF);
    private static final Color SHADOW = new Color(0x20120AFF);
    private static final int LABEL_GAP = 4;

    /** {@code health}, {@code stamina} or {@code mana}. */
    @LayoutConfig
    private String kind = "health";

    @LayoutConfig
    private String label = "";

    /** The smaller calibre: redrawn at its size, never the large one scaled down. */
    @LayoutConfig
    private boolean small;

    private Binding<Float> value = new DefaultBinding<>(0f);
    private Binding<Float> max = new DefaultBinding<>(100f);

    public void bindValue(Binding<Float> binding) {
        value = binding;
    }

    public void setValue(float val) {
        value.set(val);
    }

    public void bindMax(Binding<Float> binding) {
        max = binding;
    }

    public void setMax(float val) {
        max.set(val);
    }

    @Override
    public void onDraw(Canvas canvas) {
        String calibre = small ? "Small" : "";
        Optional<TextureRegionAsset> track = Assets.getTextureRegion("engine:orbTrack" + calibre);
        Optional<TextureRegionAsset> fill = Assets.getTextureRegion("engine:orbFill" + calibre);
        Optional<TextureRegionAsset> ring = Assets.getTextureRegion("engine:orbRing" + calibre);
        if (track.isEmpty() || fill.isEmpty() || ring.isEmpty()) {
            return;
        }
        Rectanglei region = canvas.getRegion();
        int size = track.get().getWidth();
        int x = region.minX + (region.lengthX() - size) / 2;
        Rectanglei orb = new Rectanglei(x, region.minY, x + size, region.minY + size);

        canvas.drawTextureRaw(track.get(), orb, ScaleMode.STRETCH);
        float level = level();
        if (level > 0) {
            int top = orb.maxY - Math.round(size * level);
            canvas.drawTextureRaw(fill.get(), new Rectanglei(orb.minX, top, orb.maxX, orb.maxY), tint(),
                    ScaleMode.STRETCH, 0f, 1f - level, 1f, level);
        }
        canvas.drawTextureRaw(ring.get(), orb, ScaleMode.STRETCH);

        Font number = font(small ? "engine:Grenze-Small" : "engine:Grenze-Strong");
        canvas.drawTextRawShadowed(String.valueOf(Math.round(value.get())), number, TEXT, SHADOW,
                new Rectanglei(orb.minX, orb.minY, orb.maxX, orb.maxY - size / 8),
                HorizontalAlign.CENTER, VerticalAlign.BOTTOM);
        if (!label.isEmpty()) {
            Font caps = font("engine:Grenze-Caps");
            canvas.drawTextRawShadowed(label.toUpperCase(Locale.FRANCE), caps, MUTED, SHADOW,
                    new Rectanglei(region.minX, orb.maxY + LABEL_GAP, region.maxX, region.maxY),
                    HorizontalAlign.CENTER, VerticalAlign.TOP);
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        int size = Assets.getTextureRegion("engine:orbTrack" + (small ? "Small" : "")).map(TextureRegionAsset::getWidth).orElse(66);
        if (label.isEmpty()) {
            return new Vector2i(size, size);
        }
        Font caps = font("engine:Grenze-Caps");
        return new Vector2i(Math.max(size, caps.getWidth(label.toUpperCase(Locale.FRANCE))),
                size + LABEL_GAP + caps.getLineHeight());
    }

    private float level() {
        float maximum = max.get();
        return maximum <= 0 ? 0f : Math.max(0f, Math.min(1f, value.get() / maximum));
    }

    private Color tint() {
        switch (kind) {
            case "stamina":
                return STAMINA;
            case "mana":
                return MANA;
            default:
                return HEALTH;
        }
    }

    private static Font font(String urn) {
        return Assets.getFont(urn).orElseThrow(() -> new IllegalStateException("Missing interface font " + urn));
    }
}
