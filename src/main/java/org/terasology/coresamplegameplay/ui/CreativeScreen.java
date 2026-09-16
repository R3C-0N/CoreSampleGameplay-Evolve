// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.coresamplegameplay.creative.Catalog;
import org.terasology.coresamplegameplay.creative.CatalogEntry;
import org.terasology.coresamplegameplay.creative.CatalogTab;
import org.terasology.coresamplegameplay.creative.ClearInventoryRequest;
import org.terasology.coresamplegameplay.creative.GiveCatalogEntryRequest;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.nui.CoreScreenLayer;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.module.inventory.systems.InventoryUtils;
import org.terasology.nui.databinding.ReadOnlyBinding;
import org.terasology.nui.widgets.ResettableUIText;
import org.terasology.nui.widgets.TextChangeEventListener;
import org.terasology.nui.widgets.UIButton;
import org.terasology.nui.widgets.UILabel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The creative screen: everything the game holds, on nine shelves, above the equipment and the toolbar.
 * <p>
 * It is not an override of the inventory screen — {@code CharacterScreen} already holds that one — but a
 * screen of its own, which {@code CreativeClientSystem} opens on the inventory key when the character is in
 * creative mode.
 * <p>
 * The backpack is deliberately absent: the catalogue is the backpack now. Its thirty-six slots still exist and
 * can still be filled, which is why the bin empties them along with everything else.
 */
public class CreativeScreen extends CoreScreenLayer {
    private static final Logger logger = LoggerFactory.getLogger(CreativeScreen.class);

    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;
    @In
    private Catalog catalog;

    private CatalogGrid grid;
    private ResettableUIText search;
    private UILabel title;
    private UILabel count;

    private CatalogTab current = CatalogTab.CONSTRUCTION;

    @Override
    public void initialise() {
        grid = find("catalogue", CatalogGrid.class);
        grid.setPicker(this::pick);

        title = find("tabTitle", UILabel.class);
        count = find("tabCount", UILabel.class);

        for (CatalogTab tab : CatalogTab.values()) {
            UIButton button = find(tab.getButtonId(), UIButton.class);
            if (button != null) {
                button.setText(tab.getLabel());
                button.subscribe(widget -> select(tab));
            }
        }

        search = find("search", ResettableUIText.class);
        if (search != null) {
            // Filters the tab in view, so that on "Tout" it searches the whole game and elsewhere just the shelf.
            search.subscribe((TextChangeEventListener) (oldText, newText) -> showEntries());
        }

        TrashCell trash = find("trash", TrashCell.class);
        if (trash != null) {
            trash.setAction(this::emptyOrDestroy);
        }

        ReadOnlyBinding<EntityRef> character = new ReadOnlyBinding<EntityRef>() {
            @Override
            public EntityRef get() {
                return localPlayer.getCharacterEntity();
            }
        };
        for (SlotCell cell : findAll(SlotCell.class)) {
            cell.bindTargetInventory(character);
        }

        select(current);
    }

    @Override
    public void onOpened() {
        super.onOpened();
        select(current);
    }

    @Override
    public boolean isModal() {
        return false;
    }

    private void select(CatalogTab tab) {
        current = tab;
        for (CatalogTab other : CatalogTab.values()) {
            UIButton button = find(other.getButtonId(), UIButton.class);
            if (button != null) {
                button.setFamily(other == tab ? "tab-active" : "tab-bar");
            }
        }
        title.setText(tab.getTitle());
        // A query left over from another shelf reads as an empty tab: "Aucun résultat" where the tab is meant
        // to say what the game does not have yet.
        if (search != null) {
            search.setText("");
        }
        showEntries();
    }

    /** The one place the list is rebuilt: on a tab change and on every keystroke, never on a frame. */
    private void showEntries() {
        List<CatalogEntry> entries = new ArrayList<>(catalog.entriesFor(current));
        String query = search == null ? "" : CatalogEntry.fold(search.getText().trim());
        if (!query.isEmpty()) {
            entries.removeIf(entry -> !entry.matches(query));
        }
        grid.setEntries(entries);
        count.setText(label(entries.size(), query.isEmpty()));
    }

    private static String label(int size, boolean unfiltered) {
        if (size == 0) {
            return unfiltered ? "Rien de tel n'existe encore dans le jeu" : "Aucun résultat";
        }
        return size + (size > 1 ? " entrées" : " entrée");
    }

    private void pick(CatalogEntry entry, boolean wholeStack) {
        localPlayer.getCharacterEntity()
                .send(new GiveCatalogEntryRequest(entry.getUri(), entry.isBlock(), wholeStack));
    }

    /** Holding something? the bin eats it. Holding nothing? it empties the lot. */
    private void emptyOrDestroy() {
        EntityRef character = localPlayer.getCharacterEntity();
        CharacterComponent characterComponent = character.getComponent(CharacterComponent.class);
        boolean holding = characterComponent != null
                && InventoryUtils.getItemAt(characterComponent.movingItem, 0).exists();
        character.send(new ClearInventoryRequest(!holding));
    }

    /**
     * An item still on the cursor goes back where it can, or falls at the player's feet — the behaviour of
     * every other inventory screen, and the only thing that keeps a half-finished drag from vanishing.
     */
    @Override
    public void onClosed() {
        super.onClosed();
        EntityRef player = localPlayer.getCharacterEntity();
        CharacterComponent characterComponent = player.getComponent(CharacterComponent.class);
        InventoryComponent playerInventory = player.getComponent(InventoryComponent.class);
        if (characterComponent == null || playerInventory == null) {
            logger.error("Character entity of player had no character or inventory component");
            return;
        }
        EntityRef movingItem = characterComponent.movingItem;
        List<Integer> toSlots = IntStream.range(0, playerInventory.itemSlots.size()).boxed().collect(Collectors.toList());
        inventoryManager.moveItemToSlots(player, movingItem, 0, player, toSlots);

        EntityRef item = InventoryUtils.getItemAt(movingItem, 0);
        InventoryUtils.dropItems(item, InventoryUtils.getStackCount(item), localPlayer);
    }
}
