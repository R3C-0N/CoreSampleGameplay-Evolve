// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.terasology.coresamplegameplay.food.HungerComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.layers.hud.CoreHudWidget;
import org.terasology.module.health.components.HealthComponent;
import org.terasology.nui.databinding.ReadOnlyBinding;

import java.util.function.Function;

/**
 * Replaces the row of hearts through an override of {@code Health:healthHud}. Health and hunger are real; stamina
 * and mana do not exist yet, so their globes stand full rather than show a number that means nothing.
 */
public class ResourceOrbsHud extends CoreHudWidget {
    @In
    private LocalPlayer localPlayer;

    @Override
    public void initialise() {
        ResourceOrb health = find("health", ResourceOrb.class);
        health.bindValue(healthBinding(component -> (float) component.currentHealth));
        health.bindMax(healthBinding(component -> (float) component.maxHealth));
        ResourceOrb hunger = find("hunger", ResourceOrb.class);
        if (hunger != null) {
            hunger.bindValue(hungerBinding(component -> component.currentFood));
            hunger.bindMax(hungerBinding(component -> component.maxFood));
        }
        for (String id : new String[]{"stamina", "mana"}) {
            ResourceOrb orb = find(id, ResourceOrb.class);
            if (orb != null) {
                orb.setMax(100f);
                orb.setValue(100f);
            }
        }
    }

    private ReadOnlyBinding<Float> healthBinding(Function<HealthComponent, Float> read) {
        return new ReadOnlyBinding<Float>() {
            @Override
            public Float get() {
                HealthComponent component = localPlayer.getCharacterEntity().getComponent(HealthComponent.class);
                return component == null ? 0f : read.apply(component);
            }
        };
    }

    private ReadOnlyBinding<Float> hungerBinding(Function<HungerComponent, Float> read) {
        return new ReadOnlyBinding<Float>() {
            @Override
            public Float get() {
                HungerComponent component = localPlayer.getCharacterEntity().getComponent(HungerComponent.class);
                return component == null ? 0f : read.apply(component);
            }
        };
    }
}
