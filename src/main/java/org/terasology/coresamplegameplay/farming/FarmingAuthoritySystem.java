// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.farming;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.coresamplegameplay.equipment.ToolComponent;
import org.terasology.engine.audio.StaticSound;
import org.terasology.engine.audio.events.PlaySoundEvent;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.events.DropItemEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Tilling, sowing, and what becomes of a field left alone.
 * <p>
 * <strong>A hoe turns grass or dirt into tilled soil</strong>, in one use, as long as nothing solid sits on top.
 * Broken, tilled soil gives plain dirt back. <strong>Tilled soil holds only near water:</strong> with water within
 * four blocks across and one up or down it is wet; without, it is dry, and after a minute dry it goes back to dirt,
 * and whatever grew on it is lost but for its seed.
 * <p>
 * <strong>A plant grows in four stages</strong>, each three to five minutes long, and only while it stands on wet
 * soil in the light: below {@link #MIN_LIGHT} the time does not count. The ripe stage gives one to three crops
 * and one or two seeds when broken, and any earlier stage its seed back; both are in the stage prefabs, as drop
 * grammars.
 * <p>
 * Soil and plants are block entities kept active, so that this system finds them: a field in an unloaded chunk
 * neither grows nor dries out, as in Minecraft.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class FarmingAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    static final String DRY_SOIL = "CoreSampleGameplay:TerreLabouree";
    static final String WET_SOIL = "CoreSampleGameplay:TerreLaboureeHumide";
    private static final Set<BlockUri> TILLABLE = Set.of(new BlockUri("CoreAssets:Dirt"), new BlockUri("CoreAssets:Grass"));
    private static final String DIRT = "CoreAssets:Dirt";

    private static final long TICK_MS = 1000;
    /** How long tilled soil lasts without water before it is dirt again. */
    private static final long DRY_OUT_MS = 60_000;
    /** How far water reaches, across; up and down it is one block. */
    private static final int WATER_REACH = 4;
    /** Light, sun or torch, out of fifteen, below which a plant stops growing. */
    static final int MIN_LIGHT = 9;
    private static final long STAGE_MIN_MS = 180_000;
    private static final long STAGE_MAX_MS = 300_000;

    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private BlockManager blockManager;
    @In
    private BlockEntityRegistry blockEntityRegistry;
    @In
    private Time time;

    private final Random random = new FastRandom();
    private long lastTick;

    @ReceiveEvent
    public void till(ActivateEvent event, EntityRef item, ToolComponent tool) {
        if (!"hoe".equals(tool.family)) {
            return;
        }
        BlockComponent target = event.getTarget().getComponent(BlockComponent.class);
        if (target == null) {
            return;
        }
        Vector3i ground = new Vector3i(target.getPosition());
        if (isTurf(target.getBlock())) {
            // Tall grass stands in the way of the aim more often than not: the hoe goes through it.
            ground.sub(0, 1, 0);
        }
        Block soil = worldProvider.getBlock(ground);
        if (!TILLABLE.contains(soil.getBlockFamily().getURI())) {
            return;
        }
        Vector3i above = new Vector3i(ground).add(0, 1, 0);
        Block over = worldProvider.getBlock(above);
        Block air = blockManager.getBlock(BlockManager.AIR_ID);
        if (over != air && !isTurf(over)) {
            return;
        }
        event.consume();
        if (over != air) {
            // Tall grass and flowers go with the turf.
            worldProvider.setBlock(above, air);
        }
        worldProvider.setBlock(ground, blockManager.getBlock(DRY_SOIL));
        playDigSound(blockEntityRegistry.getBlockEntityAt(ground), soil);
    }

    /** What grows on the ground and goes with it when it is turned: a replaceable plant, not a crop. */
    private static boolean isTurf(Block block) {
        return block.isReplacementAllowed() && block.isPenetrable() && !block.isLiquid()
                && block.isSupportRequired() && !block.getPrefab().map(p -> p.hasComponent(CropComponent.class)).orElse(false);
    }

    @ReceiveEvent
    public void sow(ActivateEvent event, EntityRef item, SeedComponent seed) {
        BlockComponent target = event.getTarget().getComponent(BlockComponent.class);
        Block plant = blockManager.getBlock(seed.plant);
        Vector3i above = target == null ? null : new Vector3i(target.getPosition()).add(0, 1, 0);
        if (target == null || !isTilled(target.getBlock()) || plant == null
                || worldProvider.getBlock(above) != blockManager.getBlock(BlockManager.AIR_ID)) {
            event.consume();
            return;
        }
        worldProvider.setBlock(above, plant);
    }

    @Override
    public void update(float delta) {
        long now = time.getGameTimeInMs();
        if (lastTick == 0 || now < lastTick) {
            lastTick = now;
            return;
        }
        if (lastTick + TICK_MS > now) {
            return;
        }
        long elapsed = Math.min(now - lastTick, 10 * TICK_MS);
        lastTick = now;

        // Collected first: both passes change blocks, and a changed block rebuilds its entity's components.
        List<EntityRef> soils = new ArrayList<>();
        entityManager.getEntitiesWith(TilledSoilComponent.class, BlockComponent.class).forEach(soils::add);
        List<EntityRef> crops = new ArrayList<>();
        entityManager.getEntitiesWith(CropComponent.class, BlockComponent.class).forEach(crops::add);
        soils.forEach(soil -> water(soil, elapsed));
        crops.forEach(crop -> grow(crop, elapsed));
    }

    private void water(EntityRef soil, long elapsed) {
        if (!soil.exists() || !soil.hasComponent(TilledSoilComponent.class)) {
            return;
        }
        Vector3ic position = soil.getComponent(BlockComponent.class).getPosition();
        Block block = worldProvider.getBlock(position);
        boolean wet = isWet(block);
        if (waterNear(position)) {
            if (!wet) {
                worldProvider.setBlock(position, blockManager.getBlock(WET_SOIL));
            }
            TilledSoilComponent tilled = soil.getComponent(TilledSoilComponent.class);
            if (tilled.dry != 0) {
                tilled.dry = 0;
                soil.saveComponent(tilled);
            }
            return;
        }
        if (wet) {
            worldProvider.setBlock(position, blockManager.getBlock(DRY_SOIL));
        }
        TilledSoilComponent tilled = soil.getComponent(TilledSoilComponent.class);
        tilled.dry += elapsed;
        if (tilled.dry < DRY_OUT_MS) {
            soil.saveComponent(tilled);
            return;
        }
        Vector3i above = new Vector3i(position).add(0, 1, 0);
        EntityRef plant = blockEntityRegistry.getExistingBlockEntityAt(above);
        CropComponent crop = plant.getComponent(CropComponent.class);
        if (crop != null) {
            worldProvider.setBlock(above, blockManager.getBlock(BlockManager.AIR_ID));
            drop(crop.seed, above);
        }
        worldProvider.setBlock(position, blockManager.getBlock(DIRT));
    }

    private void grow(EntityRef plant, long elapsed) {
        if (!plant.exists()) {
            return;
        }
        CropComponent crop = plant.getComponent(CropComponent.class);
        if (crop == null || crop.next.isEmpty()) {
            return;
        }
        Vector3ic position = plant.getComponent(BlockComponent.class).getPosition();
        if (crop.needed == 0) {
            crop.needed = STAGE_MIN_MS + (long) (random.nextFloat() * (STAGE_MAX_MS - STAGE_MIN_MS));
        }
        if (canGrow(position)) {
            crop.grown += elapsed;
        }
        if (crop.grown >= crop.needed) {
            Block next = blockManager.getBlock(crop.next);
            if (next != null) {
                worldProvider.setBlock(position, next);
                return;
            }
        }
        plant.saveComponent(crop);
    }

    private boolean canGrow(Vector3ic position) {
        return isWet(worldProvider.getBlock(new Vector3i(position).sub(0, 1, 0)))
                && worldProvider.getTotalLight(position) >= MIN_LIGHT;
    }

    private boolean waterNear(Vector3ic soil) {
        Vector3i probe = new Vector3i();
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -WATER_REACH; dx <= WATER_REACH; dx++) {
                for (int dz = -WATER_REACH; dz <= WATER_REACH; dz++) {
                    if (worldProvider.getBlock(probe.set(soil).add(dx, dy, dz)).isWater()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static boolean isTilled(Block block) {
        String uri = block.getBlockFamily().getURI().toString();
        return uri.equalsIgnoreCase(DRY_SOIL) || uri.equalsIgnoreCase(WET_SOIL);
    }

    private static boolean isWet(Block block) {
        return block.getBlockFamily().getURI().toString().equalsIgnoreCase(WET_SOIL);
    }

    private void drop(String prefab, Vector3ic position) {
        if (prefab.isEmpty()) {
            return;
        }
        EntityRef item = entityManager.create(prefab);
        if (item.exists()) {
            item.send(new DropItemEvent(new Vector3f(position)));
        }
    }

    private void playDigSound(EntityRef blockEntity, Block block) {
        List<StaticSound> sounds = block.getSounds().getDigSounds();
        if (!sounds.isEmpty()) {
            blockEntity.send(new PlaySoundEvent(random.nextItem(sounds), 0.8f));
        }
    }
}
