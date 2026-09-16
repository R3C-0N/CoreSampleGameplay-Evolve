// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.creative;

/**
 * The shelves of the creative catalogue, in the order their buttons appear.
 * <p>
 * Three of them are empty, and not by oversight: the repository holds no coloured block and no dyeing system,
 * no food and no potion, and not one creature — a single prefab in the whole game carries a character. They
 * are shown all the same, so that what is missing is visible rather than merely absent.
 */
public enum CatalogTab {
    CONSTRUCTION("Construction", "Blocs de construction"),
    COLORES("Colorés", "Blocs colorés"),
    DECORATION("Nature", "Décoration et nature"),
    MECANISMES("Mécanismes", "Mécanismes"),
    EQUIPEMENT("Équipement", "Équipement et combat"),
    CONSOMMABLES("Consommables", "Consommables"),
    GENERATION("Créatures", "Œufs d'apparition"),
    MATERIAUX("Matériaux", "Ingrédients et matériaux"),
    TOUT("Tout", "Tout le contenu du jeu");

    private final String label;
    private final String title;

    CatalogTab(String label, String title) {
        this.label = label;
        this.title = title;
    }

    /** What the tab button says: short, because nine of them share the width. */
    public String getLabel() {
        return label;
    }

    /** What the heading above the grid says, in full. */
    public String getTitle() {
        return title;
    }

    /** The id its button carries in the layout, {@code tabCONSTRUCTION} and so on. */
    public String getButtonId() {
        return "tab" + name();
    }
}
