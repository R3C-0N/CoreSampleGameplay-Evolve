// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

/**
 * How far a character's body has given way to the heat, the cold, the crushing deep or the thin air.
 * <p>
 * Two signed gauges, from minus to plus {@link ExposureRules#GAUGE_MAX}: a positive temperature is heat and a
 * negative one cold; a positive pressure is the deep and a negative one the heights. Nought is a body at ease.
 * Everything here is replicated, because the client reads it twice: for the gauges and veils on the screen, and
 * for the slowdown, which the client's movement prediction must apply exactly as the server does.
 */
public class ExposureComponent implements Component<ExposureComponent> {

    @Replicate
    public float temperature;

    @Replicate
    public float pressure;

    /** What the place does to the temperature gauge right now, once protection is taken off. Signed. */
    @Replicate
    public float heatExposure;

    /** What the place does to the pressure gauge right now, once protection is taken off. Signed. */
    @Replicate
    public float pressureExposure;

    /** Whether the character is in an extreme region, which is the only place the gauges are shown. */
    @Replicate
    public boolean exposed;

    @Override
    public void copyFrom(ExposureComponent other) {
        this.temperature = other.temperature;
        this.pressure = other.pressure;
        this.heatExposure = other.heatExposure;
        this.pressureExposure = other.pressureExposure;
        this.exposed = other.exposed;
    }
}
