// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.terasology.durability.components.DurabilityComponent;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.console.commandSystem.annotations.Command;
import org.terasology.engine.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.engine.logic.console.commandSystem.annotations.Sender;
import org.terasology.engine.logic.permission.PermissionManager;
import org.terasology.engine.network.ClientComponent;
import org.terasology.module.inventory.components.SelectedInventorySlotComponent;

/**
 * Wearing what is in hand by hand, to try a broken tool or a repair without breaking a thousand blocks.
 */
@RegisterSystem
public class WearCommands extends BaseComponentSystem {

    @Command(shortDescription = "Use l'objet en main",
            helpText = "Retire autant de points de solidité à l'objet en main ; sans valeur, le casse.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String usure(@Sender EntityRef client, @CommandParam(value = "points", required = false) Integer points) {
        ClientComponent clientComponent = client.getComponent(ClientComponent.class);
        EntityRef character = clientComponent == null ? EntityRef.NULL : clientComponent.character;
        SelectedInventorySlotComponent selected = character.getComponent(SelectedInventorySlotComponent.class);
        EntityRef item = selected == null ? EntityRef.NULL : CharacterStats.itemAt(character, selected.slot);
        DurabilityComponent durability = item.getComponent(DurabilityComponent.class);
        if (durability == null) {
            return "Rien en main ne s'use";
        }
        Wear.wear(item, points == null ? durability.durability : points);
        durability = item.getComponent(DurabilityComponent.class);
        return durability.durability <= 0 ? "Cassé" : "Solidité : " + durability.durability + " / " + durability.maxDurability;
    }
}
