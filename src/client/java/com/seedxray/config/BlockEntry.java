package com.seedxray.config;

import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * One entry of the tracked block list: which block to look for and what colour to
 * draw it in. Stored by id so any block - vanilla or modded - can be added.
 */
public class BlockEntry {
    /** registry id, e.g. {@code minecraft:diamond_ore} */
    public String block = "minecraft:air";
    /** 0xRRGGBB, the alpha is decided by the renderer */
    public int rgb = 0xFFFFFF;

    /** required by gson */
    public BlockEntry() {
    }

    public BlockEntry(String block, int rgb) {
        this.block = block;
        this.rgb = rgb & 0xFFFFFF;
    }

    public static BlockEntry of(Block block, int rgb) {
        return new BlockEntry(BuiltInRegistries.BLOCK.getKey(block).toString(), rgb);
    }

    /** the block this entry points at, or air when the id is unknown (removed mod, typo) */
    public Block resolve() {
        Identifier id = this.block == null ? null : Identifier.tryParse(this.block);
        if (id == null) {
            return Blocks.AIR;
        }
        return BuiltInRegistries.BLOCK.getValue(id);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BlockEntry entry
                && this.rgb == entry.rgb
                && Objects.equals(this.block, entry.block);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(this.block) + this.rgb;
    }
}
