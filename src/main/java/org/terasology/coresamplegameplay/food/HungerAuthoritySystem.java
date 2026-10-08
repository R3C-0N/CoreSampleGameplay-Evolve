// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.food;

import com.google.common.collect.Maps;
import org.terasology.coresamplegameplay.creative.CreativeModeComponent;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.characters.AliveCharacterComponent;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.players.event.OnPlayerRespawnedEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.Assets;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.core.BaseRegenAuthoritySystem;
import org.terasology.module.health.events.BeforeRegenEvent;
import org.terasology.module.health.events.DoDamageEvent;

import java.util.Map;

/**
 * Empties the hunger gauge with time, fills it with what is eaten, and makes an empty one hurt.
 * <p>
 * <strong>Hunger takes nothing away, it withholds.</strong> Above a quarter of the gauge nothing changes; below
 * it the base regeneration of the Health module is refused, so a wound stays open until the character eats; at
 * nought the character loses a point of health every few seconds, and starving to death is possible. A meal is
 * therefore what lets one heal, which is what gives hunting its point.
 * <p>
 * <strong>Eating is a use, on the right button,</strong> like opening a carcass: {@link ActivateEvent} reaches
 * the held item at the authority, and the item's {@code consumedOnUse} has the Inventory module take one from the
 * stack afterwards. A full character refuses the meal, and the event is consumed so the item is kept.
 * <p>
 * The creative player neither hungers nor starves, but may still eat.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class HungerAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {

    private static final long PERIOD_MS = 1000;

    @In
    private EntityManager entityManager;
    @In
    private Time time;

    private Prefab starvationDamage;
    private long lastTick;

    /** Seconds of starvation each character has accrued towards its next point of damage. */
    private final Map<EntityRef, Float> starving = Maps.newHashMap();

    @Override
    public void initialise() {
        starvationDamage = Assets.getPrefab("CoreSampleGameplay:starvationDamage").orElse(null);
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

        for (EntityRef character : entityManager.getEntitiesWith(HungerComponent.class, AliveCharacterComponent.class)) {
            if (character.hasComponent(CreativeModeComponent.class)) {
                starving.remove(character);
                continue;
            }
            HungerComponent hunger = character.getComponent(HungerComponent.class);
            float before = hunger.currentFood;
            hunger.currentFood = Math.max(0f, hunger.currentFood - hunger.decayPerSecond * elapsed);
            if (hunger.currentFood != before) {
                character.saveComponent(hunger);
            }
            starve(character, hunger, elapsed);
        }
        starving.keySet().removeIf(character -> !character.exists());
    }

    private void starve(EntityRef character, HungerComponent hunger, float elapsed) {
        if (hunger.currentFood > 0f) {
            starving.remove(character);
            return;
        }
        float owed = starving.getOrDefault(character, 0f) + elapsed;
        int points = (int) (owed / hunger.starvationPeriod);
        starving.put(character, owed - points * hunger.starvationPeriod);
        if (points > 0) {
            character.send(new DoDamageEvent(points, starvationDamage));
        }
    }

    @ReceiveEvent
    public void eat(ActivateEvent event, EntityRef item, FoodComponent food) {
        EntityRef eater = event.getInstigator();
        HungerComponent hunger = eater.getComponent(HungerComponent.class);
        if (hunger == null || hunger.currentFood >= hunger.maxFood) {
            event.consume();
            return;
        }
        hunger.currentFood = Math.min(hunger.maxFood, hunger.currentFood + food.nourishment);
        eater.saveComponent(hunger);
    }

    @ReceiveEvent
    public void withholdRegen(BeforeRegenEvent event, EntityRef character, HungerComponent hunger) {
        if (BaseRegenAuthoritySystem.BASE_REGEN.equals(event.getId())
                && hunger.currentFood < hunger.regenThreshold * hunger.maxFood
                && !character.hasComponent(CreativeModeComponent.class)) {
            event.consume();
        }
    }

    /** Coming back from death with an empty stomach would only starve the character again. */
    @ReceiveEvent
    public void refill(OnPlayerRespawnedEvent event, EntityRef character, HungerComponent hunger) {
        hunger.currentFood = hunger.maxFood;
        character.saveComponent(hunger);
        starving.remove(character);
    }
}
