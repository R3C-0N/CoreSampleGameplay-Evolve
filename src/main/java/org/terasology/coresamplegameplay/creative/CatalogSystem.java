// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.coresamplegameplay.equipment.ArmorComponent;
import org.terasology.coresamplegameplay.equipment.ToolComponent;
import org.terasology.coresamplegameplay.equipment.WeaponComponent;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.DisplayNameComponent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.registry.Share;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockExplorer;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.gestalt.assets.management.AssetManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reads the whole game once at startup and sorts it onto the shelves of {@link CatalogTab}.
 * <p>
 * Blocks come from {@link BlockExplorer}, not from {@code BlockManager.listRegisteredBlockFamilies()}, which
 * only ever returns the families a world has already had reason to load — a catalogue built from that one
 * would grow as the player walks.
 * <p>
 * Nothing here is recomputed afterwards. A block added while the game runs will not appear, and that is the
 * price of a screen that costs nothing per frame.
 */
@RegisterSystem
@Share(Catalog.class)
public class CatalogSystem extends BaseComponentSystem implements Catalog {
    private static final Logger logger = LoggerFactory.getLogger(CatalogSystem.class);

    /** The shape a freeform family takes when nothing asks for another, as {@code giveBlock} does. */
    private static final ResourceUrn CUBE = new ResourceUrn("engine:cube");

    /** Storage, workstations and what explodes: the closest this game has to a mechanism. */
    private static final Set<String> MECHANISM_BLOCKS = Set.of("tnt", "chest", "furnace", "trunkleft", "trunkright",
            "workbench", "astralforge", "ultimateastralforge", "lapidarytower");

    /** Blocks that furnish rather than build, and that no category tells apart. */
    private static final Set<String> DECORATION_BLOCKS = Set.of("torch", "ladder", "doorbottom", "doortop",
            "door-bar-bottom", "door-bar-top", "door-cyan-bottom", "door-cyan-top");

    /** The upstream tools, which predate {@code Tool} and carry no component to recognise them by. */
    private static final Set<String> UPSTREAM_TOOLS = Set.of("axe", "axeimproved", "pickaxe", "pickaxeimproved",
            "shovel", "shovelimproved", "gooeysfist", "gun", "gunimproved");

    private static final Set<String> MECHANISM_ITEMS = Set.of("dynamite", "dynamitebundle", "fuselong", "fuseshort");

    private static final Set<String> FURNITURE_ITEMS = Set.of("door", "door-bar", "door-cyan", "trunk");

    @In
    private PrefabManager prefabManager;
    @In
    private BlockManager blockManager;
    @In
    private AssetManager assetManager;

    private final Map<CatalogTab, List<CatalogEntry>> shelves = new EnumMap<>(CatalogTab.class);

    @Override
    public void initialise() {
        for (CatalogTab tab : CatalogTab.values()) {
            shelves.put(tab, new ArrayList<>());
        }

        BlockExplorer explorer = new BlockExplorer(assetManager);
        List<BlockUri> uris = new ArrayList<>(explorer.getAvailableBlockFamilies());
        // Most of the game's blocks are freeform — stone, soil, planks all accept any shape — and they come
        // back from a separate call, shapeless. Taking only the first list left the catalogue three blocks of
        // building material. The cube is the shape they are given anywhere else, so it is the shape here.
        for (BlockUri freeform : explorer.getFreeformBlockFamilies()) {
            uris.add(new BlockUri(freeform.getBlockFamilyDefinitionUrn(), CUBE));
        }

        int blocks = 0;
        for (BlockUri uri : uris) {
            BlockFamily family = blockManager.getBlockFamily(uri);
            if (family == null) {
                continue;
            }
            String name = family.getDisplayName();
            shelve(CatalogEntry.ofBlock(family, name == null || name.isEmpty() ? shortName(uri) : name,
                    tabOf(family)));
            blocks++;
        }

        int items = 0;
        for (Prefab prefab : prefabManager.listPrefabs(ItemComponent.class)) {
            // The engine's own item prefabs are the bases every real item inherits from; they are not items.
            if ("engine".equalsIgnoreCase(prefab.getUrn().getModuleName().toString())) {
                continue;
            }
            shelve(CatalogEntry.ofItem(prefab, itemName(prefab), tabOf(prefab)));
            items++;
        }

        Comparator<CatalogEntry> byName = Comparator.comparing(entry -> CatalogEntry.fold(entry.getName()));
        for (CatalogTab tab : CatalogTab.values()) {
            shelves.get(tab).sort(byName);
            shelves.put(tab, List.copyOf(shelves.get(tab)));
        }

        logger.info("Creative catalogue: {} blocks, {} items", blocks, items);
        for (CatalogTab tab : CatalogTab.values()) {
            logger.info("  {} — {} entries", tab.getTitle(), shelves.get(tab).size());
        }
    }

    @Override
    public List<CatalogEntry> entriesFor(CatalogTab tab) {
        return shelves.getOrDefault(tab, List.of());
    }

    /** Every entry lands on its own shelf and on {@code TOUT}, which is what the search reads. */
    private void shelve(CatalogEntry entry) {
        shelves.get(entry.getTab()).add(entry);
        shelves.get(CatalogTab.TOUT).add(entry);
    }

    /**
     * Blocks are sorted by what the game already knows about them, then by name for the handful that carry no
     * category. Whatever is left over builds: a block filed under the wrong tab can be seen, a block filed
     * nowhere cannot.
     */
    private static CatalogTab tabOf(BlockFamily family) {
        String name = shortName(family.getURI()).toLowerCase(Locale.ROOT);
        if (MECHANISM_BLOCKS.contains(name)) {
            return CatalogTab.MECANISMES;
        }
        if (DECORATION_BLOCKS.contains(name) || family.hasCategory("leaf")) {
            return CatalogTab.DECORATION;
        }
        // Flowers, mushrooms, grass and cotton share the plant template, which sets no category at all — but
        // it is the only thing in the game that makes a billboard needing ground under it.
        Block archetype = family.getArchetypeBlock();
        if (archetype != null && archetype.isDoubleSided() && archetype.isSupportRequired() && archetype.isPenetrable()) {
            return CatalogTab.DECORATION;
        }
        return CatalogTab.CONSTRUCTION;
    }

    /**
     * An item says what it is by the component it carries. What carries none is an ingredient — which is true
     * of every prefab under {@code materials/}, and of nothing else.
     */
    private static CatalogTab tabOf(Prefab prefab) {
        if (prefab.hasComponent(ToolComponent.class) || prefab.hasComponent(WeaponComponent.class)
                || prefab.hasComponent(ArmorComponent.class)) {
            return CatalogTab.EQUIPEMENT;
        }
        String name = prefab.getUrn().getResourceName().toString().toLowerCase(Locale.ROOT);
        if (UPSTREAM_TOOLS.contains(name)) {
            return CatalogTab.EQUIPEMENT;
        }
        if (MECHANISM_ITEMS.contains(name)) {
            return CatalogTab.MECANISMES;
        }
        if (FURNITURE_ITEMS.contains(name)) {
            return CatalogTab.DECORATION;
        }
        return CatalogTab.MATERIAUX;
    }

    private static String itemName(Prefab prefab) {
        DisplayNameComponent displayName = prefab.getComponent(DisplayNameComponent.class);
        if (displayName != null && displayName.name != null && !displayName.name.isEmpty()) {
            return displayName.name;
        }
        return prefab.getUrn().getResourceName().toString();
    }

    private static String shortName(BlockUri uri) {
        return uri.getBlockFamilyDefinitionUrn().getResourceName().toString();
    }

}
