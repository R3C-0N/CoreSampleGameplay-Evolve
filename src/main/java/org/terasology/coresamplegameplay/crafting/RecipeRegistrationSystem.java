// Copyright 2026 R3C-0N
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coresamplegameplay.crafting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.coresamplegameplay.equipment.ArmorComponent;
import org.terasology.coresamplegameplay.equipment.CharacterStats;
import org.terasology.coresamplegameplay.equipment.EquipmentSlots;
import org.terasology.coresamplegameplay.equipment.ToolComponent;
import org.terasology.coresamplegameplay.equipment.WeaponComponent;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.prefab.Prefab;
import org.terasology.engine.entitySystem.prefab.PrefabManager;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.common.DisplayNameComponent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.registry.Share;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.gestalt.assets.ResourceUrn;
import org.terasology.module.inventory.systems.InventoryUtils;
import org.terasology.workstationCrafting.event.UserCraftInHandRequest;
import org.terasology.workstationCrafting.system.CraftInHandRecipeRegistry;
import org.terasology.workstationCrafting.system.recipe.behaviour.ConsumeItemCraftBehaviour;
import org.terasology.workstationCrafting.system.recipe.hand.CompositeTypeBasedCraftInHandRecipe;
import org.terasology.workstationCrafting.system.recipe.hand.CraftInHandRecipe;
import org.terasology.workstationCrafting.system.recipe.hand.PlayerInventorySlotResolver;
import org.terasology.workstationCrafting.system.recipe.render.RecipeResultFactory;
import org.terasology.workstationCrafting.system.recipe.render.result.BlockRecipeResultFactory;
import org.terasology.workstationCrafting.system.recipe.render.result.ItemRecipeResultFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns every {@link RecipeComponent} prefab into a recipe of the hand-crafting registry, and keeps what it
 * read so that the crafting panel can show it.
 * <p>
 * WorkstationCrafting reads no recipe prefab by itself — its content modules each register theirs in
 * code — so without this system a recipe file would be inert. A malformed recipe is logged and skipped,
 * never allowed to take the others down with it.
 */
@RegisterSystem
@Share(RecipeBook.class)
public class RecipeRegistrationSystem extends BaseComponentSystem implements RecipeBook {
    private static final Logger logger = LoggerFactory.getLogger(RecipeRegistrationSystem.class);

    /** Words that end a shared prefix without naming anything: "Tronc de chêne" and "Tronc de pin" share "Tronc". */
    private static final Set<String> CONNECTORS = Set.of("de", "du", "des", "d'", "en", "à");

    private static final Map<String, String> TOOL_FAMILIES = Map.of("pickaxe", "Pioche", "axe", "Hache",
            "shovel", "Pelle", "hoe", "Houe", "knife", "Couteau");
    private static final List<String> TOOL_GRADES = List.of("bois", "silex", "cuivre", "bronze", "fer", "acier",
            "métal fantastique", "étherium");

    /** Where a recipe is made, said in full: tying the label to the type keeps a second station honest. */
    private static final Map<String, String> STATION_LABELS = Map.of(
            "workbench", "à l'établi", "lapidary", "à la tour de lapidaire",
            "astralforge", "à la forge astrale", "ultimateforge", "à la forge astrale ultime");

    private static final Map<String, String> WEAPON_TYPES = Map.ofEntries(
            Map.entry("oneHandedSword", "Épée à une main"), Map.entry("shield", "Bouclier"),
            Map.entry("warHammer", "Marteau de combat"), Map.entry("club", "Gourdin"),
            Map.entry("windBlade", "Lame du vent"), Map.entry("bow", "Arc"),
            Map.entry("handCrossbow", "Arbalète à une main"), Map.entry("engineerKit", "Trousse d'ingénieur"),
            Map.entry("healingScepter", "Sceptre de soin"), Map.entry("mageStaff", "Bâton de mage"),
            Map.entry("elementalStaff", "Bâton élémentaire"),
            Map.entry("druidStaff", "Bâton druidique"), Map.entry("necromancerFocus", "Focus nécromantique"),
            Map.entry("battleAxe", "Hache de guerre"), Map.entry("dagger", "Dague"), Map.entry("spear", "Lance"),
            Map.entry("arrow", "Flèche"), Map.entry("bolt", "Carreau"));
    private static final Map<String, String> ARMOR_WEIGHTS = Map.of("light", "légère", "medium", "moyenne", "heavy", "lourde");
    private static final Map<String, String> ARMOR_SLOTS = Map.of("head", "tête", "chest", "torse", "legs", "jambes",
            "feet", "pieds", "cape", "dos", "amulet", "cou", "ring", "doigt");

