package com.customitem.engine;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Generates custom ores inside deepslate substrate, within a configured Y range.
 * Supports configurable vein density and a high-density vein zone.
 */
public final class OrePopulator extends BlockPopulator {

    private final CustomItemEngine plugin;

    public OrePopulator(CustomItemEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ,
                         LimitedRegion region) {
        int veinChance = plugin.getConfig().getInt("ore-generation.vein-chance", 8);
        int attempts = plugin.getConfig().getInt("ore-generation.attempts-per-chunk", 6);
        int minY = plugin.getConfig().getInt("ore-generation.min-y", -64);
        int maxY = plugin.getConfig().getInt("ore-generation.max-y", -45);
        int veinMinY = plugin.getConfig().getInt("ore-generation.vein-min-y", -50);
        int veinMaxY = plugin.getConfig().getInt("ore-generation.vein-max-y", -55);
        int minSize = plugin.getConfig().getInt("ore-generation.vein-min-size", 3);
        int maxSize = plugin.getConfig().getInt("ore-generation.vein-max-size", 5);

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (String id : plugin.getItemRegistry().all().keySet()) {
            ConfigurationSection sec = plugin.getItemRegistry().get(id).section;
            ConfigurationSection oreSec = sec.getConfigurationSection("ore");
            if (oreSec == null || !oreSec.getBoolean("enabled", true)) continue;

            String blockType = oreSec.getString("block", "DEEPSLATE");
            if (!blockType.equalsIgnoreCase("DEEPSLATE")) continue;

            int oreMinY = oreSec.getInt("min-y", minY);
            int oreMaxY = oreSec.getInt("max-y", maxY);

            for (int attempt = 0; attempt < attempts; attempt++) {
                if (random.nextInt(100) >= veinChance) continue;

                int x = baseX + random.nextInt(16);
                int z = baseZ + random.nextInt(16);

                // Bias toward the high-density vein zone 30% of the time.
                int y;
                if (random.nextInt(100) < 30) {
                    int lo = Math.min(veinMinY, veinMaxY);
                    int hi = Math.max(veinMinY, veinMaxY);
                    y = lo + random.nextInt(Math.max(1, hi - lo + 1));
                } else {
                    int lo = Math.min(oreMinY, oreMaxY);
                    int hi = Math.max(oreMinY, oreMaxY);
                    y = lo + random.nextInt(Math.max(1, hi - lo + 1));
                }

                int size = minSize + random.nextInt(Math.max(1, maxSize - minSize + 1));
                generateVein(region, x, y, z, size, random);
            }
        }
    }

    /**
     * Generates a small blob-shaped vein of custom ore blocks.
     * Because custom blocks cannot be registered without a resource pack,
     * we embed the engine id as a tile-less marker by replacing deepslate with
     * a visually-distinct vanilla block (e.g. DEEPSLATE_DIAMOND_ORE) and
     * storing the engine id via block metadata where available. On break,
     * BlockBreakListener translates the block back to a custom item drop.
     */
    private void generateVein(LimitedRegion region, int x, int y, int z,
                              int size, Random random) {
        // Use deepslate diamond ore as the visible surrogate; the break
        // listener inspects the block to decide the real drop.
        Material surrogate = Material.DEEPSLATE_DIAMOND_ORE;
        for (int i = 0; i < size; i++) {
            int dx = x + random.nextInt(2) - random.nextInt(2);
            int dy = y + random.nextInt(2) - random.nextInt(2);
            int dz = z + random.nextInt(2) - random.nextInt(2);
            if (!region.isInRegion(dx, dy, dz)) continue;
            // Only replace deepslate substrate.
            if (region.getType(dx, dy, dz) != Material.DEEPSLATE) continue;
            region.setType(dx, dy, dz, surrogate, false);
        }
    }
}
