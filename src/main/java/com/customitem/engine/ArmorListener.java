package com.customitem.engine;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles custom armor equipping via shift-click and right-click,
 * GUI safety (no auto-equip from engine menus), and configurable set bonuses.
 */
public final class ArmorListener implements Listener {

    public static final String GUI_TITLE = ChatColor.DARK_GRAY + "CustomItemEngine";

    private final CustomItemEngine plugin;
    private final Map<UUID, String> activeSetBonuses = new HashMap<>();
    // Attribute modifier name used for set bonus.
    private static final String BONUS_NAME = "engine_set_bonus";

    public ArmorListener(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        // GUI safety: clicking armor inside engine menus must move to inventory,
        // never auto-equip. We cancel and manually place it.
        if (title.startsWith(GUI_TITLE)) {
            handleGuiClick(event, player);
            return;
        }

        // Shift-click equip logic for custom armor.
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            ItemStack cursor = event.getCurrentItem();
            if (plugin.getItemRegistry().isCustom(cursor)) {
                if (tryEquipShift(player, cursor)) {
                    event.setCancelled(true);
                    event.setCurrentItem(null);
                    player.updateInventory();
                    checkSetBonus(player);
                }
            }
        }
        checkSetBonus(player);
    }

    private void handleGuiClick(InventoryClickEvent event, Player player) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null) return;
        if (!plugin.getItemRegistry().isCustom(clicked)) {
            // It's a menu icon; cancel to prevent weird swaps.
            event.setCancelled(true);
            return;
        }
        // Custom item clicked in GUI: move to player inventory or drop.
        event.setCancelled(true);
        PlayerInventory inv = player.getInventory();
        Map<Integer, ItemStack> overflow = inv.addItem(clicked);
        overflow.forEach((i, left) ->
                player.getWorld().dropItemNaturally(player.getLocation(), left));
        event.setCurrentItem(null);
        player.updateInventory();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!plugin.getItemRegistry().isCustom(item)) return;
        // Right-click while holding armor to equip.
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                || event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            String slot = plugin.getItemRegistry().slotOf(item);
            if (slot == null) return;
            if (tryEquipHand(player, item, slot)) {
                event.setCancelled(true);
                player.updateInventory();
                checkSetBonus(player);
            }
        }
    }

    private boolean tryEquipShift(Player player, ItemStack item) {
        String slot = plugin.getItemRegistry().slotOf(item);
        if (slot == null) return false;
        return equip(player, item, slot);
    }

    private boolean tryEquipHand(Player player, ItemStack item, String slot) {
        return equip(player, item, slot);
    }

    private boolean equip(Player player, ItemStack item, String slot) {
        PlayerInventory inv = player.getInventory();
        int targetSlot;
        switch (slot.toUpperCase()) {
            case "HELMET": targetSlot = 39; break;
            case "CHESTPLATE": targetSlot = 38; break;
            case "LEGGINGS": targetSlot = 37; break;
            case "BOOTS": targetSlot = 36; break;
            default: return false;
        }
        ItemStack current = inv.getItem(targetSlot);
        inv.setItem(targetSlot, item);
        // Place the hand item (or cursor) back.
        if (inv.getItemInMainHand().equals(item)) {
            inv.setItemInMainHand(current);
        } else {
            // shift-click case: item came from clicked slot, handled by caller.
            if (current != null) {
                Map<Integer, ItemStack> overflow = inv.addItem(current);
                overflow.forEach((i, left) ->
                        player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
        return true;
    }

    /** Checks whether all 4 equipped armor pieces share a set and applies bonus. */
    public void checkSetBonus(Player player) {
        PlayerInventory inv = player.getInventory();
        String helmet = plugin.getItemRegistry().idOf(inv.getHelmet());
        String chest = plugin.getItemRegistry().idOf(inv.getChestplate());
        String legs = plugin.getItemRegistry().idOf(inv.getLeggings());
        String boots = plugin.getItemRegistry().idOf(inv.getBoots());

        if (helmet == null || chest == null || legs == null || boots == null) {
            removeBonus(player);
            return;
        }

        String setHelmet = plugin.getItemRegistry().setOf(inv.getHelmet());
        String setChest = plugin.getItemRegistry().setOf(inv.getChestplate());
        String setLegs = plugin.getItemRegistry().setOf(inv.getLeggings());
        String setBoots = plugin.getItemRegistry().setOf(inv.getBoots());

        if (setHelmet == null || !setHelmet.equals(setChest)
                || !setHelmet.equals(setLegs) || !setHelmet.equals(setBoots)) {
            removeBonus(player);
            return;
        }

        // All four match; look up set bonus from armours config.
        ConfigurationSection armoursCfg = plugin.getCategoryLoader().get("armours");
        if (armoursCfg == null) return;
        ConfigurationSection bonusSec = armoursCfg.getConfigurationSection("set_bonus");
        if (bonusSec == null || !bonusSec.getBoolean("enabled", false)) {
            removeBonus(player);
            return;
        }

        applyBonus(player, setHelmet, bonusSec);
    }

    private void applyBonus(Player player, String setName, ConfigurationSection bonusSec) {
        String type = bonusSec.getString("type", "flat-hearts");
        double value = bonusSec.getDouble("value", 0);
        UUID uuid = player.getUniqueId();

        // Already applied for this set: skip.
        if (setName.equals(activeSetBonuses.get(uuid))) return;
        removeBonus(player);

        Attribute attr = Attribute.GENERIC_MAX_HEALTH;
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;

        AttributeModifier.Operation op;
        double amount;
        if (type.equalsIgnoreCase("percent-health")) {
            op = AttributeModifier.Operation.MULTIPLY_SCALAR_1;
            amount = value / 100.0;
        } else {
            // flat-hearts -> 2 points per heart
            op = AttributeModifier.Operation.ADD_NUMBER;
            amount = value * 2.0;
        }

        AttributeModifier mod = new AttributeModifier(
                new java.util.UUID(0xBEEF, setName.hashCode()), BONUS_NAME,
                amount, op);
        inst.addModifier(mod);
        activeSetBonuses.put(uuid, setName);
        player.sendMessage(ChatColor.GREEN + "Set bonus activated: " + setName);
    }

    private void removeBonus(Player player) {
        UUID uuid = player.getUniqueId();
        if (!activeSetBonuses.containsKey(uuid)) return;
        AttributeInstance inst = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (inst != null) {
            inst.getModifiers().stream()
                    .filter(m -> m.getName().equals(BONUS_NAME))
                    .forEach(inst::removeModifier);
        }
        activeSetBonuses.remove(uuid);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        checkSetBonus(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBonus(event.getPlayer());
    }
}