    @In
    private CraftInHandRecipeRegistry recipeRegistry;
    @In
    private PrefabManager prefabManager;
    @In
    private BlockManager blockManager;
    @In
    private WorldProvider worldProvider;

    private final List<Definition> definitions = new ArrayList<>();

    @Override
    public void initialise() {
        for (Prefab prefab : prefabManager.listPrefabs(RecipeComponent.class)) {
            try {
                Definition definition = parse(prefab.getUrn().toString(), prefab.getComponent(RecipeComponent.class));
                recipeRegistry.addCraftInHandRecipe(definition.id, definition.recipe);
                definitions.add(definition);
            } catch (RuntimeException e) {
                logger.error("Recipe {} skipped: {}", prefab.getUrn(), e.getMessage());
            }
        }
        logger.info("Registered {} recipes", definitions.size());
    }

    @Override
    public List<RecipeView> recipesFor(EntityRef character) {
        List<RecipeView> views = new ArrayList<>();
        for (Definition definition : definitions) {
            if (definition.recipe instanceof StationRecipe && !((StationRecipe) definition.recipe).isAvailableTo(character)) {
                continue;
            }
            List<RecipeView.Ingredient> ingredients = new ArrayList<>();
            for (Ingredient ingredient : definition.ingredients) {
                ingredients.add(new RecipeView.Ingredient(ingredient.label, ingredient.count,
                        countCarried(character, ingredient.predicate), ingredient.iconPrefab, ingredient.iconBlock));
            }
            views.add(new RecipeView(definition.id, definition.name, definition.description, ingredients));
        }
        views.sort(Comparator.comparing(view -> !view.isCraftable()));
        return views;
    }

    @Override
    public boolean craft(EntityRef character, String recipeId) {
        CraftInHandRecipe recipe = recipeRegistry.getRecipes().get(recipeId);
        if (recipe == null) {
            return false;
        }
        List<CraftInHandRecipe.CraftInHandResult> results = recipe.getMatchingRecipeResults(character);
        if (results == null || results.isEmpty()) {
            return false;
        }
        character.send(new UserCraftInHandRequest(recipeId, results.get(0).getParameters(), 1));
        return true;
    }

    /** Backpack and toolbar only: what is worn is not an ingredient. */
    private static int countCarried(EntityRef character, UriIngredientPredicate predicate) {
        int total = 0;
        for (int slot = 0; slot < EquipmentSlots.FIRST; slot++) {
            EntityRef item = CharacterStats.itemAt(character, slot);
            if (item.exists() && predicate.apply(item)) {
                total += InventoryUtils.getStackCount(item);
            }
        }
        return total;
    }

    private Definition parse(String id, RecipeComponent component) {
        Definition definition = new Definition();
        definition.id = id;
        Prefab resultItem = prefabManager.getPrefab(component.result);
        BlockFamily resultBlock = resultItem == null ? blockManager.getBlockFamily(component.result) : null;
        RecipeResultFactory resultFactory;
        if (resultItem != null) {
            resultFactory = new ItemRecipeResultFactory(resultItem, component.count);
        } else if (resultBlock != null) {
            resultFactory = new BlockRecipeResultFactory(resultBlock.getArchetypeBlock(), component.count);
        } else {
            throw new IllegalArgumentException("result '" + component.result + "' is neither an item nor a block");
        }

        CompositeTypeBasedCraftInHandRecipe recipe = new CompositeTypeBasedCraftInHandRecipe(resultFactory);
        for (String text : component.ingredients) {
            Ingredient ingredient = parseIngredient(text);
            recipe.addItemCraftBehaviour(new ConsumeItemCraftBehaviour(ingredient.predicate, ingredient.count,
                    PlayerInventorySlotResolver.singleton()));
            definition.ingredients.add(ingredient);
        }
        definition.recipe = component.station == null ? recipe : new StationRecipe(recipe, component.station, worldProvider);

        String resultName = resultItem != null ? prefabName(resultItem, component.result) : resultBlock.getDisplayName();
        definition.name = component.count > 1 ? resultName + " ×" + component.count : resultName;
        definition.description = describe(resultItem, component.station);
        return definition;
    }

