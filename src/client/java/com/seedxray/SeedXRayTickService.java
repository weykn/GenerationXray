package com.seedxray;

import com.casiowatch123.vchunklib.generation.virtual.VUtils;
import com.casiowatch123.vchunklib.generation.virtual.world.VWorldService;
import com.seedxray.config.BlockEntry;
import com.seedxray.config.SeedXRayConfig;
import com.seedxray.render.OreRenderBatch;
import com.seedxray.world.OreChunkLoader;
import com.seedxray.world.gen.GenChunkHolder;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import me.shedaniel.autoconfig.ConfigHolder;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.dimension.DimensionType;

public class SeedXRayTickService {
    private static final Logger LOGGER = SeedXRay.LOGGER;

    /** how see-through the "render filled" box is */
    private static final int FILL_ALPHA = 0x50;

    private final ConfigHolder<SeedXRayConfig> configHolder;
    private PalettedContainerFactory palettesFactory;

    private VWorldService vWorld;
    private OreChunkLoader loader;
    private ChunkPos pos;
    private ResourceKey<Level> dimensionKey;
    private long seed;
    private int loadRadius = -1;

    /** blocks to look for, rebuilt whenever the configured list changes */
    private Set<Block> trackedBlocks = Set.of();
    private final Reference2IntMap<Block> trackedColors = new Reference2IntOpenHashMap<>();
    private int trackedVersion;
    /** hash of the config list the two fields above were built from */
    private int blockListHash;

    private CompletableFuture<StaticCache2D<GenChunkHolder>> regionFuture;
    private LoadStatus regionLoadStatus = LoadStatus.LOAD_DISABLED;

    private volatile List<OreRenderBatch> renderData = null;

    public SeedXRayTickService(ConfigHolder<SeedXRayConfig> configHolder) {
        this.configHolder = configHolder;
        rebuildTrackedBlocks();
    }

    public void updateTick(Minecraft client) {
        if (SeedXRayClient.DRM == null) {
            return;
        } else if (this.palettesFactory == null) {
            this.palettesFactory = PalettedContainerFactory.create(SeedXRayClient.DRM);
        }

        if (client.player == null || client.level == null || getDimensionKey(client.level) == Level.END) {
            this.renderData = null;
            return;
        }

        LocalPlayer player = client.player;
        ClientLevel world = client.level;
        SeedXRayConfig config = configHolder.getConfig();

        // block list edits have to reach the chunks that were already generated
        if (config.blocks.hashCode() != this.blockListHash) {
            rebuildTrackedBlocks();
            this.pos = null;
        }

        //loader

        if (this.vWorld == null || this.dimensionKey == null
         || config.seed != this.seed
         || this.dimensionKey != getDimensionKey(world)) {
            rebuildWorld(getDimensionKey(world), config.seed);
        }

        //v chunk region
        if (config.active) {
            //reload
            ChunkPos playerPos = player.chunkPosition();
            if (pos == null || this.loadRadius == -1
                 ||playerPos.pack() != pos.pack()
                 || config.renderDistance != this.loadRadius) {
                this.regionFuture = loader.loadChunks(
                        playerPos, config.renderDistance, this.trackedBlocks, this.trackedVersion);
                this.pos = player.chunkPosition();
                this.loadRadius = config.renderDistance;
            }
        } else {
            this.pos = null;
            this.regionFuture = null;
            this.renderData = null;
            this.loader.clearCache();
        }

        //ore render data
        if (regionFuture != null &&
        regionFuture.isDone() &&
        !regionFuture.isCancelled() &&
        !regionFuture.isCompletedExceptionally()) {
            //generate render data
            StaticCache2D<GenChunkHolder> region = regionFuture.join();
            if (region == null) {
                LOGGER.warn("render data update failed [{}, {}]", pos.x(), pos.z());
                return;
            }

            List<OreRenderBatch> renderData = new ArrayList<>(this.trackedBlocks.size());

            int vvd = config.verticalViewDistance;
            int startSectionY = (player.getBlockY() >> 4) - vvd;
            int endSectionY = (player.getBlockY() >> 4) + vvd;

            for (Block block : this.trackedBlocks) {
                LongCollection collection = new LongArrayList();

                region.forEach(holder -> {
                    if (vvd == -1) {
                        collection.addAll(holder.getOreData(block));
                    } else {
                        collection.addAll(holder.getOreData(block, startSectionY, endSectionY));
                    }
                });

                if (collection.isEmpty()) {
                    continue;
                }

                int rgb = this.trackedColors.getInt(block) & 0xFFFFFF;
                renderData.add(new OreRenderBatch(
                        collection,
                        (FILL_ALPHA << 24) | rgb,
                        0xFF000000 | rgb));
            }
            this.renderData = renderData;
        }

        //load status data
        if (regionFuture == null) {
            this.regionLoadStatus = LoadStatus.LOAD_DISABLED;
        } else if (regionFuture.isCompletedExceptionally()) {
            this.regionLoadStatus = LoadStatus.COMPLETED_EXCEPTIONALLY;
        } else if (regionFuture.isCancelled()) {
            this.regionLoadStatus = LoadStatus.CANCELLED;
        } else if (regionFuture.isDone()) {
            this.regionLoadStatus = LoadStatus.DONE;
        } else {
            this.regionLoadStatus = LoadStatus.IS_WORKING;
        }
    }

