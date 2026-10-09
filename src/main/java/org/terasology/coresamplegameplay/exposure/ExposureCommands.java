// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.console.commandSystem.annotations.Command;
import org.terasology.engine.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.engine.logic.console.commandSystem.annotations.Sender;
import org.terasology.engine.logic.permission.PermissionManager;
import org.terasology.engine.network.ClientComponent;

/**
 * Setting the two gauges by hand, to see a veil or a slowdown without walking to the poles.
 * <p>
 * A gauge set here is only a starting point: the next sweep moves it again with what the place does, so out of
 * the extreme regions it drains away in half a minute.
 */
@RegisterSystem
public class ExposureCommands extends BaseComponentSystem {

    @Command(shortDescription = "Règle la jauge de température",
            helpText = "Sans valeur, donne l'état des deux jauges. Avec une valeur entre -100 (froid) et 100 "
                    + "(chaleur), règle celle de la température.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String temperature(@Sender EntityRef client, @CommandParam(value = "valeur", required = false) Float value) {
        return set(client, value, true);
    }

    @Command(shortDescription = "Règle la jauge de pression",
            helpText = "Sans valeur, donne l'état des deux jauges. Avec une valeur entre -100 (air raréfié) et "
                    + "100 (profondeur), règle celle de la pression.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String pression(@Sender EntityRef client, @CommandParam(value = "valeur", required = false) Float value) {
        return set(client, value, false);
    }

    private static String set(EntityRef client, Float value, boolean temperature) {
        ClientComponent clientComponent = client.getComponent(ClientComponent.class);
        EntityRef character = clientComponent == null ? EntityRef.NULL : clientComponent.character;
        ExposureComponent exposure = character.getComponent(ExposureComponent.class);
        if (exposure == null) {
            return "Ce personnage ne ressent ni chaleur ni pression";
        }
        if (value != null) {
            float gauge = Math.max(-ExposureRules.GAUGE_MAX, Math.min(ExposureRules.GAUGE_MAX, value));
            if (temperature) {
                exposure.temperature = gauge;
            } else {
                exposure.pressure = gauge;
            }
            character.saveComponent(exposure);
        }
        return String.format("Température : %.1f (exposition %.2f) · Pression : %.1f (exposition %.2f)%s",
                exposure.temperature, exposure.heatExposure, exposure.pressure, exposure.pressureExposure,
                exposure.exposed ? " · région extrême" : "");
    }
}
