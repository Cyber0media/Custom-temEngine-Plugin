package com.customitem.engine;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Interactive 3x3 Recipe Builder GUI.
 * Slots 0-8 of a 27-slot inventory form the crafting grid.
 * Slot 13 holds the live preview/result. Slot 26 is the Save button.
 */
public final class RecipeBuilderGUI implements Listener {

    public static final String TITLE = ChatColor.DARK_GRAY + "Recipe Builder";
    public static final int SIZE = 27;
    public static final int SAVE_SLOT = 26;

    // Grid slots 0-8, result preview slot 13.
    private final CustomItemEngine plugin;
    // Player -> item id being crafted.
    private final Map<UUID, String> editing = new HashMap<>();

    public RecipeBuilderGUI(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String itemId) {
        Inventory inv = Bukkit.createInventory(player, SIZE, TITLE);
        renderChrome(inv, itemId);
        player.openInventory(inv);
        editing.put(player.getUniqueId(), itemId);
    }

    private void renderChrome(Inventory inv, String itemId) {
        ItemStack result = plugin.getItemRegistry().build(itemId);
        if (result != null) {
            inv.setItem(13, result);
        }
        ItemStack save = nameItem(Material.EMERALD_BLOCK,
                ChatColor.GREEN + "Save & Activate", itemId);
        inv.setItem(SAVE_SLOT, save);
        ItemStack info = nameItem(Material.PAPER,
                ChatColor.AQUA + "Place ingredients in the 3x3 grid", null);
        inv.setItem(22, info);
    }

    private ItemStack nameItem(Material mat, String name, String itemId) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            s.setItemMeta(m);
        }
        return s;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;

        int slot = event.getRawSlot();
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (!editing.containsKey(uuid)) return;

        // Allow clicks only in the 3x3 grid (0-8) and the save slot.
        if (slot >= SIZE) {
            // Player's own inventory: allow to pick/place ingredients.
            return;
        }

        if (slot == SAVE_SLOT) {
            event.setCancelled(true);
            save(player);
            return;
        }

        // Lock non-grid, non-player slots (e.g. result preview row).
        if (slot < 9) {
            // Grid slots: allow.
            updatePreview(player, event.getInventory());
            return;
        }
        if (slot == 13) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        // Only allow drags within the grid slots.
        for (int s : event.getRawSlots()) {
            if (s < SIZE && s >= 9 && s != 13) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private void updatePreview(Player player, Inventory inv) {
        UUID uuid = player.getUniqueId();
        String itemId = editing.get(uuid);
        if (itemId == null) return;
        ItemStack result = plugin.getItemRegistry().build(itemId);
        if (result != null) inv.setItem(13, result);
    }

    private void save(Player player) {
        UUID uuid = player.getUniqueId();
        String itemId = editing.remove(uuid);
        if (itemId == null) return;
        Inventory inv = player.getOpenInventory().getTopInventory();
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = inv.getItem(i);
        }
        plugin.getRecipeManager().saveRecipe(itemId, grid);
        player.sendMessage(ChatColor.GREEN + "Recipe saved and activated for engine:" + itemId);
        // Return any placed items to the player.
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s != null && s.getType() != Material.AIR) {
                player.getInventory().addItem(s).forEach((idx, left) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
                inv.setItem(i, null);
            }
        }
        player.closeInventory();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (!editing.containsKey(uuid)) return;
        // Return items on close without saving.
        editing.remove(uuid);
        Inventory inv = event.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s != null && s.getType() != Material.AIR) {
                player.getInventory().addItem(s).forEach((idx, left) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
                inv.setItem(i, null);
            }
        }
    }
}
