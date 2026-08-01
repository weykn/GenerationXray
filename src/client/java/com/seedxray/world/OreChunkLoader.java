package com.seedxray.world;

import com.casiowatch123.vchunklib.generation.virtual.world.VWorldService;
import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationContext;
import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationStep;
import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationSteps;
import com.seedxray.SeedXRayClient;
import com.seedxray.world.gen.*;
import it.unimi.dsi.fastutil.longs.*;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class OreChunkLoader {
    private static final Logger LOGGER = SeedXRayClient.LOGGER;
    
    private static final int MAX_DEPENDENT_REGION_RADIUS = 10;
    private static final ChunkStatus START_STATUS = ChunkStatus.EMPTY;
    private static final ChunkStatus FINAL_STATUS = ChunkStatus.FEATURES;
    private static final List<Integer> GENERATE_RADIUS_LIST = List.of(10, 10, 2, 2, 1, 1, 1, 0);
    private static final List<ChunkStatus> CHUNK_STATUS_LIST = ChunkStatus.getStatusList()
            .subList(START_STATUS.getIndex(), FINAL_STATUS.getIndex() + 1);
    
    /** keeps two regions from running the heavy stages against each other */
    private static final ReentrantLock lock = new ReentrantLock();

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    
    private volatile StaticCache2D<GenChunkHolder> cache;
    /**
     * Bumped by {@link #clearCache()}. A load that is already running captures it and
     * only stores its chunks if it still matches, so dropping the cache cannot be
     * undone a moment later by whatever was in flight at the time.
     */
    private volatile int cacheEpoch;
    /** set when this loader's world is replaced (seed or dimension change) */
    private volatile boolean cancelled;

    private final VWorldService world;
    private final VChunkGenerationContext generationContext;
    private final PalettedContainerFactory palettesFactory;
    
    public OreChunkLoader(VWorldService world, PalettedContainerFactory palettesFactory){
        this.world = world;
        this.generationContext = world.getGenerationContext();
        this.palettesFactory = palettesFactory;
    }

    /**
     * @param tracked        blocks to look for once the region is generated
     * @param trackedVersion identity of that block set, chunks already scanned for it are reused
     */
    public CompletableFuture<StaticCache2D<GenChunkHolder>> loadChunks(
            ChunkPos centerPos, int centerRadius, Set<Block> tracked, int trackedVersion) {
        if (centerRadius < 0) {
            throw new IllegalArgumentException("center radius is wrong: " + centerRadius);
        }
        if (this.cancelled) {
            return CompletableFuture.completedFuture(null);
        }

        // placing a chunk's features also writes into its neighbours, so a chunk only
        // holds every ore it is ever going to hold once the ring around it has run the
        // final step too - that ring is generated and then left out of the result
        int featureMargin = VChunkGenerationSteps.GENERATION.get(FINAL_STATUS).blockStateWriteRadius();
        int generationRadius = centerRadius + featureMargin;
        int radius = generationRadius + MAX_DEPENDENT_REGION_RADIUS;
        int size = 2*radius + 1;

        // read on the caller's thread, so clearing the cache cannot slip in between
        int epoch = this.cacheEpoch;
        StaticCache2D<GenChunkHolder> cacheSnapshot = this.cache;

        try {
            return CompletableFuture.supplyAsync(() -> {
                        StaticCache2D<GenChunkHolder> genChunkHolderArray = generateRegion(centerPos, radius, cacheSnapshot);
                        StaticCache2D<ChunkAccess> chunks = StaticCache2D.create(
                                centerPos.x(), centerPos.z(), radius,
                                (x, z) -> genChunkHolderArray.get(x, z).getChunk());

                        for (ChunkStatus status : CHUNK_STATUS_LIST) {
                            if (this.cancelled) {
                                return null;
                            }
                            int currentGeneratingSize = 2 * (GENERATE_RADIUS_LIST.get(status.getIndex()) + generationRadius) + 1;
                            int blank = (size - currentGeneratingSize) / 2;

                            VChunkGenerationStep step = VChunkGenerationSteps.GENERATION.get(status);

                            boolean lockFlag = false;
                            if (status == ChunkStatus.FEATURES || status == ChunkStatus.NOISE) {
                                lock.lock();
                                lockFlag = true;
                            }
                            try {
                                int total = (size - 2 * blank) * (size - 2 * blank);
                                if (total <= 0) {
                                    continue;
                                }
                                AtomicInteger remaining = new AtomicInteger(total);
                                CompletableFuture<Void> done = new CompletableFuture<>();

                                for (int j = blank; j < size - blank; j++) {
                                    for (int k = blank; k < size - blank; k++) {
                                        int x = centerPos.x() - radius + k;
                                        int z = centerPos.z() - radius + j;
                                        generate(genChunkHolderArray.get(x, z), step, chunks, genChunkHolderArray)
                                                .whenComplete((chunk, ex) -> {
                                                    if (ex != null) {
                                                        done.completeExceptionally(ex);
                                                    }
                                                    if (remaining.decrementAndGet() == 0) {
                                                        done.complete(null);
                                                    }
                                                });
                                    }
                                }

                                done.join();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } finally {
                                if (lockFlag) {
                                    lock.unlock();
                                }
                            }
                        }

                        if (this.cacheEpoch == epoch) {
                            this.cache = genChunkHolderArray;
                        }

                        StaticCache2D<GenChunkHolder> region = StaticCache2D.create(
                                centerPos.x(), centerPos.z(), centerRadius,
                                genChunkHolderArray::get);

                        // the finished chunks are the source of truth: reading the blocks back
                        // out of them picks up anything generation put there, whatever placed it.
                        // every chunk in here is done being written to, so a chunk that a previous
                        // region already scanned can be taken as is
                        region.forEach(holder -> holder.scan(tracked, trackedVersion));

                        return region;
                    }, EXECUTOR)
                    .exceptionally(ex -> {
                        LOGGER.warn("Region generation failed: [{}, {}]", centerPos.x(), centerPos.z());
                        return null;
                    });
        } catch (RejectedExecutionException e) {
            // half generated chunks would only show a partial, unstable set of ores
            LOGGER.warn("Region generation rejected: [{}, {}]", centerPos.x(), centerPos.z());
            return CompletableFuture.completedFuture(null);
        }
    }

    private StaticCache2D<GenChunkHolder> generateRegion(
            ChunkPos centerPos, int radius, 
            StaticCache2D<GenChunkHolder> chunkHolderCache) {
        int size = 2*radius + 1;
        int startX = centerPos.x() - radius;
        int startZ = centerPos.z() - radius;

        Long2ObjectMap<GenChunkHolder> chunks = new Long2ObjectOpenHashMap<>();
        
        for(int i = 0; i < size; i++) {
            for(int j = 0; j < size; j++) {
                int xPos = i + startX;
                int zPos = j + startZ;
                
                if (chunkHolderCache == null || !chunkHolderCache.contains(xPos, zPos)) {
                    ChunkPos pos = new ChunkPos(i + startX, j + startZ);
                    chunks.put(pos.pack(), new GenChunkHolder(pos, generateProtoChunk(pos)));
                }
            }
        }
        
        return StaticCache2D.create(
                centerPos.x(), centerPos.z(), radius,
                (x, z) -> {
                    if (chunkHolderCache != null && chunkHolderCache.contains(x, z)) {
                        return chunkHolderCache.get(x, z);
                    }
                    return chunks.get(ChunkPos.pack(x, z));
                });
    }
    
    private CompletableFuture<ChunkAccess> generate(
            GenChunkHolder chunkHolder, 
            VChunkGenerationStep step, 
            StaticCache2D<ChunkAccess> chunks, 
            StaticCache2D<? extends SemaphoreHolder> lockableChunkArray) throws InterruptedException {
        long[] ordinalLocks = lockChunks(chunkHolder.getPos(), step.blockStateWriteRadius(), lockableChunkArray);
        
        if (chunkHolder.getStatus().isBefore(step.targetStatus())) {
            try {
                return step.run(this.generationContext, chunks, chunkHolder.getChunk())
                        .whenComplete((chunk, ex) -> unlockChunks(chunkHolder.getPos(), step.blockStateWriteRadius(), lockableChunkArray, ordinalLocks))
                        .thenApply(chunk -> {
                            chunkHolder.setStatus(step.targetStatus());
                            return chunk;
                        });
            } catch (Throwable ex) {
                unlockChunks(chunkHolder.getPos(), step.blockStateWriteRadius(), lockableChunkArray, ordinalLocks);
                throw ex;
            }
        }

        unlockChunks(chunkHolder.getPos(), step.blockStateWriteRadius(), lockableChunkArray, ordinalLocks);
        return CompletableFuture.completedFuture(chunkHolder.getChunk());
    }
    
    private static long[] lockChunks(ChunkPos centerPos, int radius, StaticCache2D<? extends SemaphoreHolder> lockableChunks) throws InterruptedException {
        int startX = centerPos.x() - radius;
        int startZ = centerPos.z() - radius;
        int size = 2 * radius + 1;
        
        LongList list = new LongArrayList();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                list.add(ChunkPos.pack(startX + i, startZ + j));
            }
        }
        long[] locks = list.longStream()
                .sorted()
                .toArray();
        
        for(long l : locks) {
            lockableChunks.get(ChunkPos.getX(l), ChunkPos.getZ(l))
                    .getMutex()
                    .acquire();
        }
        return locks;
    }

    private static void unlockChunks(ChunkPos centerPos, int radius, StaticCache2D<? extends SemaphoreHolder> lockableChunks, long[] locks) {
        for(long l : LongArrays.reverse(locks)) {
            lockableChunks.get(ChunkPos.getX(l), ChunkPos.getZ(l))
                    .getMutex()
                    .release();
        }
    }

    private ProtoChunk generateProtoChunk(ChunkPos chunkPos) {
        return new ProtoChunk(
                chunkPos, 
                UpgradeData.EMPTY, 
                this.world.world(), 
                this.palettesFactory, 
                null);
    }
    
    public static void shutdown() {
        EXECUTOR.shutdown();
    }
    
    public void clearCache() {
        this.cacheEpoch++;
        this.cache = null;
    }

    /**
     * Abandons this loader. Generation already queued on the shared pool stops at the
     * next chunk status instead of finishing work for a world that no longer exists.
     */
    public void cancel() {
        this.cancelled = true;
        clearCache();
    }
}
