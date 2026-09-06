package com.customitem.engine;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;

/**
 * Applies configured potion effects when a custom food item is eaten.
 */
public final class FoodListener implements Listener {

    private final CustomItemEngine plugin;

    public FoodListener(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        String id = plugin.getItemRegistry().idOf(item);
        if (id == null) return;

        var pdc = item.getItemMeta().getPersistentDataContainer();
        Integer nutrition = pdc.get(plugin.getItemRegistry().foodNutritionKey(),
                PersistentDataType.INTEGER);
        Double saturation = pdc.get(plugin.getItemRegistry().foodSaturationKey(),
                PersistentDataType.DOUBLE);
        String effectsRaw = pdc.get(plugin.getItemRegistry().foodEffectsKey(),
                PersistentDataType.STRING);

        Player player = event.getPlayer();

        // Apply nutrition/saturation manually since we use vanilla food base.
        if (nutrition != null && nutrition > 0) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + nutrition));
        }
        if (saturation != null && saturation > 0) {
            player.setSaturation(Math.min(20f,
                    (float) (player.getSaturation() + saturation)));
        }

        if (effectsRaw != null && !effectsRaw.isEmpty()) {
            List<String> effects = Arrays.asList(effectsRaw.split("\\|"));
            for (String entry : effects) {
                String[] parts = entry.split(":");
                if (parts.length < 3) continue;
                String effectName = parts[0].trim();
                int duration = parseSeconds(parts[1]) * 20; // ticks
                int amplifier = parseInt(parts[2], 0);
                PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase());
                if (type == null) continue;
                player.addPotionEffect(new PotionEffect(type, duration, amplifier));
            }
        }
    }

    private int parseSeconds(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 5; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}
