// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.input.binds.movement.JumpButton;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.NUIManager;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.input.ButtonState;
import org.terasology.module.inventory.input.InventoryButton;

/**
 * The two client-side gestures of creative mode: which screen the inventory key opens, and the double tap on
 * jump that lifts the character off the ground.
 */
@RegisterSystem(RegisterMode.CLIENT)
public class CreativeClientSystem extends BaseComponentSystem {
    private static final String CREATIVE_SCREEN = "CoreSampleGameplay:creativeScreen";

    /** Two jumps closer together than this are one gesture; further apart, they are two jumps. */
    private static final long DOUBLE_TAP_MS = 300;

    @In
    private NUIManager nuiManager;
    @In
    private LocalPlayer localPlayer;
    @In
    private Time time;

    private long lastJumpTime;

    /**
     * Taken above {@code InventoryUIClientSystem}, which would otherwise open the character screen. Going
     * through the key rather than through a second override of {@code Inventory:inventoryScreen} matters:
     * {@code CharacterScreen} already holds that override, and a module cannot claim it twice.
     */
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = ClientComponent.class)
    public void openCatalogue(InventoryButton event, EntityRef client, ClientComponent clientComponent) {
        if (event.getState() != ButtonState.DOWN
                || !clientComponent.character.hasComponent(CreativeModeComponent.class)) {
            return;
        }
        nuiManager.toggleScreen(CREATIVE_SCREEN);
        event.consume();
    }

    /**
     * The jump binding repeats while the key is held, so only the presses are counted. The event is never
     * consumed: the second tap still jumps, which is what takes the character off the ground before it flies.
     */
    @ReceiveEvent(components = ClientComponent.class)
    public void flyOnDoubleJump(JumpButton event, EntityRef client, ClientComponent clientComponent) {
        if (event.getState() != ButtonState.DOWN
                || !clientComponent.character.hasComponent(CreativeModeComponent.class)) {
            return;
        }
        long now = time.getGameTimeInMs();
        if (now - lastJumpTime < DOUBLE_TAP_MS) {
            localPlayer.getCharacterEntity().send(new ToggleFlightRequest());
            lastJumpTime = 0;
        } else {
            lastJumpTime = now;
        }
    }
}
