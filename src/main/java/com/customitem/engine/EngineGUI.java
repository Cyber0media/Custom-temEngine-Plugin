package com.customitem.engine;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Category selection GUI and per-category item browser.
 * Right-clicking an item in a category opens the Recipe Builder for that item.
 */
public final class EngineGUI implements Listener {

    public static final String MAIN_TITLE = ChatColor.DARK_GRAY + "CustomItemEngine";
    public static final String CATEGORY_PREFIX = ChatColor.DARK_GRAY + "Category: ";
    public static final String ITEMS_PREFIX = ChatColor.DARK_GRAY + "Items: ";

    private final CustomItemEngine plugin;
    private final RecipeBuilderGUI recipeBuilder;

    // Track which category a player is viewing.
    private final Map<UUID, String> viewingCategory = new HashMap<>();

    public EngineGUI(CustomItemEngine plugin) {
        this.plugin = plugin;
        this.recipeBuilder = new RecipeBuilderGUI(plugin);
        plugin.getServer().getPluginManager().registerEvents(recipeBuilder, plugin);
    }

    public RecipeBuilderGUI getRecipeBuilder() { return recipeBuilder; }

    public void openMain(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 27,
                MAIN_TITLE);
        int slot = 10;
        for (String cat : CategoryConfigLoader.CATEGORIES) {
            YamlConfiguration cfg = plugin.getCategoryLoader().get(cat);
            if (cfg == null) continue;
            ConfigurationSection catSec = cfg.getConfigurationSection("category");
            String name = catSec != null ? catSec.getString("display-name", cat) : cat;
            String icon = catSec != null ? catSec.getString("icon", "CHEST") : "CHEST";
            Material mat = parseMaterial(icon, Material.CHEST);
            ItemStack item = iconItem(mat, ChatColor.AQUA + name,
                    ChatColor.GRAY + "Click to browse " + cat);
            inv.setItem(slot, item);
            slot++;
            if (slot == 13) slot = 16; // skip middle row
        }
        player.openInventory(inv);
    }

    public void openCategory(Player player, String category) {
        YamlConfiguration cfg = plugin.getCategoryLoader().get(category);
        if (cfg == null) return;
        ConfigurationSection itemsSec = cfg.getConfigurationSection("items");
        if (itemsSec == null) return;

        Inventory inv = plugin.getServer().createInventory(null, 54,
                ITEMS_PREFIX + category);
        int slot = 0;
        for (String id : itemsSec.getKeys(false)) {
            ItemStack built = plugin.getItemRegistry().build(id);
            if (built == null) continue;
            // Tag the lore with status.
            ItemMeta meta = built.getItemMeta();
            if (meta != null) {
                java.util.List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
                if (lore == null) lore = new java.util.ArrayList<>();
                boolean active = plugin.getRecipeManager().isActive(id);
                lore.add((active ? ChatColor.GREEN : ChatColor.YELLOW)
                        + (active ? "Active" : "Draft"));
                meta.setLore(lore);
                built.setItemMeta(meta);
            }
            inv.setItem(slot, built);
            slot++;
            if (slot >= 45) break;
        }
        // Back button.
        inv.setItem(49, iconItem(Material.ARROW, ChatColor.AQUA + "Back", null));
        viewingCategory.put(player.getUniqueId(), category);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(MAIN_TITLE) && !title.startsWith(ITEMS_PREFIX)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot >= event.getInventory().getSize()) return;

        if (title.equals(MAIN_TITLE)) {
            handleMainClick(player, slot);
        } else if (title.startsWith(ITEMS_PREFIX)) {
            handleItemsClick(player, slot, event.getClick());
        }
    }

    private void handleMainClick(Player player, int slot) {
        int idx = 0;
        for (String cat : CategoryConfigLoader.CATEGORIES) {
            int expected = 10 + idx + (idx >= 3 ? 3 : 0);
            if (slot == expected) {
                openCategory(player, cat);
                return;
            }
            idx++;
        }
    }

    private void handleItemsClick(Player player, int slot, ClickType click) {
        if (slot == 49) {
            openMain(player);
            return;
        }
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack clicked = inv.getItem(slot);
        if (clicked == null) return;
        String id = plugin.getItemRegistry().idOf(clicked);
        if (id == null) return;

        if (click == ClickType.RIGHT) {
            // Open recipe builder.
            recipeBuilder.open(player, id);
        } else if (click == ClickType.LEFT) {
            // Give one to the player.
            ItemStack built = plugin.getItemRegistry().build(id);
            if (built != null) {
                player.getInventory().addItem(built).forEach((i, left) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
    }

    private ItemStack iconItem(Material mat, String name, String lore) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null) m.setLore(java.util.List.of(lore));
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            s.setItemMeta(m);
        }
        return s;
    }

    private Material parseMaterial(String name, Material fallback) {
        try { return Material.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return fallback; }
    }
}
