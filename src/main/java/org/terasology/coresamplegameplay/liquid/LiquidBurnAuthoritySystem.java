// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.liquid;

import com.google.common.collect.Maps;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.characters.CharacterMovementComponent;
import org.terasology.engine.logic.location.Location;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.Assets;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.module.health.components.HealthComponent;
import org.terasology.module.health.events.DoDamageEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * Burns whoever stands in a liquid hot enough to do it.
 * <p>
 * The threshold is the block's declared {@code warmth}, not the warmth that travels through the chunk as a
 * light channel. The two are easily confused and only one is safe here: the propagated channel is rebuilt
 * locally by each client and never reaches the server's save or the network, whereas the declared value sits
 * on the shared block table and reads the same everywhere.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class LiquidBurnAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    /**
     * Below this a liquid is merely warm. Above it, the damage a second is the warmth less this - so lava,
     * at fifteen, takes eight a second.
     */
    public static final int BURN_THRESHOLD = 7;

    /**
     * Above this the burning shows: flames on the character and a veil on their screen.
     */
    public static final int FLAME_THRESHOLD = 12;

    private static final String FLAMES = "CoreSampleGameplay:burningFlames";
    private static final long PERIOD_MS = 500;

    @In
    private EntityManager entityManager;
    @In
    private WorldProvider worldProvider;
    @In
    private Time time;

    private Prefab fireDamage;
    private long lastBurn;

    /**
     * The fractional damage each character has carried over, since a damage event only takes whole points.
     */
    private final Map<EntityRef, Float> remainders = Maps.newHashMap();

    /**
     * The flame emitter attached to each burning character, so it can be taken away again.
     */
    private final Map<EntityRef, EntityRef> flames = Maps.newHashMap();

    @Override
    public void initialise() {
        fireDamage = Assets.getPrefab("CoreSampleGameplay:fireDamage").orElse(null);
    }

    @Override
    public void update(float delta) {
        long now = time.getGameTimeInMs();
        if (lastBurn + PERIOD_MS > now) {
            return;
        }
        float elapsed = (now - lastBurn) / 1000f;
        lastBurn = now;

        for (EntityRef character : entityManager.getEntitiesWith(HealthComponent.class,
                LocationComponent.class, CharacterMovementComponent.class)) {
            burn(character, hottestLiquidAt(character), elapsed);
        }
        forgetTheDeparted();
    }

    /**
     * The warmth of the hottest liquid the character is standing in, nought if it is standing in none.
     * <p>
     * The two heights sampled are the ones the mover itself uses to decide on swimming, so a character is
     * burnt by exactly the liquid it is swimming in.
     */
    private int hottestLiquidAt(EntityRef character) {
        LocationComponent location = character.getComponent(LocationComponent.class);
        CharacterMovementComponent movement = character.getComponent(CharacterMovementComponent.class);
        if (location == null || movement == null) {
            return 0;
        }
        Vector3f position = location.getWorldPosition(new Vector3f());
        if (!position.isFinite()) {
            return 0;
        }
        int warmth = warmthAt(position.x, position.y + 0.5f * movement.height, position.z);
        return Math.max(warmth, warmthAt(position.x, position.y - 0.25f * movement.height, position.z));
    }

    private int warmthAt(float x, float y, float z) {
        Block block = worldProvider.getBlock(new Vector3f(x, y, z));
        return block.isLiquid() ? block.getWarmth() : 0;
    }

    private void burn(EntityRef character, int warmth, float elapsed) {
        if (warmth <= BURN_THRESHOLD) {
            remainders.remove(character);
            extinguish(character);
            return;
        }

        float owed = (warmth - BURN_THRESHOLD) * elapsed + remainders.getOrDefault(character, 0f);
        int points = (int) owed;
        remainders.put(character, owed % 1);
        if (points > 0) {
            character.send(new DoDamageEvent(points, fireDamage));
        }

        if (warmth > FLAME_THRESHOLD) {
            ignite(character, warmth);
        } else {
            extinguish(character);
        }
    }

    private void ignite(EntityRef character, int warmth) {
        BurningComponent burning = character.getComponent(BurningComponent.class);
        if (burning == null) {
            burning = new BurningComponent();
            burning.warmth = warmth;
            character.addComponent(burning);
        } else if (burning.warmth != warmth) {
            burning.warmth = warmth;
            character.saveComponent(burning);
        }
        if (flames.containsKey(character) && flames.get(character).exists()) {
            return;
        }
        EntityBuilder builder = entityManager.newBuilder(FLAMES);
        if (builder.getComponent(LocationComponent.class) == null) {
            return;
        }
        EntityRef emitter = builder.build();
        // No existing effect in the code base rides a moving entity - the three that exist drop a puff at a
        // fixed world position - so this attachment is the first thing to check on screen.
        Location.attachChild(character, emitter, new Vector3f(0, 0.4f, 0), new Quaternionf());
        flames.put(character, emitter);
    }

    private void extinguish(EntityRef character) {
        if (character.hasComponent(BurningComponent.class)) {
            character.removeComponent(BurningComponent.class);
        }
        EntityRef emitter = flames.remove(character);
        if (emitter != null && emitter.exists()) {
            Location.removeChild(character, emitter);
            emitter.destroy();
        }
    }

    private void forgetTheDeparted() {
        for (Iterator<Map.Entry<EntityRef, EntityRef>> it = flames.entrySet().iterator(); it.hasNext();) {
            Map.Entry<EntityRef, EntityRef> entry = it.next();
            if (!entry.getKey().exists()) {
                if (entry.getValue().exists()) {
                    entry.getValue().destroy();
                }
                it.remove();
            }
        }
        remainders.keySet().removeIf(character -> !character.exists());
    }
}
