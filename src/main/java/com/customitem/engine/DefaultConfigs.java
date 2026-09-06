package com.customitem.engine;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Writes default configuration files directly to the server disk at runtime.
 * No category configs are bundled inside the jar — they are generated here
 * so operators can inspect and edit them on first launch.
 */
public final class DefaultConfigs {

    private DefaultConfigs() {}

    public static final String[] CATEGORIES = {"ores", "armours", "weapons", "foods"};

    // --- Default content (all strings double-quoted for SnakeYAML safety) ---

    public static final String MAIN_CONFIG = ""
            + "# CustomItemEngine main configuration\n"
            + "# All string values are quoted for SnakeYAML safety.\n"
            + "\n"
            + "settings:\n"
            + "  locale: \"en\"\n"
            + "  auto-generate-files: true\n"
            + "\n"
            + "ore-generation:\n"
            + "  enabled: true\n"
            + "  min-y: -64\n"
            + "  max-y: -45\n"
            + "  vein-min-y: -50\n"
            + "  vein-max-y: -55\n"
            + "  vein-chance: 8\n"
            + "  vein-min-size: 3\n"
            + "  vein-max-size: 5\n"
            + "  attempts-per-chunk: 6\n"
            + "\n"
            + "gui:\n"
            + "  title: \"CustomItemEngine\"\n"
            + "  category-rows: 3\n"
            + "\n"
            + "debug: false\n";

    public static final String RECIPES_CONFIG = ""
            + "# CustomItemEngine recipe definitions\n"
            + "# Recipes are written automatically by the in-game Recipe Builder GUI.\n"
            + "# You may also edit them manually; run /engine reload afterwards.\n"
            + "\n"
            + "recipes: {}\n";

    public static final String ORES_CONFIG = ""
            + "# Ores category configuration\n"
            + "# Items declared here start in \"draft\" state until a recipe is assigned.\n"
            + "\n"
            + "category:\n"
            + "  display-name: \"Ores\"\n"
            + "  icon: \"DIAMOND_ORE\"\n"
            + "\n"
            + "set_bonus:\n"
            + "  enabled: false\n"
            + "\n"
            + "items:\n"
            + "  ruby:\n"
            + "    display-name: \"&cRuby\"\n"
            + "    material: \"AMETHYST_SHARD\"\n"
            + "    custom-model-data: 7001\n"
            + "    ore:\n"
            + "      enabled: true\n"
            + "      block: \"DEEPSLATE\"\n"
            + "      min-y: -64\n"
            + "      max-y: -45\n"
            + "      drop-min: 1\n"
            + "      drop-max: 2\n"
            + "      silk-touch-drop: true\n"
            + "      fortune-multiplier: true\n"
            + "    harvest-tool: \"DIAMOND_PICKAXE\"\n"
            + "  sapphire:\n"
            + "    display-name: \"&9Sapphire\"\n"
            + "    material: \"AMETHYST_SHARD\"\n"
            + "    custom-model-data: 7002\n"
            + "    ore:\n"
            + "      enabled: true\n"
            + "      block: \"DEEPSLATE\"\n"
            + "      min-y: -64\n"
            + "      max-y: -45\n"
            + "      drop-min: 1\n"
            + "      drop-max: 1\n"
            + "      silk-touch-drop: true\n"
            + "      fortune-multiplier: true\n"
            + "    harvest-tool: \"IRON_PICKAXE\"\n";

    public static final String ARMOURS_CONFIG = ""
            + "# Armours category configuration\n"
            + "\n"
            + "category:\n"
            + "  display-name: \"Armours\"\n"
            + "  icon: \"DIAMOND_CHESTPLATE\"\n"
            + "\n"
            + "# Set bonus applied when all 4 matching pieces of a set are equipped.\n"
            + "# type: \"flat-hearts\" adds flat hearts; \"percent-health\" multiplies max health.\n"
            + "set_bonus:\n"
            + "  enabled: true\n"
            + "  type: \"flat-hearts\"\n"
            + "  value: 4.0\n"
            + "\n"
            + "items:\n"
            + "  ruby_helmet:\n"
            + "    display-name: \"&cRuby Helmet\"\n"
            + "    material: \"DIAMOND_HELMET\"\n"
            + "    custom-model-data: 7101\n"
            + "    armor:\n"
            + "      slot: \"HELMET\"\n"
            + "      set: \"ruby\"\n"
            + "      defense: 3\n"
            + "      toughness: 2\n"
            + "      durability: 550\n"
            + "  ruby_chestplate:\n"
            + "    display-name: \"&cRuby Chestplate\"\n"
            + "    material: \"DIAMOND_CHESTPLATE\"\n"
            + "    custom-model-data: 7102\n"
            + "    armor:\n"
            + "      slot: \"CHESTPLATE\"\n"
            + "      set: \"ruby\"\n"
            + "      defense: 8\n"
            + "      toughness: 2\n"
            + "      durability: 800\n"
            + "  ruby_leggings:\n"
            + "    display-name: \"&cRuby Leggings\"\n"
            + "    material: \"DIAMOND_LEGGINGS\"\n"
            + "    custom-model-data: 7103\n"
            + "    armor:\n"
            + "      slot: \"LEGGINGS\"\n"
            + "      set: \"ruby\"\n"
            + "      defense: 6\n"
            + "      toughness: 2\n"
            + "      durability: 750\n"
            + "  ruby_boots:\n"
            + "    display-name: \"&cRuby Boots\"\n"
            + "    material: \"DIAMOND_BOOTS\"\n"
            + "    custom-model-data: 7104\n"
            + "    armor:\n"
            + "      slot: \"BOOTS\"\n"
            + "      set: \"ruby\"\n"
            + "      defense: 3\n"
            + "      toughness: 2\n"
            + "      durability: 450\n";