    /**
     * Turns the configured id list into the block set the scan runs against. The
     * version bumps only when the set of blocks changes, so recolouring an entry
     * does not throw away chunks that are already scanned.
     */
    private void rebuildTrackedBlocks() {
        List<BlockEntry> entries = configHolder.getConfig().blocks;
        Set<Block> tracked = new ReferenceOpenHashSet<>(entries.size());

        this.trackedColors.clear();
        for (BlockEntry entry : entries) {
            Block block = entry.resolve();
            if (block == Blocks.AIR) {
                continue;
            }
            tracked.add(block);
            this.trackedColors.put(block, entry.rgb & 0xFFFFFF);
        }

        if (!tracked.equals(this.trackedBlocks)) {
            this.trackedBlocks = tracked;
            this.trackedVersion++;
        }
        this.blockListHash = entries.hashCode();
        this.renderData = null;
    }

    /**
     * Throws away everything tied to the old seed/dimension and starts over, so a seed
     * change takes effect on the next tick instead of on the next world join.
     */
    private void rebuildWorld(ResourceKey<Level> dimensionKey, long seed) {
        if (dimensionKey == Level.END) {
            return;
        }

        // stop the in-flight region: its chunks were generated with the old seed, and
        // leaving it running would keep the shared generation pool busy for nothing
        if (this.loader != null) {
            this.loader.cancel();
        }
        if (this.regionFuture != null) {
            this.regionFuture.cancel(true);
            this.regionFuture = null;
        }

        this.dimensionKey = dimensionKey;
        this.seed = seed;
        this.vWorld = new VWorldService(
                SeedXRayClient.DRM,
                VUtils.createDimensionArg(dimensionKey, SeedXRayClient.DRM),
                seed);
        this.loader = new OreChunkLoader(this.vWorld, palettesFactory);

        // drop the old ores immediately - otherwise they keep being drawn until the
        // new region finishes generating
        this.renderData = null;
        this.pos = null;
        this.loadRadius = -1;
    }

    public List<OreRenderBatch> getOreRenderData() {
        List<OreRenderBatch> data = this.renderData;
        if (data != null) {
            return data;
        }
        return List.of();
    }

    /** total ore positions currently queued for rendering */
    public int getRenderedOreCount() {
        List<OreRenderBatch> data = this.renderData;
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (OreRenderBatch batch : data) {
            count += batch.positions().size();
        }
        return count;
    }

    public LoadStatus getLoadStatus() {
        return this.regionLoadStatus;
    }

    private static ResourceKey<Level> getDimensionKey(ClientLevel world) {
        if (world.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD) {
            return Level.OVERWORLD;
        }
        if (world.dimensionType().cardinalLightType() == CardinalLighting.Type.NETHER) {
            return Level.NETHER;
        }
        return Level.END;
    }

    public enum LoadStatus {
        LOAD_DISABLED,
        IS_WORKING,
        DONE,
        CANCELLED,
        COMPLETED_EXCEPTIONALLY;
    }
}
