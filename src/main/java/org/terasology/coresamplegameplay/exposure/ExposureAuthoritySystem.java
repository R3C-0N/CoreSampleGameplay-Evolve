// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.exposure;

import com.google.common.collect.Maps;
import org.joml.Vector3f;
import org.terasology.biomesAPI.Biome;
import org.terasology.biomesAPI.BiomeRegistry;
import org.terasology.core.world.CoreBiome;
import org.terasology.coresamplegameplay.creative.CreativeModeComponent;
import org.terasology.coresamplegameplay.equipment.ArmorComponent;
import org.terasology.coresamplegameplay.equipment.CharacterStats;
import org.terasology.coresamplegameplay.equipment.EquipmentSlots;
import org.terasology.coresamplegameplay.food.HungerComponent;
import org.terasology.cubeworlds.generator.CubeGeometry;
import org.terasology.engine.context.Context;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.characters.AliveCharacterComponent;
import org.terasology.engine.logic.characters.CharacterMovementComponent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.logic.players.event.OnPlayerRespawnedEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.Assets;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.events.DoDamageEvent;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads what the place does to each player, moves their two gauges, and makes a full one hurt.
 * <p>
 * <strong>Only the extreme regions are felt.</strong> The heat is the volcanoes', the cold the polar caps' and
 * the abyss's water, the deep is the abyss and the thin air is anything high enough. Everywhere else the place
 * does nothing, the gauges drain and nothing shows: the present is peaceful (D7).
 * <p>
 * <strong>Protection is read off the character and its surroundings,</strong> and none of it is a stat to keep
 * in sync. Armour counts by the share of the body it covers — head, chest, legs and feet — not by its defence,
 * which is about blows. A fire within reach and a roof overhead help against the cold only. Together they take
 * off one notch at most, see {@link ExposureRules}.
 * <p>
 * There is no campfire yet, so the fire is any lava, torch or furnace near enough to warm one's hands.
 * <p>
 * The creative player feels nothing, like it hungers for nothing.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class ExposureAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final long PERIOD_MS = 500;

    /** How far a fire warms, in blocks, and how far the lava's heat reaches out of the lava. */
    private static final int FIRE_REACH = 4;

    /** How high a roof may be and still shelter, in blocks above the head. */
    private static final int ROOF_REACH = 8;

    /** What a roof is worth against the cold, a fire being worth a whole notch. */
    private static final float ROOF_PROTECTION = 0.5f;

    /** The four slots that cover the body, among the eight of the equipment. */
    private static final int COVERING_SLOTS = 4;

    private static final Set<BlockUri> FIRES = Set.of(
            new BlockUri("CoreAssets:Torch"),
            new BlockUri("CoreSampleGameplay:Furnace"));

    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private BiomeRegistry biomeRegistry;
    @In
    private Context context;
    @In
    private Time time;

    private Prefab heatDamage;
    private Prefab coldDamage;
    private Prefab crushDamage;
    private Prefab thinAirDamage;
    private CubeGeometry geometry;
    private long lastTick;

    /** The fractional damage each character carries over, temperature first, since damage is in whole points. */
    private final Map<EntityRef, float[]> owed = Maps.newHashMap();

    @Override
    public void initialise() {
        heatDamage = Assets.getPrefab("CoreSampleGameplay:heatDamage").orElse(null);
        coldDamage = Assets.getPrefab("CoreSampleGameplay:coldDamage").orElse(null);
        crushDamage = Assets.getPrefab("CoreSampleGameplay:crushDamage").orElse(null);
        thinAirDamage = Assets.getPrefab("CoreSampleGameplay:thinAirDamage").orElse(null);
    }

    @Override
    public void update(float delta) {
        long now = time.getGameTimeInMs();
        if (lastTick == 0 || now < lastTick) {
            lastTick = now;
            return;
        }
        if (lastTick + PERIOD_MS > now) {
            return;
        }
        float elapsed = (now - lastTick) / 1000f;
        lastTick = now;

        for (EntityRef character : entityManager.getEntitiesWith(HungerComponent.class,
                AliveCharacterComponent.class, LocationComponent.class)) {
            ExposureComponent exposure = character.getComponent(ExposureComponent.class);
            boolean fresh = exposure == null;
            if (fresh) {
                exposure = new ExposureComponent();
            }
            ExposureComponent before = new ExposureComponent();
            before.copyFrom(exposure);

            if (character.hasComponent(CreativeModeComponent.class)) {
                exposure.copyFrom(new ExposureComponent());
                owed.remove(character);
            } else {
                expose(character, exposure, elapsed);
            }

            if (fresh) {
                character.addComponent(exposure);
            } else if (changed(before, exposure)) {
                character.saveComponent(exposure);
            }
        }
        owed.keySet().removeIf(character -> !character.exists());
    }

    private void expose(EntityRef character, ExposureComponent exposure, float elapsed) {
        Vector3f position = character.getComponent(LocationComponent.class).getWorldPosition(new Vector3f());
        if (!position.isFinite()) {
            return;
        }
        CharacterMovementComponent movement = character.getComponent(CharacterMovementComponent.class);
        float height = movement == null ? 1.6f : movement.height;
        int x = Math.round(position.x);
        int z = Math.round(position.z);
        int feet = Math.round(position.y - 0.4f * height);
        int head = Math.round(position.y + 0.4f * height);

        Optional<Biome> biome = biomeRegistry.getBiome(x, feet, z);
        Block atHead = worldProvider.getBlock(x, head, z);
        boolean submerged = atHead.isLiquid() && atHead.getWarmth() == 0;
        boolean abyss = is(biome, CoreBiome.ABYSS);
        Surroundings around = surroundings(x, head, z);

        float heat = 0f;
        if (is(biome, CoreBiome.VOLCANIC)) {
            heat = around.lava ? ExposureRules.LAVA_SHORE_HEAT : ExposureRules.VOLCANIC_HEAT;
        } else if (is(biome, CoreBiome.ICE_SHELF)) {
            heat = -ExposureRules.ICE_SHELF_COLD;
        } else if (is(biome, CoreBiome.PACK_ICE)) {
            heat = -ExposureRules.PACK_ICE_COLD;
        } else if (abyss && submerged) {
            heat = -ExposureRules.ABYSS_COLD;
        }

        float pressure = ExposureRules.altitudePressure(position.y);
        if (abyss && submerged) {
            pressure += ExposureRules.depthPressure(seaLevel() - head);
        }

        float armour = coverage(character);
        float warmth = heat < 0f ? armour + (around.fire ? 1f : 0f) + (roofed(x, head, z) ? ROOF_PROTECTION : 0f)
                : armour;
        exposure.heatExposure = ExposureRules.protect(heat, warmth);
        exposure.pressureExposure = ExposureRules.protect(pressure, armour);
        exposure.exposed = heat != 0f || pressure != 0f;

        exposure.temperature = ExposureRules.step(exposure.temperature, exposure.heatExposure, elapsed);
        exposure.pressure = ExposureRules.step(exposure.pressure, exposure.pressureExposure, elapsed);

        float[] debt = owed.computeIfAbsent(character, c -> new float[2]);
        debt[0] = hurt(character, debt[0] + ExposureRules.damage(exposure.temperature, exposure.heatExposure, elapsed),
                exposure.temperature > 0f ? heatDamage : coldDamage);
        debt[1] = hurt(character, debt[1] + ExposureRules.damage(exposure.pressure, exposure.pressureExposure, elapsed),
                exposure.pressure > 0f ? crushDamage : thinAirDamage);
    }

    private static float hurt(EntityRef character, float debt, Prefab type) {
        int points = (int) debt;
        if (points > 0) {
            character.send(new DoDamageEvent(points, type));
        }
        return debt - points;
    }

    /** What lies around the head: a fire to warm by, and lava close enough to scorch. */
    private Surroundings surroundings(int x, int y, int z) {
        Surroundings found = new Surroundings();
        for (int dx = -FIRE_REACH; dx <= FIRE_REACH; dx++) {
            for (int dy = -FIRE_REACH; dy <= FIRE_REACH; dy++) {
                for (int dz = -FIRE_REACH; dz <= FIRE_REACH; dz++) {
                    Block block = worldProvider.getBlock(x + dx, y + dy, z + dz);
                    if (block.getWarmth() > 0) {
                        found.fire = true;
                        found.lava |= block.isLiquid();
                    } else if (FIRES.contains(block.getBlockFamily().getURI())) {
                        found.fire = true;
                    }
                    if (found.lava) {
                        return found;
                    }
                }
            }
        }
        return found;
    }

    private boolean roofed(int x, int y, int z) {
        for (int dy = 1; dy <= ROOF_REACH; dy++) {
            if (!worldProvider.getBlock(x, y + dy, z).isPenetrable()) {
                return true;
            }
        }
        return false;
    }

    /** The share of the body covered by armour, from nought to one. */
    private static float coverage(EntityRef character) {
        int covered = 0;
        for (int slot = EquipmentSlots.FIRST; slot < EquipmentSlots.FIRST + COVERING_SLOTS; slot++) {
            if (CharacterStats.itemAt(character, slot).hasComponent(ArmorComponent.class)) {
                covered++;
            }
        }
        return covered / (float) COVERING_SLOTS;
    }

    private static boolean is(Optional<Biome> biome, CoreBiome expected) {
        return biome.isPresent() && expected.getId().equals(biome.get().getId());
    }

    /**
     * Looked up on use and never cached as absent, like the build limit does: the generator publishes the
     * geometry while the world loads, which may be after this system is wired.
     */
    private int seaLevel() {
        if (geometry == null && context != null) {
            geometry = context.get(CubeGeometry.class);
        }
        return geometry == null ? CubeGeometry.DEFAULT_SEA_LEVEL : geometry.seaLevel();
    }

    private static boolean changed(ExposureComponent a, ExposureComponent b) {
        return a.temperature != b.temperature || a.pressure != b.pressure || a.heatExposure != b.heatExposure
                || a.pressureExposure != b.pressureExposure || a.exposed != b.exposed;
    }

    /** Back from death, the body starts afresh. */
    @ReceiveEvent
    public void reset(OnPlayerRespawnedEvent event, EntityRef character, ExposureComponent exposure) {
        exposure.copyFrom(new ExposureComponent());
        character.saveComponent(exposure);
        owed.remove(character);
    }

    private static final class Surroundings {
        boolean fire;
        boolean lava;
    }
}