    public static final String WEAPONS_CONFIG = ""
            + "# Weapons category configuration\n"
            + "\n"
            + "category:\n"
            + "  display-name: \"Weapons\"\n"
            + "  icon: \"DIAMOND_SWORD\"\n"
            + "\n"
            + "set_bonus:\n"
            + "  enabled: false\n"
            + "\n"
            + "items:\n"
            + "  ruby_sword:\n"
            + "    display-name: \"&cRuby Sword\"\n"
            + "    material: \"DIAMOND_SWORD\"\n"
            + "    custom-model-data: 7201\n"
            + "    weapon:\n"
            + "      attack-damage: 8.0\n"
            + "      attack-speed: 1.6\n"
            + "      durability: 1200\n"
            + "  ruby_pickaxe:\n"
            + "    display-name: \"&cRuby Pickaxe\"\n"
            + "    material: \"DIAMOND_PICKAXE\"\n"
            + "    custom-model-data: 7202\n"
            + "    tool:\n"
            + "      mining-speed: 9.0\n"
            + "      durability: 1500\n";

    public static final String FOODS_CONFIG = ""
            + "# Foods category configuration\n"
            + "\n"
            + "category:\n"
            + "  display-name: \"Foods\"\n"
            + "  icon: \"GOLDEN_APPLE\"\n"
            + "\n"
            + "set_bonus:\n"
            + "  enabled: false\n"
            + "\n"
            + "items:\n"
            + "  ruby_berry:\n"
            + "    display-name: \"&cRuby Berry\"\n"
            + "    material: \"APPLE\"\n"
            + "    custom-model-data: 7301\n"
            + "    food:\n"
            + "      nutrition: 6\n"
            + "      saturation: 4.0\n"
            + "      effects:\n"
            + "        - \"REGENERATION:5:1\"\n"
            + "        - \"SPEED:10:1\"\n";

    /** Returns the default content for a category name. */
    public static String categoryContent(String category) {
        switch (category) {
            case "ores": return ORES_CONFIG;
            case "armours": return ARMOURS_CONFIG;
            case "weapons": return WEAPONS_CONFIG;
            case "foods": return FOODS_CONFIG;
            default: return "";
        }
    }

    // --- Disk writers ---

    /** Ensures a directory exists, logging a warning on failure. */
    public static boolean ensureDir(File dir, JavaPlugin plugin) {
        if (dir.exists()) return true;
        if (dir.mkdirs()) return true;
        plugin.getLogger().warning("Could not create directory: " + dir);
        return false;
    }

    /** Writes text content to a file only if it does not already exist. */
    public static boolean writeIfMissing(File file, String content, JavaPlugin plugin) {
        if (file.exists()) return true;
        File parent = file.getParentFile();
        if (parent != null) ensureDir(parent, plugin);
        try {
            Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not write " + file + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates the full on-disk plugin folder tree and default configs.
     * Categories are written programmatically — never copied from the jar.
     */
    public static void generateFileStructure(JavaPlugin plugin) {
        File base = plugin.getDataFolder();
        ensureDir(base, plugin);

        // categories/ + each subfolder
        File categories = new File(base, "categories");
        ensureDir(categories, plugin);
        for (String cat : CATEGORIES) {
            ensureDir(new File(categories, cat), plugin);
        }

        // assets/block and assets/item
        ensureDir(new File(base, "assets/block"), plugin);
        ensureDir(new File(base, "assets/item"), plugin);

        // Top-level configs (only if missing — never overwrite user edits)
        writeIfMissing(new File(base, "config.yml"), MAIN_CONFIG, plugin);
        writeIfMissing(new File(base, "recipes.yml"), RECIPES_CONFIG, plugin);

        // Category configs
        for (String cat : CATEGORIES) {
            writeIfMissing(new File(categories, cat + "/config.yml"),
                    categoryContent(cat), plugin);
        }
    }

    /** Ensures a single category config exists on disk, creating it if needed. */
    public static void ensureCategoryConfig(JavaPlugin plugin, String category) {
        File file = new File(plugin.getDataFolder(), "categories/" + category + "/config.yml");
        writeIfMissing(file, categoryContent(category), plugin);
    }

    /** Ensures recipes.yml exists on disk, creating it if needed. */
    public static void ensureRecipesConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "recipes.yml");
        writeIfMissing(file, RECIPES_CONFIG, plugin);
    }

    /** Saves a YamlConfiguration to disk, logging on failure. */
    public static boolean saveYaml(YamlConfiguration cfg, File file, JavaPlugin plugin) {
        try {
            cfg.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + file + ": " + e.getMessage());
            return false;
        }
    }
}
