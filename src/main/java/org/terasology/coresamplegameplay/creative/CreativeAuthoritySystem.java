// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.EventPriority;
import org.terasology.engine.entitySystem.event.Priority;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.AffectItemUseCooldownTimeEvent;
import org.terasology.engine.logic.characters.events.AttackEvent;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.health.DestroyEvent;
import org.terasology.engine.logic.health.EngineDamageTypes;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.entity.CreateBlockDropsEvent;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.bestiaire.BeforeHuntedEvent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.module.health.events.BeforeDamagedEvent;

/**
 * The six rules of creative mode, each one slotted into an existing consumable event.
 * <p>
 * Priorities run {@code CRITICAL 200 > HIGH 150 > NORMAL 100 > LOW 50 > TRIVIAL 0}, and a consumed event stops
 * being propagated — which is the whole mechanism here. Two of the five events are received on the character,
 * and can be filtered by component; the other three are received on an item or on a block, so they have to ask
 * their instigator.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class CreativeAuthoritySystem extends BaseComponentSystem {

    /**
     * How long a creative character waits between one action and the next, in milliseconds.
     * <p>
     * Two places this does not reach, both in the engine and neither worth a change there. An empty hand never
     * raises the event at all - {@code CharacterSystem.onItemUse} adds a flat 200 ms when nothing is held and
     * asks nobody - so clearing blocks bare-handed runs a shade quicker than this; the fifty milliseconds are
     * not a difference anyone can feel, and the pause is there either way. And in multiplayer this system is
     * authority only while that engine method runs on both sides, so a remote client works out the item's own
     * cooldown while the server works out this one, and a click landing between the two is quietly dropped
     * rather than acted on. Single player is its own authority, so the two cannot disagree there.
     */
    public static final float ACTION_DELAY_MS = 250f;

    /** Fall damage comes through this event too, so nothing else is needed to survive a drop. */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void takeNoDamage(BeforeDamagedEvent event, EntityRef character) {
        event.consume();
    }

    /**
     * Nothing hunts a builder.
     * <p>
     * Invulnerability alone would have left the wolves circling: they would come, they would bite, and the
     * blows would land on nothing — a fight with no stakes played out on top of whatever was being built.
     * The bestiary asks before it settles on a prey, and this is the answer. It is the only consumer of that
     * question today, and it is the reason the question exists rather than a component the wolf reads: the
     * mode is declared here, in a module that depends on the bestiary, so the bestiary cannot see it.
     */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void goUnnoticed(BeforeHuntedEvent event, EntityRef character) {
        event.consume();
    }

    /**
     * A fixed pause between one action and the next, whatever is in hand.
     * <p>
     * Creative used to wait for nothing at all, and a held button laid a wall or cleared a hillside faster than
     * anyone could see what they were doing. A quarter of a second is the lightest brake that is felt: it costs
     * nothing to someone building deliberately, and it ends the accidental burst of a double click.
     * <p>
     * The multiplier throws the item's own cooldown away and the flat addition puts this one in its place, so
     * the pace is the character's rather than whatever happens to be held - a hand has a rhythm, not a tool.
     * That also means one timer for both buttons, since the engine keeps a single
     * {@code nextItemUseTime}: no breaking a block the instant after placing one, and none the other way round.
     */
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = CreativeModeComponent.class)
    public void actAtASteadyPace(AffectItemUseCooldownTimeEvent event, EntityRef character) {
        event.multiply(0f);
        event.postAdd(ACTION_DELAY_MS);
    }

    /**
     * The block is placed by {@code BlockItemSystem} at {@code PRIORITY_NORMAL}; consuming here, below it, only
     * keeps {@code ItemAuthoritySystem.usedItem} at {@code PRIORITY_TRIVIAL} from spending the stack.
     * <p>
     * Anything else listening at {@code TRIVIAL} would be cut off as well. Today nothing else is there, and the
     * scraping of soil sits at {@code NORMAL}, so it still runs.
     */
    @Priority(EventPriority.PRIORITY_LOW)
    @ReceiveEvent(components = BlockItemComponent.class)
    public void placeWithoutSpending(ActivateEvent event, EntityRef item) {
        if (isCreative(event.getInstigator())) {
            event.consume();
        }
    }

    /**
     * One blow, one block. Destroying it here, above {@code BlockDamageAuthoritySystem}, means the block never
     * gets the {@code HealthComponent} that would have made it take several hits.
     */
    @Priority(EventPriority.PRIORITY_HIGH)
    @ReceiveEvent(components = BlockComponent.class)
    public void breakInOneBlow(AttackEvent event, EntityRef blockEntity) {
        if (isCreative(event.getInstigator())) {
            blockEntity.send(new DestroyEvent(event.getInstigator(), event.getDirectCause(),
                    EngineDamageTypes.PHYSICAL.get()));
            event.consume();
        }
    }

    /**
     * Nothing falls in creative: the catalogue gives what is wanted, and a floor covered in what was cleared is
     * only in the way. This runs above {@code HarvestAuthoritySystem}, whose tool grades no longer apply.
     */
    @Priority(EventPriority.PRIORITY_CRITICAL)
    @ReceiveEvent(components = BlockComponent.class)
    public void dropNothing(CreateBlockDropsEvent event, EntityRef blockEntity) {
        if (isCreative(event.getInstigator())) {
            event.consume();
        }
    }

    private static boolean isCreative(EntityRef character) {
        return character.hasComponent(CreativeModeComponent.class);
    }
}
