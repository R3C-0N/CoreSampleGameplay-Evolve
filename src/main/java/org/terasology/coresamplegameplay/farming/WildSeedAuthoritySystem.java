// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.biomesAPI.Biome;
import org.terasology.biomesAPI.BiomeRegistry;
import org.terasology.core.world.CoreBiome;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.health.DoDestroyEvent;
import org.terasology.engine.logic.inventory.events.DropItemEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.gestalt.naming.Name;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Where seeds come from: tall grass, one time in ten, and which seed depends on the biome it grew in.
 * <p>
 * The grass keeps its fibre, from its drop grammar; the seed comes on top. Each crop belongs to the biome the design
 * gave it — wheat and onion to the plains, the potato to the mountains (there are no hills), the carrot to the
 * forest, rice and soy to the swamp, the squash to the savanna. Tall grass elsewhere gives no seed.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class WildSeedAuthoritySystem extends BaseComponentSystem {
    private static final float SEED_CHANCE = 0.1f;
    private static final String M = "CoreSampleGameplay:";
    private static final Set<String> TALL_GRASS = Set.of("coreassets:tallgrass1", "coreassets:tallgrass2",
            "coreassets:tallgrass3");
    private static final Map<Name, List<String>> SEEDS = Map.of(
            CoreBiome.PLAINS.getId(), List.of(M + "grainesBle", M + "grainesOignon"),
            CoreBiome.MOUNTAINS.getId(), List.of(M + "grainesPommeDeTerre"),
            CoreBiome.FOREST.getId(), List.of(M + "grainesCarotte"),
            CoreBiome.SWAMP.getId(), List.of(M + "grainesRiz", M + "grainesSoja"),
            CoreBiome.SAVANNA.getId(), List.of(M + "grainesCourge"));

    @In
    private EntityManager entityManager;
    @In
    private BiomeRegistry biomeRegistry;

    private final Random random = new FastRandom();

    @ReceiveEvent
    public void seedFromGrass(DoDestroyEvent event, EntityRef entity, BlockComponent block) {
        if (!TALL_GRASS.contains(block.getBlock().getBlockFamily().getURI().toString().toLowerCase())
                || random.nextFloat() >= SEED_CHANCE) {
            return;
        }
        List<String> seeds = biomeOf(block.getPosition()).map(biome -> SEEDS.get(biome.getId())).orElse(null);
        if (seeds == null) {
            return;
        }
        EntityRef seed = entityManager.create(seeds.get(random.nextInt(seeds.size())));
        if (seed.exists()) {
            seed.send(new DropItemEvent(new Vector3f(block.getPosition())));
        }
    }

    /** The grass's own cell, or the ground under it if the generator left the air above the surface without one. */
    private Optional<Biome> biomeOf(Vector3ic position) {
        Optional<Biome> biome = biomeRegistry.getBiome(position);
        if (biome.isPresent() && SEEDS.containsKey(biome.get().getId())) {
            return biome;
        }
        return biomeRegistry.getBiome(new Vector3i(position).sub(0, 1, 0)).or(() -> biome);
    }
}
