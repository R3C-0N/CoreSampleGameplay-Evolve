// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.food;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.console.commandSystem.annotations.Command;
import org.terasology.engine.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.engine.logic.console.commandSystem.annotations.Sender;
import org.terasology.engine.logic.permission.PermissionManager;
import org.terasology.engine.network.ClientComponent;

/**
 * Setting the hunger gauge by hand, to try a meal or an empty stomach without waiting forty minutes.
 */
@RegisterSystem
public class HungerCommands extends BaseComponentSystem {

    @Command(shortDescription = "Règle la jauge de faim",
            helpText = "Sans valeur, donne l'état de la jauge. Avec une valeur, la règle entre 0 et le maximum.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String faim(@Sender EntityRef client, @CommandParam(value = "valeur", required = false) Float value) {
        ClientComponent clientComponent = client.getComponent(ClientComponent.class);
        EntityRef character = clientComponent == null ? EntityRef.NULL : clientComponent.character;
        HungerComponent hunger = character.getComponent(HungerComponent.class);
        if (hunger == null) {
            return "Ce personnage n'a pas faim";
        }
        if (value != null) {
            hunger.currentFood = Math.max(0f, Math.min(hunger.maxFood, value));
            character.saveComponent(hunger);
        }
        return String.format("Faim : %.1f / %.0f", hunger.currentFood, hunger.maxFood);
    }
}
