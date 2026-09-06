package com.customitem.engine;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Loads/saves recipes.yml and dynamically injects shaped recipes into Bukkit.
 */
public final class RecipeManager {

    private final CustomItemEngine plugin;
    private final File file;
    private YamlConfiguration config;
    private final Map<String, NamespacedKey> activeKeys = new HashMap<>();
    private final Set<String> activeItems = new HashSet<>();
    private final Map<String, String[][]> recipeMatrix = new LinkedHashMap<>();

    public RecipeManager(CustomItemEngine plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "recipes.yml");
    }

    public void loadAndInject() {
        DefaultConfigs.ensureRecipesConfig(plugin);
        config = YamlConfiguration.loadConfiguration(file);
        recipeMatrix.clear();
        // Remove previously injected recipes.
        for (NamespacedKey key : activeKeys.values()) {
            plugin.getServer().removeRecipe(key);
        }
        activeKeys.clear();
        activeItems.clear();

        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) return;
        for (String itemId : recipes.getKeys(false)) {
            ConfigurationSection r = recipes.getConfigurationSection(itemId);
            if (r == null) continue;
            String[][] matrix = readMatrix(r);
            if (matrix == null) continue;
            recipeMatrix.put(itemId, matrix);
            injectRecipe(itemId, matrix);
        }
    }

    public int activeCount() { return activeItems.size(); }

    public boolean isActive(String itemId) { return activeItems.contains(itemId); }

    public String[][] getMatrix(String itemId) { return recipeMatrix.get(itemId); }

    public boolean hasRecipe(String itemId) { return recipeMatrix.containsKey(itemId); }

    /** Writes a recipe to recipes.yml and injects it at runtime. */
    public void saveRecipe(String itemId, ItemStack[] grid) {
        // grid is a 9-element array (row-major) from the 3x3 GUI.
        String[][] matrix = new String[3][3];
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            ItemStack slot = grid[i];
            matrix[row][col] = encodeIngredient(slot);
        }

        // Save to config.
        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) {
            config.createSection("recipes");
            recipes = config.getConfigurationSection("recipes");
        }
        String base = "recipes." + itemId;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                config.set(base + ".row" + row + "." + col, matrix[row][col]);
            }
        }
        try {
            DefaultConfigs.saveYaml(config, file, plugin);
        } catch (Throwable e) {
            plugin.getLogger().warning("Could not save recipes.yml: " + e.getMessage());
        }

        recipeMatrix.put(itemId, matrix);
        injectRecipe(itemId, matrix);
    }

    private String encodeIngredient(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return "AIR";
        String engineId = plugin.getItemRegistry().idOf(stack);
        if (engineId != null) return "engine:" + engineId;
        return stack.getType().name();
    }

    private String[][] readMatrix(ConfigurationSection r) {
        String[][] matrix = new String[3][3];
        for (int row = 0; row < 3; row++) {
            ConfigurationSection rowSec = r.getConfigurationSection("row" + row);
            for (int col = 0; col < 3; col++) {
                String val;
                if (rowSec != null) {
                    val = rowSec.getString(String.valueOf(col), "AIR");
                } else {
                    val = "AIR";
                }
                matrix[row][col] = val;
            }
        }
        return matrix;
    }

    /** Injects (or refreshes) a shaped recipe into Bukkit at runtime. */
    public void injectRecipe(String itemId, String[][] matrix) {
        ItemStack result = plugin.getItemRegistry().build(itemId);
        if (result == null) return;

        // Remove old key if present.
        if (activeKeys.containsKey(itemId)) {
            plugin.getServer().removeRecipe(activeKeys.get(itemId));
        }

        NamespacedKey key = new NamespacedKey(plugin, "recipe_" + itemId);
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        // Build shape lines using characters A-I mapped to grid slots 0-8.
        // Empty slots become spaces. Empty rows become "   ".
        char[] chars = {'A','B','C','D','E','F','G','H','I'};
        StringBuilder[] rows = {new StringBuilder(), new StringBuilder(), new StringBuilder()};
        Map<Character, ItemStack> ingredients = new HashMap<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                String ing = matrix[row][col];
                char c = chars[row * 3 + col];
                if (ing == null || ing.equals("AIR")) {
                    rows[row].append(' ');
                } else {
                    rows[row].append(c);
                    ItemStack ingredient = resolveIngredient(ing);
                    if (ingredient != null) {
                        ingredients.put(c, ingredient);
                    }
                }
            }
        }
        recipe.shape(rows[0].toString(), rows[1].toString(), rows[2].toString());
        for (Map.Entry<Character, ItemStack> e : ingredients.entrySet()) {
            // Use RecipeChoice.ExactChoice so custom NBT items match exactly
            // and vanilla items match on data. This prevents plain items from
            // substituting for custom ingredients.
            recipe.setIngredient(e.getKey(),
                    new RecipeChoice.ExactChoice(e.getValue()));
        }

        try {
            plugin.getServer().addRecipe(recipe);
            activeKeys.put(itemId, key);
            activeItems.add(itemId);
        } catch (IllegalStateException dup) {
            // Already registered; refresh by removing then adding.
            plugin.getServer().removeRecipe(key);
            try {
                plugin.getServer().addRecipe(recipe);
                activeKeys.put(itemId, key);
                activeItems.add(itemId);
            } catch (Throwable ignored) {}
        }
    }

    /** Resolves an ingredient string ("engine:id" or "MATERIAL") to an ItemStack. */
    public ItemStack resolveIngredient(String ing) {
        if (ing == null || ing.equals("AIR")) return null;
        if (ing.startsWith("engine:")) {
            String id = ing.substring("engine:".length());
            return plugin.getItemRegistry().build(id);
        }
        try {
            Material mat = Material.valueOf(ing.toUpperCase());
            return new ItemStack(mat);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Returns the ingredient key at a matrix position, or "AIR". */
    public String ingredientAt(String itemId, int row, int col) {
        String[][] m = recipeMatrix.get(itemId);
        if (m == null) return "AIR";
        return m[row][col];
    }
}
