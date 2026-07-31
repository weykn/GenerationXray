package com.seedxray.config;

import com.seedxray.ore.DefaultBlocks;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@Config(name = "seedxray")
public class SeedXRayConfig implements ConfigData {
    /** loading and rendering of the outlines, the one master switch */
    public boolean active = false;

    public long seed = 0;

    /** blocks to look for, in the order they show up in the block list screen */
    public List<BlockEntry> blocks = DefaultBlocks.defaultEntries();

    /** outlines are always drawn, this adds the translucent box inside them */
    public boolean renderFilled = true;

    /** region radius in chunks */
    public int renderDistance = 2;

    /** sections above/below the player, -1 for no limit */
    public int verticalViewDistance = -1;

    public boolean hudStatusIndicator = false;

    @Override
    public void validatePostLoad() {
        this.renderDistance = clamp(this.renderDistance,
                ConfigConstants.MIN_RENDER_DISTANCE, ConfigConstants.MAX_RENDER_DISTANCE);
        this.verticalViewDistance = clamp(this.verticalViewDistance,
                ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE, ConfigConstants.MAX_VERTICAL_VIEW_DISTANCE);

        // an older config, or a hand edited one, can leave this null or full of junk
        if (this.blocks == null) {
            this.blocks = DefaultBlocks.defaultEntries();
            return;
        }
        List<BlockEntry> cleaned = new ArrayList<>(this.blocks.size());
        for (BlockEntry entry : this.blocks) {
            if (entry != null && entry.block != null) {
                entry.rgb &= 0xFFFFFF;
                cleaned.add(entry);
            }
        }
        this.blocks = cleaned;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class ConfigConstants {
        public static final int MIN_VERTICAL_VIEW_DISTANCE = -1;
        public static final int MAX_VERTICAL_VIEW_DISTANCE = 10;

        public static final int MIN_RENDER_DISTANCE = 0;
        public static final int MAX_RENDER_DISTANCE = 9;
    }
}
