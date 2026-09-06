package com.customitem.engine;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads category configuration files from plugins/CustomItemEngine/categories/*.
 */
public final class CategoryConfigLoader {

    private final CustomItemEngine plugin;
    private final Map<String, YamlConfiguration> configs = new LinkedHashMap<>();

    public static final String[] CATEGORIES = {"ores", "armours", "weapons", "foods"};

    public CategoryConfigLoader(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        configs.clear();
        for (String cat : CATEGORIES) {
            DefaultConfigs.ensureCategoryConfig(plugin, cat);
            File file = new File(plugin.getDataFolder(), "categories/" + cat + "/config.yml");
            if (file.exists()) {
                configs.put(cat, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    public Map<String, YamlConfiguration> getAll() {
        return configs;
    }

    public YamlConfiguration get(String category) {
        return configs.get(category);
    }

    /** Returns the category name that contains a given item id, or null. */
    public String categoryOf(String itemId) {
        for (Map.Entry<String, YamlConfiguration> e : configs.entrySet()) {
            ConfigurationSection items = e.getValue().getConfigurationSection("items");
            if (items != null && items.contains(itemId)) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Returns the ConfigurationSection for an item id across all categories. */
    public ConfigurationSection itemSection(String itemId) {
        for (YamlConfiguration cfg : configs.values()) {
            ConfigurationSection items = cfg.getConfigurationSection("items");
            if (items != null) {
                ConfigurationSection sec = items.getConfigurationSection(itemId);
                if (sec != null) return sec;
            }
        }
        return null;
    }
}
