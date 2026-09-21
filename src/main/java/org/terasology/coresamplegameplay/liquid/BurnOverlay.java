// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

import org.terasology.engine.core.Time;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.layers.hud.CoreHudWidget;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;

/**
 * The veil over the screen of a burning player: a wash of orange that breathes, brighter the hotter the
 * liquid.
 * <p>
 * It needs no texture. {@code Canvas.drawFilledRectangle} over the whole region does the job, which is one
 * fewer asset to draw and one fewer thing to get wrong in two themes.
 */
public class BurnOverlay extends CoreHudWidget {

    private static final int BASE_ALPHA = 40;
    private static final int ALPHA_PER_DEGREE = 9;
    private static final float PULSE_HZ = 2.2f;
    private static final float PULSE_DEPTH = 0.25f;

    @In
    private LocalPlayer localPlayer;
    @In
    private Time time;

    @Override
    public void initialise() {
    }

    @Override
    public void onDraw(Canvas canvas) {
        int warmth = warmth();
        if (warmth <= 0) {
            return;
        }
        float pulse = 1f + PULSE_DEPTH
                * (float) Math.sin(time.getGameTime() * PULSE_HZ * 2f * Math.PI);
        int over = warmth - LiquidBurnAuthoritySystem.FLAME_THRESHOLD;
        int alpha = Math.round((BASE_ALPHA + over * ALPHA_PER_DEGREE) * pulse);
        canvas.drawFilledRectangle(canvas.getRegion(),
                new Color(255, 96, 24, Math.min(255, Math.max(0, alpha))));
    }

    /**
     * @return the warmth burning the local player, nought if it is not burning
     */
    private int warmth() {
        if (localPlayer == null) {
            return 0;
        }
        BurningComponent burning = localPlayer.getCharacterEntity().getComponent(BurningComponent.class);
        return burning == null ? 0 : burning.warmth;
    }
}
