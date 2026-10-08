// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.console.commandSystem.annotations.Command;
import org.terasology.engine.logic.console.commandSystem.annotations.CommandParam;
import org.terasology.engine.logic.console.commandSystem.annotations.Sender;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.permission.PermissionManager;
import org.terasology.engine.network.ClientComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Growing the plants nearby by hand, to try a harvest without waiting a quarter of an hour.
 */
@RegisterSystem
public class FarmingCommands extends BaseComponentSystem {
    private static final float REACH = 16f;
    private static final int RIPE = 3;

    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;

    @Command(shortDescription = "Fait pousser les plants proches",
            helpText = "Fait avancer d'autant de stades les plants à moins de seize blocs, jusqu'à maturité sans valeur.",
            runOnServer = true,
            requiredPermission = PermissionManager.CHEAT_PERMISSION)
    public String pousse(@Sender EntityRef client, @CommandParam(value = "stades", required = false) Integer stages) {
        ClientComponent clientComponent = client.getComponent(ClientComponent.class);
        EntityRef character = clientComponent == null ? EntityRef.NULL : clientComponent.character;
        LocationComponent location = character.getComponent(LocationComponent.class);
        if (location == null) {
            return "Personne ne regarde pousser";
        }
        Vector3f here = location.getWorldPosition(new Vector3f());
        int steps = stages == null ? RIPE : Math.max(0, stages);

        // Positions, not entities: changing a block may hand its position a new entity.
        Set<Vector3i> near = new HashSet<>();
        for (EntityRef plant : entityManager.getEntitiesWith(CropComponent.class, BlockComponent.class)) {
            Vector3i position = new Vector3i(plant.getComponent(BlockComponent.class).getPosition());
            if (new Vector3f(position).distance(here) <= REACH) {
                near.add(position);
            }
        }
        int grown = 0;
        for (Vector3i position : near) {
            boolean moved = false;
            for (int i = 0; i < steps; i++) {
                CropComponent crop = blockEntityRegistry.getBlockEntityAt(position).getComponent(CropComponent.class);
                Block next = crop == null || crop.next.isEmpty() ? null : blockManager.getBlock(crop.next);
                if (next == null) {
                    break;
                }
                worldProvider.setBlock(position, next);
                moved = true;
            }
            if (moved) {
                grown++;
            }
        }
        return String.format("%d plant(s) poussé(s) sur %d à portée", grown, near.size());
    }
}
