// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

import java.util.List;

/**
 * Everything the game can hand out, sorted onto shelves once and for all. The screen only displays it.
 */
public interface Catalog {
    /** The entries of one tab, by display name. The list is immutable: the screen copies it before filtering. */
    List<CatalogEntry> entriesFor(CatalogTab tab);
}
