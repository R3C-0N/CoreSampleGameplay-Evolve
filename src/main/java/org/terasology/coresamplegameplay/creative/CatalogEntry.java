// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.world.block.family.BlockFamily;

import java.util.Locale;

/**
 * One line of the catalogue: something the game can hand out, and what to draw for it.
 * <p>
 * It holds a prefab or a block family, never an entity. Four hundred and sixty live entities, kept only so
 * that a grid has something to look at, would be four hundred and sixty entities the server has to carry.
 */
public final class CatalogEntry {
    /** Letters with and without their accents, so that {@code Écorce} files under E and "epee" finds "Épée". */
    private static final String ACCENTED = "àâäãáçéèêëíìîïñóòôöõúùûüýÿ";
    private static final String PLAIN = "aaaaaceeeeiiiinooooouuuuyy";

    private final String uri;
    private final String name;
    private final String searchKey;
    private final CatalogTab tab;
    private final Prefab itemPrefab;
    private final BlockFamily blockFamily;

    private CatalogEntry(String uri, String name, CatalogTab tab, Prefab itemPrefab, BlockFamily blockFamily) {
        this.uri = uri;
        this.name = name;
        this.tab = tab;
        this.itemPrefab = itemPrefab;
        this.blockFamily = blockFamily;
        this.searchKey = fold(name + " " + uri);
    }

    /**
     * Lowercases and strips accents. A module may not touch {@code java.text.Collator} nor
     * {@code java.text.Normalizer} — the sandbox denies the class outright and skips the system that asked for
     * it, with nothing but a warning in the log — so the fold is done by hand over the letters French uses.
     */
    public static String fold(String text) {
        String lower = text.toLowerCase(Locale.FRENCH);
        StringBuilder folded = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            int accent = ACCENTED.indexOf(c);
            folded.append(accent < 0 ? c : PLAIN.charAt(accent));
        }
        return folded.toString();
    }

    public static CatalogEntry ofItem(Prefab prefab, String name, CatalogTab tab) {
        return new CatalogEntry(prefab.getUrn().toString(), name, tab, prefab, null);
    }

    public static CatalogEntry ofBlock(BlockFamily family, String name, CatalogTab tab) {
        return new CatalogEntry(family.getURI().toString(), name, tab, null, family);
    }

    /** What {@link GiveCatalogEntryRequest} names to the server: an item prefab urn, or a block family uri. */
    public String getUri() {
        return uri;
    }

    public String getName() {
        return name;
    }

    public CatalogTab getTab() {
        return tab;
    }

    public boolean isBlock() {
        return blockFamily != null;
    }

    /** Null for a block. */
    public Prefab getItemPrefab() {
        return itemPrefab;
    }

    /** Null for an item. */
    public BlockFamily getBlockFamily() {
        return blockFamily;
    }

    /** Matches the displayed name and the uri both, so that "CoreAssets" and "cuivre" each find something. */
    public boolean matches(String foldedQuery) {
        return searchKey.contains(foldedQuery);
    }
}
