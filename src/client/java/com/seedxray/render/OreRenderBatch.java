package com.seedxray.render;

import it.unimi.dsi.fastutil.longs.LongCollection;

/** every position of one tracked block, plus the colours to draw them in */
public record OreRenderBatch(
        LongCollection positions,
        int fillArgb,
        int outlineArgb
) {
}
