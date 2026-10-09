// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The numbers Mat asked for, checked by walking the gauge second by second as the game does.
 */
class ExposureRulesTest {

    private static final float TICK = 0.5f;

    /** Seconds until the damage starts under a constant exposure. */
    private static float secondsToDamage(float exposure) {
        float gauge = 0f;
        float seconds = 0f;
        while (ExposureRules.damage(gauge, exposure, TICK) == 0f && seconds < 1000f) {
            gauge = ExposureRules.step(gauge, exposure, TICK);
            seconds += TICK;
        }
        return seconds;
    }

    @Test
    void oneNotchOfThinAirVeilsAndHurtsInFortyFiveSeconds() {
        float seconds = secondsToDamage(-1f);
        assertEquals(45f, seconds, TICK);
        assertEquals(1f, ExposureRules.veil(-ExposureRules.GAUGE_MAX));
    }

    @Test
    void aStrongerExposureIsFaster() {
        assertEquals(22.5f, secondsToDamage(2f), TICK);
        assertEquals(90f, secondsToDamage(0.5f), TICK);
    }

    @Test
    void theDamageGrowsWithTheIntensity() {
        float full = ExposureRules.GAUGE_MAX;
        assertEquals(0.5f, ExposureRules.damage(full, 1f, 1f));
        assertEquals(1f, ExposureRules.damage(full, 2f, 1f));
        assertEquals(0f, ExposureRules.damage(full, -1f, 1f), "a full heat gauge does not hurt in the cold");
        assertEquals(0f, ExposureRules.damage(full * 0.99f, 1f, 1f), "no damage before the gauge is full");
    }

    @Test
    void theGaugeDrainsOnceTheExposureStops() {
        float gauge = -ExposureRules.GAUGE_MAX;
        for (float seconds = 0f; seconds < ExposureRules.RECOVER_SECONDS; seconds += TICK) {
            gauge = ExposureRules.step(gauge, 0f, TICK);
        }
        assertEquals(0f, gauge, 1e-3f);
    }

    @Test
    void protectionTakesOffOneNotchAtMost() {
        assertEquals(0f, ExposureRules.protect(-1f, 1f), "a moderate cold is lived in with the right gear");
        assertEquals(-1f, ExposureRules.protect(-2f, 3f), "an extreme cold is not, whatever is piled up");
        assertEquals(0.5f, ExposureRules.protect(1f, 0.5f));
    }

    @Test
    void onlyTheColdAndTheThinAirSlow() {
        float full = ExposureRules.GAUGE_MAX;
        assertEquals(1f, ExposureRules.speedFactor(full, full), "heat and the deep do not slow");
        assertEquals(1f - ExposureRules.MAX_SLOW, ExposureRules.speedFactor(-full, 0f), 1e-6f);
        assertTrue(ExposureRules.speedFactor(-full / 2f, 0f) > ExposureRules.speedFactor(-full, 0f));
        assertEquals(1f, ExposureRules.speedFactor(-full * ExposureRules.SLOW_START, 0f), 1e-6f);
    }

    @Test
    void theDeepAndTheHeightsGrowBlockByBlock() {
        assertEquals(0f, ExposureRules.depthPressure(ExposureRules.DEPTH_START));
        assertEquals(1f, ExposureRules.depthPressure(ExposureRules.DEPTH_START + ExposureRules.DEPTH_PER_NOTCH));
        assertEquals(0f, ExposureRules.altitudePressure(100f));
        assertEquals(-1f, ExposureRules.altitudePressure(
                ExposureRules.ALTITUDE_START + ExposureRules.ALTITUDE_PER_NOTCH));
    }
}
