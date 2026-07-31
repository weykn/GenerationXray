package com.seedxray.ore;

import com.seedxray.config.BlockEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The colour palette the mod ships with. Blocks outside of it are still trackable,
 * they just fall back to their map colour when they get added to the list.
 */
public final class DefaultBlocks {
    private static final Map<Block, Integer> PALETTE = new LinkedHashMap<>();

    /** blocks the list starts out with - the ones people actually dig for */
    private static final List<Block> DEFAULT_LIST;

    static {
        PALETTE.put(Blocks.COAL_ORE, 0x111111);
        PALETTE.put(Blocks.DEEPSLATE_COAL_ORE, 0x111111);
        PALETTE.put(Blocks.COPPER_ORE, 0xFF6600);
        PALETTE.put(Blocks.DEEPSLATE_COPPER_ORE, 0xFF6600);
        PALETTE.put(Blocks.RAW_COPPER_BLOCK, 0xFF3300);
        PALETTE.put(Blocks.IRON_ORE, 0xFFCC99);
        PALETTE.put(Blocks.DEEPSLATE_IRON_ORE, 0xFFCC99);
        PALETTE.put(Blocks.RAW_IRON_BLOCK, 0xCC6633);
        PALETTE.put(Blocks.GOLD_ORE, 0xFFFF33);
        PALETTE.put(Blocks.DEEPSLATE_GOLD_ORE, 0xFFFF33);
        PALETTE.put(Blocks.REDSTONE_ORE, 0x990000);
        PALETTE.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0x990000);
        PALETTE.put(Blocks.LAPIS_ORE, 0x3333CC);
        PALETTE.put(Blocks.DEEPSLATE_LAPIS_ORE, 0x3333CC);
        PALETTE.put(Blocks.DIAMOND_ORE, 0x00FFFF);
        PALETTE.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0x00FFFF);
        PALETTE.put(Blocks.EMERALD_ORE, 0x33FF33);
        PALETTE.put(Blocks.DEEPSLATE_EMERALD_ORE, 0x33FF33);
        PALETTE.put(Blocks.ANCIENT_DEBRIS, 0x995533);
        PALETTE.put(Blocks.NETHER_GOLD_ORE, 0xFFFF33);
        PALETTE.put(Blocks.NETHER_QUARTZ_ORE, 0xEEEEEE);

        PALETTE.put(Blocks.ANDESITE, 0x333333);
        PALETTE.put(Blocks.DIORITE, 0xCCCCCC);
        PALETTE.put(Blocks.GRANITE, 0xCC3300);
        PALETTE.put(Blocks.TUFF, 0x996633);
        PALETTE.put(Blocks.CLAY, 0x9999CC);
        PALETTE.put(Blocks.DIRT, 0x996633);
        PALETTE.put(Blocks.GRAVEL, 0x666699);
        PALETTE.put(Blocks.INFESTED_STONE, 0xFF0099);
        PALETTE.put(Blocks.INFESTED_DEEPSLATE, 0xFF0099);
        PALETTE.put(Blocks.BLACKSTONE, 0x333333);
        PALETTE.put(Blocks.MAGMA_BLOCK, 0xFF6600);
        PALETTE.put(Blocks.SOUL_SAND, 0x112233);

        DEFAULT_LIST = List.of(
                Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE,
                Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.RAW_COPPER_BLOCK,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE,
                Blocks.ANCIENT_DEBRIS, Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE);
    }

    private DefaultBlocks() {
    }

    public static List<BlockEntry> defaultEntries() {
        List<BlockEntry> entries = new ArrayList<>(DEFAULT_LIST.size());
        for (Block block : DEFAULT_LIST) {
            entries.add(BlockEntry.of(block, colorFor(block)));
        }
        return entries;
    }

    /** palette colour if we have one, otherwise whatever the block looks like on a map */
    public static int colorFor(Block block) {
        Integer color = PALETTE.get(block);
        if (color != null) {
            return color;
        }

        int mapColor = block.defaultMapColor().col & 0xFFFFFF;
        return mapColor == 0 ? 0xFFFFFF : mapColor;
    }
}