    private Ingredient parseIngredient(String text) {
        String[] countAndUris = text.split("\\*", 2);
        if (countAndUris.length != 2) {
            throw new IllegalArgumentException("ingredient '" + text + "' is not count*uri");
        }
        Ingredient ingredient = new Ingredient();
        ingredient.count = Integer.parseInt(countAndUris[0].trim());
        Set<ResourceUrn> accepted = new LinkedHashSet<>();
        List<String> names = new ArrayList<>();
        for (String uri : countAndUris[1].split("\\|")) {
            ResourceUrn urn = new ResourceUrn(uri.trim());
            accepted.add(urn);
            Prefab prefab = prefabManager.getPrefab(urn.toString());
            if (prefab != null) {
                names.add(prefabName(prefab, urn.getResourceName().toString()));
                if (ingredient.iconPrefab == null && ingredient.iconBlock == null) {
                    ingredient.iconPrefab = prefab;
                }
            } else {
                BlockFamily family = blockManager.getBlockFamily(urn.toString());
                names.add(family != null ? family.getDisplayName() : urn.getResourceName().toString());
                if (ingredient.iconPrefab == null && ingredient.iconBlock == null) {
                    ingredient.iconBlock = family;
                }
            }
        }
        ingredient.predicate = new UriIngredientPredicate(accepted);
        ingredient.label = sharedName(names);
        return ingredient;
    }

    private static String prefabName(Prefab prefab, String fallback) {
        DisplayNameComponent displayName = prefab.getComponent(DisplayNameComponent.class);
        return displayName != null && !displayName.name.isEmpty() ? displayName.name : fallback;
    }

    /** The words alternatives start with — "Tronc" for the three trunks — or the first name if they share none. */
    static String sharedName(List<String> names) {
        if (names.size() == 1) {
            return names.get(0);
        }
        String[] first = names.get(0).split(" ");
        int shared = first.length;
        for (String other : names) {
            String[] words = other.split(" ");
            int i = 0;
            while (i < shared && i < words.length && words[i].equals(first[i])) {
                i++;
            }
            shared = i;
        }
        while (shared > 0 && CONNECTORS.contains(first[shared - 1])) {
            shared--;
        }
        return shared == 0 ? names.get(0) : String.join(" ", Arrays.copyOf(first, shared));
    }

    private static String describe(Prefab resultItem, String station) {
        String kind;
        WeaponComponent weapon = resultItem == null ? null : resultItem.getComponent(WeaponComponent.class);
        ArmorComponent armor = resultItem == null ? null : resultItem.getComponent(ArmorComponent.class);
        ToolComponent tool = resultItem == null ? null : resultItem.getComponent(ToolComponent.class);
        ItemComponent item = resultItem == null ? null : resultItem.getComponent(ItemComponent.class);
        if (resultItem == null) {
            kind = "Bloc";
        } else if (tool != null) {
            kind = TOOL_FAMILIES.getOrDefault(tool.family, "Outil") + " · "
                    + TOOL_GRADES.get(Math.max(0, Math.min(tool.grade, TOOL_GRADES.size() - 1)));
        } else if (weapon != null) {
            kind = WEAPON_TYPES.getOrDefault(weapon.type, "Arme") + (weapon.hands == 2 ? " à deux mains" : "")
                    + " · " + (item != null ? item.baseDamage : 1) + " dégâts";
        } else if (armor != null) {
            kind = "Armure " + ARMOR_WEIGHTS.getOrDefault(armor.weight, "") + " · " + ARMOR_SLOTS.getOrDefault(armor.slot, "")
                    + " · défense " + armor.protection;
        } else {
            kind = "Matériau";
        }
        if (station == null) {
            return kind;
        }
        return kind + " · " + STATION_LABELS.getOrDefault(station, "en atelier");
    }

    private static final class Definition {
        private String id;
        private String name;
        private String description;
        private CraftInHandRecipe recipe;
        private final List<Ingredient> ingredients = new ArrayList<>();
    }

    private static final class Ingredient {
        private int count;
        private String label;
        private UriIngredientPredicate predicate;
        private Prefab iconPrefab;
        private BlockFamily iconBlock;
    }
}
