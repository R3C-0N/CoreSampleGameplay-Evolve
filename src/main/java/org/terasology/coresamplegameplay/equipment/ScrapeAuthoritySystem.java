// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.equipment;

import org.joml.Vector3f;
import org.terasology.engine.audio.StaticSound;
import org.terasology.engine.audio.events.PlaySoundEvent;
import org.terasology.engine.core.Time;
import org.terasology.engine.entitySystem.entity.EntityBuilder;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.entitySystem.systems.UpdateSubscriberSystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.inventory.events.DropItemEvent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.utilities.random.FastRandom;
import org.terasology.engine.utilities.random.Random;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.entity.damage.BlockDamageModifierComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scraping soil with a shovel, on the use button: it turns up flint without breaking the block.
 * <p>
 * A pass takes exactly as long as breaking the block would. Each use puts in the same strength as a hit — the
 * item's base damage, times its damage type's multiplier for the block — at the same cooldown, and a pass is
 * complete once that adds up to the block's hardness. Then a burst of soil and a short pause mark the end of it,
 * and one pass in five turns up a flint.
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class ScrapeAuthoritySystem extends BaseComponentSystem implements UpdateSubscriberSystem {
    /** Between two passes: uses in the meantime are taken, and put nothing in. */
    static final long PAUSE_MS = 300;
    /** Uses come every cooldown; past this without one, the scraper has let go. */
    static final long STILL_SCRAPING_MS = 450;
    /** A pass left alone this long starts over, as a damaged block heals. */
    private static final long FORGET_AFTER_MS = 1000;
    private static final float CLEANUP_INTERVAL = 0.5f;

    private static final float FLINT_CHANCE = 0.2f;
    private static final String FLINT = "CoreAssets:Flint";
    private static final String BURST = "CoreSampleGameplay:scrapeBurst";
    private static final Set<BlockUri> SCRAPABLE = Set.of(
            new BlockUri("CoreAssets:Dirt"), new BlockUri("CoreAssets:Grass"), new BlockUri("CoreAssets:Gravel"));

    @In
    private EntityManager entityManager;
    @In
    private Time time;

    private final Random random = new FastRandom();
    private float sinceCleanup;

    @ReceiveEvent
    public void scrape(ActivateEvent event, EntityRef item, ToolComponent tool, ItemComponent itemComponent) {
        if (!"shovel".equals(tool.family)) {
            return;
        }
        EntityRef target = event.getTarget();
        BlockComponent blockComponent = target.getComponent(BlockComponent.class);
        if (blockComponent == null) {
            return;
        }
        Block block = blockComponent.getBlock();
        if (!SCRAPABLE.contains(block.getBlockFamily().getURI())) {
            return;
        }
        event.consume();

        long now = time.getGameTimeInMs();
        ScrapingComponent scraping = target.getComponent(ScrapingComponent.class);
        boolean fresh = scraping == null;
        if (fresh) {
            scraping = new ScrapingComponent();
        } else if (now - scraping.lastScrapeTime > FORGET_AFTER_MS) {
            scraping.progress = 0;
        }
        if (fresh || now - scraping.lastScrapeTime > STILL_SCRAPING_MS) {
            scraping.startTime = now;
        }
        scraping.scraper = event.getInstigator();
        scraping.hardness = block.getHardness();
        boolean pausing = !fresh && now - scraping.passEndTime < PAUSE_MS;
        scraping.lastScrapeTime = now;
        if (!pausing) {
            scraping.progress += strength(itemComponent, block);
            if (scraping.progress >= scraping.hardness) {
                scraping.progress = 0;
                scraping.passEndTime = now;
                Vector3f face = faceOf(blockComponent, event.getHitNormal());
                burst(face);
                playDigSound(target, block, 0.8f);
                if (random.nextFloat() < FLINT_CHANCE) {
                    entityManager.create(FLINT).send(new DropItemEvent(feetOf(event.getInstigator(), face)));
                }
            } else {
                playDigSound(target, block, 0.3f);
            }
        }
        if (fresh) {
            target.addComponent(scraping);
        } else {
            target.saveComponent(scraping);
        }
    }

    @Override
    public void update(float delta) {
        sinceCleanup += delta;
        if (sinceCleanup < CLEANUP_INTERVAL) {
            return;
        }
        sinceCleanup = 0;
        long now = time.getGameTimeInMs();
        List<EntityRef> forgotten = new ArrayList<>();
        for (EntityRef block : entityManager.getEntitiesWith(ScrapingComponent.class)) {
            if (now - block.getComponent(ScrapingComponent.class).lastScrapeTime > FORGET_AFTER_MS) {
                forgotten.add(block);
            }
        }
        forgotten.forEach(block -> block.removeComponent(ScrapingComponent.class));
    }

    /** What a hit with this item would take off the block, see {@code BlockDamageAuthoritySystem}. */
    private static int strength(ItemComponent item, Block block) {
        int strength = item.baseDamage;
        Prefab damageType = item.damageType;
        BlockDamageModifierComponent modifier = damageType == null ? null : damageType.getComponent(BlockDamageModifierComponent.class);
        if (modifier != null) {
            for (String category : block.getBlockFamily().getCategories()) {
                Integer factor = modifier.materialDamageMultiplier.get(category);
                if (factor != null) {
                    strength *= factor;
                }
            }
        }
        return strength;
    }

    /** Just outside the scraped face, so that what comes out lands at the player's feet rather than in the block. */
    private static Vector3f faceOf(BlockComponent blockComponent, Vector3f hitNormal) {
        Vector3f position = new Vector3f(blockComponent.getPosition());
        if (hitNormal != null) {
            position.add(new Vector3f(hitNormal).mul(0.7f));
        }
        return position;
    }

    /**
     * Where the flint lands: at the scraper's feet. Left on the scraped face, out of reach, it stood between the
     * player's aim and the block, and the scraping stopped as soon as one fell.
     */
    private static Vector3f feetOf(EntityRef scraper, Vector3f fallback) {
        LocationComponent location = scraper.getComponent(LocationComponent.class);
        if (location == null) {
            return fallback;
        }
        Vector3f position = location.getWorldPosition(new Vector3f());
        return position.isFinite() ? position : fallback;
    }

    private void burst(Vector3f position) {
        EntityBuilder builder = entityManager.newBuilder(BURST);
        LocationComponent location = builder.getComponent(LocationComponent.class);
        if (location != null) {
            location.setWorldPosition(position);
            builder.build();
        }
    }

    private void playDigSound(EntityRef blockEntity, Block block, float volume) {
        List<StaticSound> sounds = block.getSounds().getDigSounds();
        if (!sounds.isEmpty()) {
            blockEntity.send(new PlaySoundEvent(random.nextItem(sounds), volume));
        }
    }
}
