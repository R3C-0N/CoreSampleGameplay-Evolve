// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.terasology.engine.core.Time;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.layers.hud.CoreHudWidget;
import org.terasology.engine.utilities.Assets;
import org.terasology.joml.geom.Rectanglei;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.HorizontalAlign;
import org.terasology.nui.ScaleMode;
import org.terasology.nui.UITextureRegion;
import org.terasology.nui.VerticalAlign;

import java.util.Optional;

/**
 * The two gauges at the top of the screen, and the four veils.
 * <p>
 * Each of the four states has its own look, so a player knows what is happening without reading the gauge:
 * <ul>
 *     <li>heat, an orange wash that shimmers;</li>
 *     <li>cold, frost creeping in from the edges;</li>
 *     <li>the deep, the screen closing in to a dark tunnel;</li>
 *     <li>the thin air, the screen washing out to white.</li>
 * </ul>
 * A veil starts half way up its gauge and is at its strongest when the damage starts.
 * <p>
 * Everything is drawn from the engine's one pixel white texture, tinted, like the bin of the inventory: the
 * overlay costs no asset, and {@code drawFilledRectangle} paints nothing from a heads-up element.
 */
public class ExposureOverlay extends CoreHudWidget {

    private static final Color HEAT = new Color(255, 120, 30, 255);
    private static final Color FROST = new Color(205, 232, 255, 255);
    private static final Color DEEP = new Color(4, 6, 20, 255);
    private static final Color THIN_AIR = new Color(250, 250, 255, 255);

    private static final Color TRACK = new Color(20, 20, 24, 255);
    private static final Color MARK = new Color(230, 230, 230, 255);
    private static final Color COLD_FILL = new Color(110, 180, 255, 255);
    private static final Color HEAT_FILL = new Color(255, 120, 30, 255);
    private static final Color DEEP_FILL = new Color(70, 60, 200, 255);
    private static final Color THIN_FILL = new Color(235, 235, 245, 255);
    private static final Color LABEL = new Color(240, 240, 240, 255);
    private static final Color LABEL_SHADOW = new Color(0, 0, 0, 200);

    /** How many bands an edge veil is made of: enough that the steps do not show. */
    private static final int BANDS = 12;

    private static final int GAUGE_WIDTH = 220;
    private static final int GAUGE_HEIGHT = 8;
    private static final int GAUGE_TOP = 18;
    private static final int GAUGE_GAP = 24;
    private static final int LABEL_WIDTH = 110;

    @In
    private LocalPlayer localPlayer;
    @In
    private Time time;

    @Override
    public void initialise() {
    }

    @Override
    public void onDraw(Canvas canvas) {
        ExposureComponent exposure = exposure();
        if (exposure == null) {
            return;
        }
        Optional<UITextureRegion> white = Assets.getTextureRegion("engine:white").map(region -> region);
        if (white.isEmpty()) {
            return;
        }
        float seconds = time.getGameTime();
        Rectanglei screen = canvas.getRegion();

        float temperature = ExposureRules.veil(exposure.temperature);
        if (exposure.temperature > 0f && temperature > 0f) {
            float shimmer = 1f + 0.18f * (float) Math.sin(seconds * 7f * 2f * Math.PI);
            wash(canvas, white.get(), screen, HEAT, Math.min(0.6f, 0.32f * temperature * shimmer));
        } else if (exposure.temperature < 0f && temperature > 0f) {
            edges(canvas, white.get(), screen, FROST, 0.24f * temperature, 0.6f * temperature);
        }

        float pressure = ExposureRules.veil(exposure.pressure);
        if (exposure.pressure > 0f && pressure > 0f) {
            wash(canvas, white.get(), screen, DEEP, 0.25f * pressure);
            edges(canvas, white.get(), screen, DEEP, 0.38f * pressure, 0.9f * pressure);
        } else if (exposure.pressure < 0f && pressure > 0f) {
            float breath = 1f + 0.2f * (float) Math.sin(seconds * 0.6f * 2f * Math.PI);
            wash(canvas, white.get(), screen, THIN_AIR, Math.min(0.7f, 0.45f * pressure * breath));
        }

        if (exposure.exposed || exposure.temperature != 0f || exposure.pressure != 0f) {
            int y = screen.minY + GAUGE_TOP;
            gauge(canvas, white.get(), screen, y, "Température", exposure.temperature, COLD_FILL, HEAT_FILL);
            gauge(canvas, white.get(), screen, y + GAUGE_GAP, "Pression", exposure.pressure, THIN_FILL, DEEP_FILL);
        }
        canvas.setAlpha(1f);
    }

