// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

/**
 * The arithmetic of exposure, kept apart from the entity system so it can be read and tested on its own.
 * <p>
 * <strong>An intensity is a count of notches.</strong> One is a moderate place: the edge of a volcano, the pack
 * ice, the first fathoms of the abyss. Two and more is an extreme one: the lava's own shore, the ice shelf, the
 * deep. Intensities are continuous, so the deep and the heights grow with every block rather than by steps.
 * <p>
 * <strong>Protection takes off at most one notch</strong>, whatever is piled up: armour, a fire, a roof. That
 * is the whole rule of the two tiers — what is moderate can be lived in with the right gear, what is extreme
 * cannot, and only the region's vehicle will make it so.
 * <p>
 * <strong>The gauge fills in {@link #RISE_SECONDS} at one notch</strong>, twice as fast at two. Half way, the
 * veil starts to show; full, the damage starts, again in proportion to the intensity. Out of the exposure the
 * gauge drains back to nought in {@link #RECOVER_SECONDS}.
 */
public final class ExposureRules {

    public static final float GAUGE_MAX = 100f;

    /** Seconds for one notch of exposure to fill an empty gauge. */
    public static final float RISE_SECONDS = 45f;

    /** Seconds for a full gauge to drain once the exposure stops. */
    public static final float RECOVER_SECONDS = 30f;

    /** Points of damage a second, per notch, once the gauge is full. */
    public static final float DAMAGE_PER_NOTCH = 0.5f;

    /** Share of the gauge at which the veil starts to show. It is full when the damage starts. */
    public static final float VEIL_START = 0.5f;

    /** Share of the gauge at which the cold or the thin air starts to slow a character down. */
    public static final float SLOW_START = 0.3f;

    /** How much of its speed a character has lost with a full gauge of cold or of thin air. */
    public static final float MAX_SLOW = 0.5f;

    /** The most that armour, fire and shelter together take off an intensity. */
    public static final float MAX_PROTECTION = 1f;

    /** Notches of heat in a volcanic region, and close to its lava. */
    public static final float VOLCANIC_HEAT = 1f;
    public static final float LAVA_SHORE_HEAT = 2f;

    /** Notches of cold on the northern pack ice and on the southern ice shelf. */
    public static final float PACK_ICE_COLD = 1f;
    public static final float ICE_SHELF_COLD = 2f;

    /** Notches of cold in the water of the abyss. */
    public static final float ABYSS_COLD = 1f;

    /** Depth below the sea at which the water starts to press, and depth for each further notch. */
    public static final float DEPTH_START = 16f;
    public static final float DEPTH_PER_NOTCH = 16f;

    /** Height at which the air starts to thin out, and height for each further notch. */
    public static final float ALTITUDE_START = 160f;
    public static final float ALTITUDE_PER_NOTCH = 64f;

    private ExposureRules() {
    }

    /** Notches of pressure at this depth below the sea, nought above {@link #DEPTH_START}. */
    public static float depthPressure(float depth) {
        return Math.max(0f, (depth - DEPTH_START) / DEPTH_PER_NOTCH);
    }

    /** Notches of thin air at this height, as a negative pressure, nought below {@link #ALTITUDE_START}. */
    public static float altitudePressure(float y) {
        return -Math.max(0f, (y - ALTITUDE_START) / ALTITUDE_PER_NOTCH);
    }

    /**
     * What is left of an intensity once protection is taken off. The sign is kept; the protection never turns
     * a cold into a heat.
     */
    public static float protect(float intensity, float protection) {
        float shield = Math.max(0f, Math.min(MAX_PROTECTION, protection));
        float left = Math.max(0f, Math.abs(intensity) - shield);
        return Math.copySign(left, intensity);
    }

    /**
     * The gauge after {@code seconds} under this exposure: towards its side at a pace set by the intensity, or
     * back towards nought when there is none.
     */
    public static float step(float gauge, float exposure, float seconds) {
        if (exposure == 0f) {
            float drain = GAUGE_MAX / RECOVER_SECONDS * seconds;
            return gauge > 0f ? Math.max(0f, gauge - drain) : Math.min(0f, gauge + drain);
        }
        float next = gauge + exposure * GAUGE_MAX / RISE_SECONDS * seconds;
        return Math.max(-GAUGE_MAX, Math.min(GAUGE_MAX, next));
    }

    /**
     * Damage owed for {@code seconds} at this gauge and exposure: none until the gauge is full on the side the
     * exposure pushes it to.
     */
    public static float damage(float gauge, float exposure, float seconds) {
        if (exposure == 0f || Math.abs(gauge) < GAUGE_MAX || Math.signum(gauge) != Math.signum(exposure)) {
            return 0f;
        }
        return Math.abs(exposure) * DAMAGE_PER_NOTCH * seconds;
    }

    /** How strongly the veil of a gauge shows, from nought at {@link #VEIL_START} to one when full. */
    public static float veil(float gauge) {
        float share = Math.abs(gauge) / GAUGE_MAX;
        return clamp01((share - VEIL_START) / (1f - VEIL_START));
    }

    /**
     * The factor on a character's speed: only the cold and the thin air slow, the heat and the deep do not.
     */
    public static float speedFactor(float temperature, float pressure) {
        return (1f - slow(temperature)) * (1f - slow(pressure));
    }

    private static float slow(float gauge) {
        if (gauge >= 0f) {
            return 0f;
        }
        float share = -gauge / GAUGE_MAX;
        return MAX_SLOW * clamp01((share - SLOW_START) / (1f - SLOW_START));
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
