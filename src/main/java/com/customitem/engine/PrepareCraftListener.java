package com.customitem.engine;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

/**
 * Validates that custom ingredients in crafting recipes match exact NBT keys,
 * preventing players from substituting plain vanilla items for custom ones.
 */
public final class PrepareCraftListener implements Listener {

    private final CustomItemEngine plugin;

    public PrepareCraftListener(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack result = inv.getResult();
        if (result == null) return;

        // Only enforce for engine recipes (result is a custom item).
        String resultId = plugin.getItemRegistry().idOf(result);
        if (resultId == null) return;

        String[][] matrix = plugin.getRecipeManager().getMatrix(resultId);
        if (matrix == null) return;

        ItemStack[] contents = inv.getMatrix();
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            String expected = matrix[row][col];
            ItemStack actual = (i < contents.length) ? contents[i] : null;

            if (expected == null || expected.equals("AIR")) {
                if (actual != null && !actual.getType().isAir()) {
                    inv.setResult(null);
                    return;
                }
                continue;
            }

            if (expected.startsWith("engine:")) {
                String expectedId = expected.substring("engine:".length());
                String actualId = plugin.getItemRegistry().idOf(actual);
                if (actualId == null || !actualId.equals(expectedId)) {
                    inv.setResult(null);
                    return;
                }
            } else {
                // Vanilla material must match exactly.
                if (actual == null || !actual.getType().name().equalsIgnoreCase(expected)) {
                    inv.setResult(null);
                    return;
                }
            }
        }
    }
}
