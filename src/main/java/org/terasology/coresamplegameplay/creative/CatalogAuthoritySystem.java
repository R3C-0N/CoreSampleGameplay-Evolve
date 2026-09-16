// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import com.google.common.primitives.UnsignedBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.logic.characters.CharacterMovementComponent;
import org.terasology.engine.logic.characters.MovementMode;
import org.terasology.engine.logic.characters.events.SetMovementModeEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.inventory.events.GiveItemEvent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.items.BlockItemFactory;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.module.inventory.systems.InventoryUtils;

/**
 * What the server does with a click in the catalogue, and with a double tap on the jump key.
 * <p>
 * Both handlers require {@link CreativeModeComponent} on the character, and that requirement is the whole
 * access check: a client that forged either event while in survival would be asking an entity that does not
 * carry the component, and nothing would answer.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CatalogAuthoritySystem extends BaseComponentSystem {
    private static final Logger logger = LoggerFactory.getLogger(CatalogAuthoritySystem.class);

    /** What a stack holds, matching {@code ItemComponent.maxStackSize} for blocks, which carry no prefab. */
    private static final int BLOCK_STACK = 99;

    @In
    private EntityManager entityManager;
    @In
    private PrefabManager prefabManager;
    @In
    private BlockManager blockManager;
    @In
    private InventoryManager inventoryManager;

    private BlockItemFactory blockItemFactory;

    @Override
    public void initialise() {
        blockItemFactory = new BlockItemFactory(entityManager);
    }

    @ReceiveEvent(components = CreativeModeComponent.class)
    public void giveFromCatalogue(GiveCatalogEntryRequest event, EntityRef character) {
        EntityRef item = event.isBlock() ? blockStack(event) : itemStack(event);
        if (item == null || !item.exists()) {
            logger.warn("Creative catalogue asked for '{}', which no longer resolves", event.getUri());
            return;
        }
        GiveItemEvent give = new GiveItemEvent(character);
        item.send(give);
        if (!give.isHandled()) {
            item.destroy();
        }
    }

    private EntityRef itemStack(GiveCatalogEntryRequest event) {
        Prefab prefab = prefabManager.getPrefab(event.getUri());
        if (prefab == null) {
            return null;
        }
        ItemComponent template = prefab.getComponent(ItemComponent.class);
        if (template == null) {
            return null;
        }
        int amount = event.isWholeStack() ? Math.max(1, UnsignedBytes.toInt(template.maxStackSize)) : 1;
        EntityRef item = entityManager.create(prefab);
        if (amount > 1) {
            ItemComponent created = item.getComponent(ItemComponent.class);
            created.stackCount = UnsignedBytes.checkedCast(amount);
            item.saveComponent(created);
        }
        return item;
    }

    private EntityRef blockStack(GiveCatalogEntryRequest event) {
        BlockFamily family = blockManager.getBlockFamily(event.getUri());
        if (family == null) {
            return null;
        }
        boolean stackable = family.getArchetypeBlock().isStackable();
        int amount = event.isWholeStack() && stackable ? BLOCK_STACK : 1;
        return blockItemFactory.newInstance(family, amount);
    }

    /**
     * The bin. Emptying takes every slot, equipment included — "entirely" was the word — which in creative is
     * one click away from being undone.
     */
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void emptyInventory(ClearInventoryRequest event, EntityRef character) {
        if (!event.isEverything()) {
            CharacterComponent characterComponent = character.getComponent(CharacterComponent.class);
            if (characterComponent != null) {
                destroyStackAt(characterComponent.movingItem, character, 0);
            }
            return;
        }
        InventoryComponent inventory = character.getComponent(InventoryComponent.class);
        if (inventory == null) {
            return;
        }
        for (int slot = 0; slot < inventory.itemSlots.size(); slot++) {
            destroyStackAt(character, character, slot);
        }
    }

    private void destroyStackAt(EntityRef inventory, EntityRef instigator, int slot) {
        EntityRef item = InventoryUtils.getItemAt(inventory, slot);
        if (item.exists()) {
            inventoryManager.removeItem(inventory, instigator, slot, true, InventoryUtils.getStackCount(item));
        }
    }

    /**
     * Flying is a mode of movement, and {@code SetMovementModeEvent} toggles back to walking when handed the
     * mode already in force — so the state is read first, and the component keeps it for the next respawn.
     */
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void toggleFlight(ToggleFlightRequest event, EntityRef character, CreativeModeComponent creative) {
        CharacterMovementComponent movement = character.getComponent(CharacterMovementComponent.class);
        boolean flying = movement != null && movement.mode == MovementMode.FLYING;
        character.send(new SetMovementModeEvent(flying ? MovementMode.WALKING : MovementMode.FLYING));
        creative.flying = !flying;
        character.saveComponent(creative);
    }
}
