// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

import org.terasology.engine.core.Time;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.assets.texture.Texture;
import org.terasology.engine.rendering.assets.texture.TextureUtil;
import org.terasology.engine.rendering.nui.layers.hud.CoreHudWidget;
import org.terasology.engine.utilities.Assets;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;

import java.util.Optional;

/**
 * The veil over the screen of a burning player: a wash of orange that breathes, stronger the hotter the
 * liquid.
 * <p>
 * It draws a one pixel colour texture stretched over the whole region rather than calling
 * {@code drawFilledRectangle}, which paints nothing at all from a heads-up element - not even opaque red
 * over the entire screen. The texture is procedural, so this costs no asset either way.
 */
public class BurnOverlay extends CoreHudWidget {

    private static final Color FIRE = new Color(255, 96, 24, 255);
    private static final float BASE_ALPHA = 0.13f;
    private static final float ALPHA_PER_DEGREE = 0.028f;
    private static final float PULSE_HZ = 2.2f;
    private static final float PULSE_DEPTH = 0.25f;

    @In
    private LocalPlayer localPlayer;
    @In
    private Time time;

    private Texture fire;

    @Override
    public void initialise() {
    }

    @Override
    public void onDraw(Canvas canvas) {
        int warmth = warmth();
        if (warmth <= 0) {
            return;
        }
        if (fire == null) {
            Optional<Texture> loaded = Assets.getTexture(TextureUtil.getTextureUriForColor(FIRE).toString());
            if (loaded.isEmpty()) {
                return;
            }
            fire = loaded.get();
        }
        float pulse = 1f + PULSE_DEPTH * (float) Math.sin(time.getGameTime() * PULSE_HZ * 2f * Math.PI);
        int over = warmth - LiquidBurnAuthoritySystem.FLAME_THRESHOLD;
        float alpha = Math.min(0.75f, (BASE_ALPHA + over * ALPHA_PER_DEGREE) * pulse);

        canvas.setAlpha(alpha);
        canvas.drawTexture(fire, canvas.getRegion());
        canvas.setAlpha(1f);
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
