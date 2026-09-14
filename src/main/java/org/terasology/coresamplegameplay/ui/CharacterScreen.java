// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.coresamplegameplay.crafting.RecipeBook;
import org.terasology.coresamplegameplay.crafting.RecipeView;
import org.terasology.coresamplegameplay.equipment.CharacterStats;
import org.terasology.coresamplegameplay.equipment.EquipmentSlots;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.logic.common.DisplayNameComponent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.players.LocalPlayer;
import org.terasology.engine.registry.In;
import org.terasology.engine.rendering.assets.texture.TextureRegion;
import org.terasology.engine.rendering.nui.CoreScreenLayer;
import org.terasology.engine.utilities.Assets;
import org.terasology.module.inventory.components.InventoryComponent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.module.inventory.systems.InventoryUtils;
import org.terasology.module.inventory.ui.ItemIcon;
import org.terasology.nui.databinding.ReadOnlyBinding;
import org.terasology.nui.layouts.ColumnLayout;
import org.terasology.nui.layouts.RowLayout;
import org.terasology.nui.layouts.RowLayoutHint;
import org.terasology.nui.widgets.UIButton;
import org.terasology.nui.widgets.UILabel;
import org.terasology.nui.widgets.UIList;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The in-game screen of the 2g artboard: the character and what it wears, the backpack, and crafting, side by
 * side. It replaces {@code Inventory:inventoryScreen} through an override, so the inventory key opens it and a
 * station opens it too.
 */
public class CharacterScreen extends CoreScreenLayer {
    private static final Logger logger = LoggerFactory.getLogger(CharacterScreen.class);

    /** Inventory changes arrive from the server; a quarter second is below what a click can notice. */
    private static final float REFRESH_SECONDS = 0.25f;

    @In
    private LocalPlayer localPlayer;
    @In
    private InventoryManager inventoryManager;
    @In
    private RecipeBook recipeBook;

    private UIList<RecipeView> recipes;
    private UILabel recipeName;
    private UILabel recipeDescription;
    private UILabel missing;
    private ColumnLayout ingredients;
    private UIButton craftButton;

    private String selectedId;
    private String shownSignature = "";
    private float sinceRefresh;

    @Override
    @SuppressWarnings("unchecked")
    public void initialise() {
        ColumnLayout grid = find("grid", ColumnLayout.class);
        for (int i = 0; i < EquipmentSlots.GRID; i++) {
            SlotCell cell = new SlotCell();
            cell.setTargetSlot(EquipmentSlots.TOOLBAR + i);
            grid.addWidget(cell);
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

        bindText("attack", () -> String.valueOf(CharacterStats.attack(character())));
        bindText("defense", () -> String.valueOf(CharacterStats.defense(character())));
        bindText("speed", () -> String.format(Locale.FRANCE, "%.1f", CharacterStats.attackSpeed(character())));
        bindText("heroName", this::heroName);

        recipes = find("recipes", UIList.class);
        recipes.setItemRenderer(new RecipeRenderer());
        recipes.subscribeSelection((widget, recipe) -> {
            if (recipe != null) {
                selectedId = recipe.getId();
                showDetail(recipe);
            }
        });
        recipeName = find("recipeName", UILabel.class);
        recipeDescription = find("recipeDescription", UILabel.class);
        missing = find("missing", UILabel.class);
        ingredients = find("ingredients", ColumnLayout.class);
        ingredients.setFillVerticalSpace(false);
        craftButton = find("craft", UIButton.class);
        craftButton.subscribe(widget -> craftSelected());
    }

    @Override
    public void onOpened() {
        super.onOpened();
        shownSignature = "";
        refresh();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        sinceRefresh += delta;
        if (sinceRefresh >= REFRESH_SECONDS) {
            sinceRefresh = 0;
            refresh();
        }
    }

    @Override
    public boolean isModal() {
        return false;
    }

    private EntityRef character() {
        return localPlayer.getCharacterEntity();
    }

    private void bindText(String id, Supplier<String> text) {
        find(id, UILabel.class).bindText(new ReadOnlyBinding<String>() {
            @Override
            public String get() {
                return text.get();
            }
        });
    }

    private String heroName() {
        DisplayNameComponent name = localPlayer.getClientInfoEntity().getComponent(DisplayNameComponent.class);
        return name != null && !name.name.isEmpty() ? name.name : "Personnage";
    }

    /** Rebuilds the list only when something it shows has changed, so a selection is not lost every tick. */
    private void refresh() {
        List<RecipeView> views = recipeBook.recipesFor(character());
        String signature = views.stream().map(RecipeView::getSignature).collect(Collectors.joining("|"));
        if (signature.equals(shownSignature)) {
            return;
        }
        shownSignature = signature;
        recipes.setList(views);
        RecipeView selected = views.stream()
                .filter(view -> view.getId().equals(selectedId))
                .findFirst()
                .orElse(views.isEmpty() ? null : views.get(0));
        selectedId = selected == null ? null : selected.getId();
        recipes.setSelection(selected);
        showDetail(selected);
    }

    private void showDetail(RecipeView recipe) {
        ingredients.removeAllWidgets();
        if (recipe == null) {
            recipeName.setText("");
            recipeDescription.setText("");
            missing.setText("");
            craftButton.setEnabled(false);
            return;
        }
        recipeName.setText(recipe.getName());
        recipeDescription.setText(recipe.getDescription());
        for (RecipeView.Ingredient ingredient : recipe.getIngredients()) {
            // Fixed shares rather than content widths: a content-sized count wraps "14 / 3" onto two lines.
            RowLayout row = new RowLayout();
            row.setHorizontalSpacing(8);
            row.addWidget(icon(ingredient), new RowLayoutHint());
            UILabel name = new UILabel(ingredient.getName());
            name.setFamily("ingredient");
            row.addWidget(name, new RowLayoutHint());
            UILabel count = new UILabel(ingredient.getOwned() + " / " + ingredient.getNeeded());
            count.setFamily(ingredient.isSatisfied() ? "count-ok" : "count-missing");
            row.addWidget(count, new RowLayoutHint());
            // after the widgets: the ratios are written into their hints, one per widget already added
            row.setColumnRatios(0.13f, 0.57f, 0.30f);
            ingredients.addWidget(row);
        }
        missing.setText(recipe.getMissingText());
        craftButton.setEnabled(recipe.isCraftable());
    }

    private static ItemIcon icon(RecipeView.Ingredient ingredient) {
        ItemIcon icon = new ItemIcon();
        icon.setFamily("ingredient");
        if (ingredient.getItemPrefab() != null) {
            ItemComponent item = ingredient.getItemPrefab().getComponent(ItemComponent.class);
            if (item != null && item.icon != null) {
                icon.setIcon((TextureRegion) item.icon);
            }
        } else if (ingredient.getBlockFamily() != null) {
            icon.setMesh(ingredient.getBlockFamily().getArchetypeBlock().getMeshGenerator().getStandaloneMesh());
            Assets.getTexture("engine:terrain").ifPresent(icon::setMeshTexture);
        }
        return icon;
    }

    private void craftSelected() {
        if (selectedId != null && recipeBook.craft(character(), selectedId)) {
            sinceRefresh = REFRESH_SECONDS;
        }
    }

    /**
     * An item still held on the cursor goes back into the inventory, or drops at the player's feet — the
     * behaviour of {@code InventoryScreen}, which this screen replaces.
     */
    @Override
    public void onClosed() {
        super.onClosed();
        EntityRef player = character();
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
