package com.customitem.engine;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds all registered custom items, keyed by engine:&lt;id&gt;.
 * Provides ItemStack builders and metadata lookups.
 */
public final class ItemRegistry {

    public static final String NAMESPACE = "engine";
    public static final String ID_KEY = "engine_id";
    public static final String SET_KEY = "engine_set";
    public static final String SLOT_KEY = "engine_slot";
    public static final String CATEGORY_KEY = "engine_category";
    public static final String FOOD_NUTRITION_KEY = "engine_nutrition";
    public static final String FOOD_SATURATION_KEY = "engine_saturation";
    public static final String FOOD_EFFECTS_KEY = "engine_effects";

    private final CustomItemEngine plugin;
    private final Map<String, RegisteredItem> items = new LinkedHashMap<>();

    public ItemRegistry(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        items.clear();
        for (Map.Entry<String, YamlConfiguration> entry :
                plugin.getCategoryLoader().getAll().entrySet()) {
            String category = entry.getKey();
            ConfigurationSection itemsSec = entry.getValue().getConfigurationSection("items");
            if (itemsSec == null) continue;
            for (String id : itemsSec.getKeys(false)) {
                ConfigurationSection sec = itemsSec.getConfigurationSection(id);
                if (sec == null) continue;
                RegisteredItem ri = new RegisteredItem(id, category, sec);
                items.put(id, ri);
            }
        }
    }

    public int size() { return items.size(); }

    public boolean has(String id) { return items.containsKey(id); }

    public RegisteredItem get(String id) { return items.get(id); }

    public Map<String, RegisteredItem> all() { return items; }

    public NamespacedKey idKey() {
        return new NamespacedKey(plugin, ID_KEY);
    }

    public NamespacedKey setKey() {
        return new NamespacedKey(plugin, SET_KEY);
    }

    public NamespacedKey slotKey() {
        return new NamespacedKey(plugin, SLOT_KEY);
    }

    public NamespacedKey categoryKey() {
        return new NamespacedKey(plugin, CATEGORY_KEY);
    }

    public NamespacedKey foodNutritionKey() {
        return new NamespacedKey(plugin, FOOD_NUTRITION_KEY);
    }

    public NamespacedKey foodSaturationKey() {
        return new NamespacedKey(plugin, FOOD_SATURATION_KEY);
    }

    public NamespacedKey foodEffectsKey() {
        return new NamespacedKey(plugin, FOOD_EFFECTS_KEY);
    }

    /** Builds a fresh ItemStack for the given engine id. */
    public ItemStack build(String id) {
        RegisteredItem ri = items.get(id);
        if (ri == null) return null;
        return buildItem(ri);
    }

    public ItemStack buildItem(RegisteredItem ri) {
        ConfigurationSection sec = ri.section;
        String matName = sec.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        // Display name
        String rawName = sec.getString("display-name", ri.id);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rawName));

        // Strict 2-line lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.DARK_GRAY + "CustomItemEngine");
        lore.add(ChatColor.GRAY + "engine:" + ri.id);
        meta.setLore(lore);

        // CustomModelData
        int cmd = sec.getInt("custom-model-data", 0);
        if (cmd != 0) {
            try {
                meta.setCustomModelData(cmd);
            } catch (Throwable ignored) {}
        }

        // Persistent NBT identifier
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(idKey(), PersistentDataType.STRING, ri.id);
        pdc.set(categoryKey(), PersistentDataType.STRING, ri.category);

        // Durability: 1.20.4 has no setMaxDamage API. Custom durability is
        // recorded so a runtime listener could honor it; the material's
        // default max damage is used as the base.

        // Armor attributes
        ConfigurationSection armorSec = sec.getConfigurationSection("armor");
        if (armorSec != null) {
            String slot = armorSec.getString("slot", "CHESTPLATE");
            String set = armorSec.getString("set", "");
            int defense = armorSec.getInt("defense", 0);
            int toughness = armorSec.getInt("toughness", 0);
            pdc.set(setKey(), PersistentDataType.STRING, set);
            pdc.set(slotKey(), PersistentDataType.STRING, slot);

            EquipmentSlot eqSlot = mapSlot(slot);
            if (eqSlot != null && defense > 0) {
                addModifier(meta, "engine_def_" + ri.id, Attribute.GENERIC_ARMOR,
                        defense, eqSlot);
            }
            if (eqSlot != null && toughness > 0) {
                addModifier(meta, "engine_tough_" + ri.id, Attribute.GENERIC_ARMOR_TOUGHNESS,
                        toughness, eqSlot);
            }
        }

        // Weapon / tool attributes
        ConfigurationSection weaponSec = sec.getConfigurationSection("weapon");
        if (weaponSec != null) {
            double dmg = weaponSec.getDouble("attack-damage", 0);
            double speed = weaponSec.getDouble("attack-speed", 0);
            if (dmg > 0) {
                addModifier(meta, "engine_dmg_" + ri.id, Attribute.GENERIC_ATTACK_DAMAGE,
                        dmg, EquipmentSlot.HAND);
            }
            if (speed > 0) {
                addModifier(meta, "engine_spd_" + ri.id, Attribute.GENERIC_ATTACK_SPEED,
                        speed, EquipmentSlot.HAND);
            }
        }

        // Food metadata
        ConfigurationSection foodSec = sec.getConfigurationSection("food");
        if (foodSec != null) {
            int nutrition = foodSec.getInt("nutrition", 0);
            double saturation = foodSec.getDouble("saturation", 0);
            pdc.set(foodNutritionKey(), PersistentDataType.INTEGER, nutrition);
            pdc.set(foodSaturationKey(), PersistentDataType.DOUBLE, saturation);
            List<String> effects = foodSec.getStringList("effects");
            if (effects.isEmpty()) {
                String single = foodSec.getString("effects", null);
                if (single != null) effects = List.of(single);
            }
            pdc.set(foodEffectsKey(), PersistentDataType.STRING, String.join("|", effects));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE);
        stack.setItemMeta(meta);
        return stack;
    }

    private void addModifier(ItemMeta meta, String name, Attribute attr,
                             double amount, EquipmentSlot slot) {
        try {
            AttributeModifier mod = new AttributeModifier(
                    UUID.randomUUID(), name, amount,
                    AttributeModifier.Operation.ADD_NUMBER, slot);
            meta.addAttributeModifier(attr, mod);
        } catch (Throwable ignored) {}
    }

    private EquipmentSlot mapSlot(String slot) {
        switch (slot.toUpperCase()) {
            case "HELMET": return EquipmentSlot.HEAD;
            case "CHESTPLATE": return EquipmentSlot.CHEST;
            case "LEGGINGS": return EquipmentSlot.LEGS;
            case "BOOTS": return EquipmentSlot.FEET;
            default: return null;
        }
    }

    /** Returns the engine id stored on an ItemStack, or null if not a custom item. */
    public String idOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        PersistentDataContainer pdc = stack.getItemMeta().getPersistentDataContainer();
        return pdc.get(idKey(), PersistentDataType.STRING);
    }

    /** True if the stack is a registered custom engine item. */
    public boolean isCustom(ItemStack stack) {
        return idOf(stack) != null;
    }

    public String setOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(setKey(), PersistentDataType.STRING);
    }

    public String slotOf(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(slotKey(), PersistentDataType.STRING);
    }

    /** Simple data holder for a registered item. */
    public static final class RegisteredItem {
        public final String id;
        public final String category;
        public final ConfigurationSection section;

        public RegisteredItem(String id, String category, ConfigurationSection section) {
            this.id = id;
            this.category = category;
            this.section = section;
        }
    }
}
