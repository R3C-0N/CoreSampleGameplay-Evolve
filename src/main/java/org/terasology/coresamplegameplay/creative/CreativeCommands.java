// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.MovementMode;
import org.terasology.engine.logic.characters.events.SetMovementModeEvent;
import org.terasology.engine.logic.console.commandSystem.annotations.Command;
import org.terasology.engine.logic.console.commandSystem.annotations.Sender;
import org.terasology.engine.logic.permission.PermissionManager;
import org.terasology.engine.network.ClientComponent;

/**
 * Switching a character between building and surviving, without leaving the world.
 * <p>
 * The other way in is {@code CreativeGameplay}, a gameplay module whose only content is a delta putting
 * {@link CreativeModeComponent} on the player: a world started in that mode begins creative. This command is
 * what makes the choice reversible afterwards, in both directions.
 */
@RegisterSystem
public class CreativeCommands extends BaseComponentSystem {

    @Command(shortDescription = "Bascule le mode créatif",
            helpText = "En créatif : invulnérable, pose sans dépenser, casse d'un coup, et l'inventaire devient "
                    + "le catalogue de tout le jeu. Double appui sur espace pour voler.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String creative(@Sender EntityRef client) {
        ClientComponent clientComponent = client.getComponent(ClientComponent.class);
        if (clientComponent == null) {
            return "Aucun personnage à basculer";
        }
        EntityRef character = clientComponent.character;
        if (character.hasComponent(CreativeModeComponent.class)) {
            character.removeComponent(CreativeModeComponent.class);
            // Leaving creative on the wing would drop the character out of the sky with no way back up.
            character.send(new SetMovementModeEvent(MovementMode.WALKING));
            return "Mode créatif désactivé";
        }
        character.addComponent(new CreativeModeComponent());
        return "Mode créatif activé";
    }
}
