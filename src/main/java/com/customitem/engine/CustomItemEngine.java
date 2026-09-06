package com.customitem.engine;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Entry point for CustomItemEngine.
 * Handles lifecycle, disk-based file-structure generation, and subsystem wiring.
 */
public final class CustomItemEngine extends JavaPlugin {

    private static CustomItemEngine instance;

    private ItemRegistry itemRegistry;
    private RecipeManager recipeManager;
    private CategoryConfigLoader categoryLoader;
    private EngineCommand commandHandler;
    private EngineGUI guiManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Build the on-disk file structure and write default configs.
        DefaultConfigs.generateFileStructure(this);

        // 2. Load configs and registry.
        saveDefaultConfig();
        this.categoryLoader = new CategoryConfigLoader(this);
        this.categoryLoader.loadAll();

        this.itemRegistry = new ItemRegistry(this);
        this.itemRegistry.registerAll();

        this.recipeManager = new RecipeManager(this);
        this.recipeManager.loadAndInject();

        // 3. GUI + command wiring.
        this.guiManager = new EngineGUI(this);
        this.commandHandler = new EngineCommand(this);
        getCommand("engine").setExecutor(commandHandler);
        getCommand("engine").setTabCompleter(commandHandler);

        // 4. Listeners.
        getServer().getPluginManager().registerEvents(new PrepareCraftListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorListener(this), this);
        getServer().getPluginManager().registerEvents(new FoodListener(this), this);
        getServer().getPluginManager().registerEvents(new OreBreakListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        // 5. Ore generation.
        if (getConfig().getBoolean("ore-generation.enabled", true)) {
            getServer().getWorlds().forEach(w -> w.getPopulators().add(new OrePopulator(this)));
        }

        getLogger().info("CustomItemEngine v4.0 enabled. Registered "
                + itemRegistry.size() + " items, "
                + recipeManager.activeCount() + " active recipes.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CustomItemEngine disabled.");
    }

    /** Reloads all configs and refreshes recipes without a server reboot. */
    public void reload() {
        // Re-verify the on-disk structure and fill in any missing defaults.
        DefaultConfigs.generateFileStructure(this);
        reloadConfig();
        categoryLoader.loadAll();
        itemRegistry.registerAll();
        recipeManager.loadAndInject();
        getLogger().info("Reloaded. Items: " + itemRegistry.size()
                + ", Active recipes: " + recipeManager.activeCount());
    }

    public static CustomItemEngine getInstance() { return instance; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public CategoryConfigLoader getCategoryLoader() { return categoryLoader; }
    public EngineGUI getGuiManager() { return guiManager; }
}
