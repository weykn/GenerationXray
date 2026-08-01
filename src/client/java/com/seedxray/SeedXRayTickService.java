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

    /** ticks to wait after a region failed to generate before asking for it again */
    private static final int FAILED_LOAD_RETRY_TICKS = 40;
    /** stands in for "no vertical limit" in the render data key */
    private static final int NO_SECTION_LIMIT = Integer.MIN_VALUE;

    private final ConfigHolder<SeedXRayConfig> configHolder;
    private PalettedContainerFactory palettesFactory;

    private VWorldService vWorld;
    private OreChunkLoader loader;
    private ResourceKey<Level> dimensionKey;
    private long seed;

    /** blocks to look for, rebuilt whenever the configured list changes */
    private Set<Block> trackedBlocks = Set.of();
    private final Reference2IntMap<Block> trackedColors = new Reference2IntOpenHashMap<>();
    private int trackedVersion;
    /** hash of the config list the two fields above were built from */
    private int blockListHash;

    /**
     * The load that is in flight, or the last one that ran. Only one region is ever
     * generated at a time: overlapping regions share their chunk holders, and two of
     * them taking the per chunk locks in different orders can wedge the whole pool.
     */
    private CompletableFuture<StaticCache2D<GenChunkHolder>> regionFuture;
    /** false while {@link #regionFuture} still has a result nobody looked at */
    private boolean regionConsumed = true;
    /** centre and radius {@link #regionFuture} was asked for, null when nothing was */
    private ChunkPos requestedPos;
    private int requestedRadius = -1;
    private int retryCooldown;

    /** newest region that finished generating, what the boxes below are built from */
    private StaticCache2D<GenChunkHolder> loadedRegion;
    private LoadStatus regionLoadStatus = LoadStatus.LOAD_DISABLED;

    private volatile List<OreRenderBatch> renderData = null;
    // what renderData was built from, so it is only rebuilt when one of them moves
    private StaticCache2D<GenChunkHolder> renderDataRegion;
    private int renderDataSectionY;
    private int renderDataVerticalDistance;
    private int renderDataFillAlpha;

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
        if (config.blocks.hashCode() != this.blockListHash && rebuildTrackedBlocks()) {
            invalidateRegion();
        }

        //loader

        if (this.vWorld == null || this.dimensionKey == null
         || config.seed != this.seed
         || this.dimensionKey != getDimensionKey(world)) {
            rebuildWorld(getDimensionKey(world), config.seed);
        }

        if (!config.active) {
            deactivate();
            return;
        }

        //pick up a region that finished generating
        if (this.regionFuture != null && this.regionFuture.isDone() && !this.regionConsumed) {
            this.regionConsumed = true;
            takeFinishedRegion();
        }

        //v chunk region
        ChunkPos playerPos = player.chunkPosition();
        // the request is keyed to where it was asked from, not to where the player was
        // when it was issued: a load that failed or that the player walked out of stays
        // stale until a fresh one actually finishes
        boolean stale = this.requestedPos == null
                || this.requestedPos.pack() != playerPos.pack()
                || this.requestedRadius != config.renderDistance;
        boolean idle = this.regionFuture == null || (this.regionFuture.isDone() && this.regionConsumed);

        if (this.retryCooldown > 0) {
            this.retryCooldown--;
        } else if (stale && idle) {
            this.regionFuture = this.loader.loadChunks(
                    playerPos, config.renderDistance, this.trackedBlocks, this.trackedVersion);
            this.regionConsumed = false;
            this.requestedPos = playerPos;
            this.requestedRadius = config.renderDistance;
        }

        //ore render data
        updateRenderData(player, config);

        //load status data
        this.regionLoadStatus = statusOf(stale);
    }

    /** Reads the finished region out of {@link #regionFuture}, or queues a retry. */
    private void takeFinishedRegion() {
        StaticCache2D<GenChunkHolder> region = null;
        if (!this.regionFuture.isCancelled() && !this.regionFuture.isCompletedExceptionally()) {
            region = this.regionFuture.join();
        }

        if (region == null) {
            LOGGER.warn("region generation failed [{}, {}], retrying",
                    this.requestedPos == null ? 0 : this.requestedPos.x(),
                    this.requestedPos == null ? 0 : this.requestedPos.z());
            // forget what was asked for, otherwise the position stays marked as loaded
            // and the ores never move again until the player toggles the mod off and on
            invalidateRegion();
            this.retryCooldown = FAILED_LOAD_RETRY_TICKS;
            return;
        }

        this.loadedRegion = region;
    }

    /**
     * Rebuilds the ore boxes, but only when something they depend on actually moved:
     * the region itself, the vertical slice around the player, or the fill opacity.
     */
    private void updateRenderData(LocalPlayer player, SeedXRayConfig config) {
        StaticCache2D<GenChunkHolder> region = this.loadedRegion;
        if (region == null) {
            return;
        }

        int vvd = config.verticalViewDistance;
        int sectionY = vvd == SeedXRayConfig.ConfigConstants.MIN_VERTICAL_VIEW_DISTANCE
                ? NO_SECTION_LIMIT
                : player.getBlockY() >> 4;
        int fillAlpha = fillAlpha(config);

        if (this.renderData != null
                && this.renderDataRegion == region
                && this.renderDataSectionY == sectionY
                && this.renderDataVerticalDistance == vvd
                && this.renderDataFillAlpha == fillAlpha) {
            return;
        }

        List<OreRenderBatch> renderData = new ArrayList<>(this.trackedBlocks.size());

        for (Block block : this.trackedBlocks) {
            LongCollection collection = new LongArrayList();

            region.forEach(holder -> {
                if (sectionY == NO_SECTION_LIMIT) {
                    collection.addAll(holder.getOreData(block));
                } else {
                    collection.addAll(holder.getOreData(block, sectionY - vvd, sectionY + vvd));
                }
            });

            if (collection.isEmpty()) {
                continue;
            }

            int rgb = this.trackedColors.getInt(block) & 0xFFFFFF;
            renderData.add(new OreRenderBatch(
                    collection,
                    (fillAlpha << 24) | rgb,
                    0xFF000000 | rgb));
        }

        this.renderData = renderData;
        this.renderDataRegion = region;
        this.renderDataSectionY = sectionY;
        this.renderDataVerticalDistance = vvd;
        this.renderDataFillAlpha = fillAlpha;
    }

    /** configured percentage turned into the alpha byte of the filled box */
    private static int fillAlpha(SeedXRayConfig config) {
        int percent = Math.max(SeedXRayConfig.ConfigConstants.MIN_FILL_OPACITY,
                Math.min(SeedXRayConfig.ConfigConstants.MAX_FILL_OPACITY, config.fillOpacity));
        return Math.round(percent * 255f / SeedXRayConfig.ConfigConstants.MAX_FILL_OPACITY);
    }

    private LoadStatus statusOf(boolean stale) {
        if (this.regionFuture == null) {
            return LoadStatus.IS_WORKING;
        }
        if (this.regionFuture.isCancelled()) {
            return LoadStatus.CANCELLED;
        }
        if (this.regionFuture.isCompletedExceptionally()) {
            return LoadStatus.COMPLETED_EXCEPTIONALLY;
        }
        // a region that finished for a chunk the player already left is not done
        return this.regionFuture.isDone() && !stale ? LoadStatus.DONE : LoadStatus.IS_WORKING;
    }

    /** Marks the loaded region as no longer matching what should be on screen. */
    private void invalidateRegion() {
        this.requestedPos = null;
        this.requestedRadius = -1;
    }

    private void deactivate() {
        if (this.regionLoadStatus == LoadStatus.LOAD_DISABLED) {
            return;
        }
        // the future is kept: a region that is still generating has to be waited out
        // before the next one starts, marking it consumed just throws its result away
        this.regionConsumed = true;
        this.retryCooldown = 0;
        this.loadedRegion = null;
        this.renderData = null;
        this.renderDataRegion = null;
        this.regionLoadStatus = LoadStatus.LOAD_DISABLED;
        invalidateRegion();
        if (this.loader != null) {
            this.loader.clearCache();
        }
    }

    /**
     * Turns the configured id list into the block set the scan runs against. The
     * version bumps only when the set of blocks changes, so recolouring an entry
     * does not throw away chunks that are already scanned.
     *
     * @return true when the set of blocks changed and the region has to be rescanned
     */
    private boolean rebuildTrackedBlocks() {
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

        boolean changed = !tracked.equals(this.trackedBlocks);
        if (changed) {
            this.trackedBlocks = tracked;
            this.trackedVersion++;
        }
        this.blockListHash = entries.hashCode();
        // a colour edit does not touch the scan, but it does change the boxes
        this.renderData = null;
        this.renderDataRegion = null;
        return changed;
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
        this.regionFuture = null;
        this.regionConsumed = true;
        this.retryCooldown = 0;

        this.dimensionKey = dimensionKey;
        this.seed = seed;
        this.vWorld = new VWorldService(
                SeedXRayClient.DRM,
                VUtils.createDimensionArg(dimensionKey, SeedXRayClient.DRM),
                seed);
        this.loader = new OreChunkLoader(this.vWorld, palettesFactory);

        // drop the old ores immediately - otherwise they keep being drawn until the
        // new region finishes generating
        this.loadedRegion = null;
        this.renderData = null;
        this.renderDataRegion = null;
        invalidateRegion();
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