    private static void wash(Canvas canvas, UITextureRegion white, Rectanglei screen, Color color, float alpha) {
        canvas.setAlpha(alpha);
        canvas.drawTextureRaw(white, screen, color, ScaleMode.STRETCH);
    }

    /**
     * A veil creeping in from the four edges: {@link #BANDS} frames, each a little further in and fainter than
     * the one before, up to {@code depth} of the shorter side of the screen.
     */
    private static void edges(Canvas canvas, UITextureRegion white, Rectanglei screen, Color color, float depth,
                              float alpha) {
        int reach = (int) (Math.min(screen.lengthX(), screen.lengthY()) * depth);
        if (reach <= 0) {
            return;
        }
        for (int band = 0; band < BANDS; band++) {
            int outer = reach * band / BANDS;
            int inner = reach * (band + 1) / BANDS;
            if (inner <= outer) {
                continue;
            }
            canvas.setAlpha(alpha * (1f - band / (float) BANDS) / 2f);
            frame(canvas, white, screen, color, outer, inner);
        }
    }

    /** The ring between {@code outer} and {@code inner} pixels from the edges, as four rectangles. */
    private static void frame(Canvas canvas, UITextureRegion white, Rectanglei screen, Color color, int outer,
                              int inner) {
        int left = screen.minX;
        int top = screen.minY;
        int right = screen.maxX;
        int bottom = screen.maxY;
        canvas.drawTextureRaw(white, new Rectanglei(left + outer, top + outer, right - outer, top + inner), color,
                ScaleMode.STRETCH);
        canvas.drawTextureRaw(white, new Rectanglei(left + outer, bottom - inner, right - outer, bottom - outer),
                color, ScaleMode.STRETCH);
        canvas.drawTextureRaw(white, new Rectanglei(left + outer, top + inner, left + inner, bottom - inner), color,
                ScaleMode.STRETCH);
        canvas.drawTextureRaw(white, new Rectanglei(right - inner, top + inner, right - outer, bottom - inner), color,
                ScaleMode.STRETCH);
    }

    /**
     * One gauge: a track with a mark at its middle, filled from the middle leftwards for the cold or the thin
     * air and rightwards for the heat or the deep.
     */
    private static void gauge(Canvas canvas, UITextureRegion white, Rectanglei screen, int y, String label,
                              float value, Color low, Color high) {
        int centre = screen.minX + screen.lengthX() / 2;
        int left = centre - GAUGE_WIDTH / 2;
        int right = left + GAUGE_WIDTH;

        canvas.setAlpha(0.7f);
        canvas.drawTextureRaw(white, new Rectanglei(left - 2, y - 2, right + 2, y + GAUGE_HEIGHT + 2), TRACK,
                ScaleMode.STRETCH);

        int span = Math.round(GAUGE_WIDTH / 2f * Math.min(1f, Math.abs(value) / ExposureRules.GAUGE_MAX));
        if (span > 0) {
            canvas.setAlpha(Math.abs(value) >= ExposureRules.GAUGE_MAX ? 1f : 0.9f);
            Rectanglei fill = value < 0f
                    ? new Rectanglei(centre - span, y, centre, y + GAUGE_HEIGHT)
                    : new Rectanglei(centre, y, centre + span, y + GAUGE_HEIGHT);
            canvas.drawTextureRaw(white, fill, value < 0f ? low : high, ScaleMode.STRETCH);
        }

        canvas.setAlpha(1f);
        canvas.drawTextureRaw(white, new Rectanglei(centre - 1, y - 3, centre + 1, y + GAUGE_HEIGHT + 3), MARK,
                ScaleMode.STRETCH);
        Assets.getFont("engine:Grenze-Small").ifPresent(font -> canvas.drawTextRawShadowed(label, font, LABEL,
                LABEL_SHADOW, new Rectanglei(left - LABEL_WIDTH - 8, y - 6, left - 8, y + GAUGE_HEIGHT + 6),
                HorizontalAlign.RIGHT, VerticalAlign.MIDDLE));
    }

    private ExposureComponent exposure() {
        if (localPlayer == null) {
            return null;
        }
        return localPlayer.getCharacterEntity().getComponent(ExposureComponent.class);
    }
}
