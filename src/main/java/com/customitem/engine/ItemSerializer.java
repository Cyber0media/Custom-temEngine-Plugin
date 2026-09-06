package com.customitem.engine;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Serialization helpers for ItemStacks and NBT persistence.
 * Used to persist custom items (e.g. in database-backed storage) and to
 * round-trip engine id / CustomModelData metadata.
 */
public final class ItemSerializer {

    private ItemSerializer() {}

    /** Serializes an ItemStack to a Base64 string via Bukkit's object streams. */
    public static String toBase64(ItemStack stack) {
        if (stack == null) return null;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            out.writeObject(stack);
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException e) {
            return null;
        }
    }

    /** Deserializes a Base64 string back into an ItemStack. */
    public static ItemStack fromBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) return null;
        try (ByteArrayInputStream bytes =
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded));
             BukkitObjectInputStream in = new BukkitObjectInputStream(bytes)) {
            Object obj = in.readObject();
            return (obj instanceof ItemStack) ? (ItemStack) obj : null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Returns the engine id stored on an ItemStack's persistent data container,
     * or null if the stack is not a custom engine item.
     */
    public static String engineId(ItemStack stack, ItemRegistry registry) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(registry.idKey(), PersistentDataType.STRING);
    }

    /** True if the stack carries a CustomModelData tag. */
    public static boolean hasCustomModelData(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        return stack.getItemMeta().hasCustomModelData();
    }

    /** Returns the CustomModelData value, or 0 if absent. */
    public static int customModelData(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return 0;
        ItemMeta meta = stack.getItemMeta();
        return meta.hasCustomModelData() ? meta.getCustomModelData() : 0;
    }
}
