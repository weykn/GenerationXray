package com.seedxray.ore;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import net.minecraft.world.level.block.Block;

/** positions of tracked blocks inside one chunk section */
public class OreDataMap {
    private final Reference2ObjectMap<Block, LongCollection> map = new Reference2ObjectOpenHashMap<>();

    public OreDataMap() {
    }

    public void put(Block block, long pos) {
        map.computeIfAbsent(block, key -> new LongArrayList()).add(pos);
    }

    public LongCollection get(Block block) {
        LongCollection collection = map.get(block);

        if (collection != null) {
            return collection;
        }
        return LongArrayList.of();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }
}
