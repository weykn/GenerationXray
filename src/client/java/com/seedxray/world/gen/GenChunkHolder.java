package com.seedxray.world.gen;

import com.seedxray.ore.OreDataMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;

import java.util.Set;
import java.util.concurrent.Semaphore;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class GenChunkHolder implements SemaphoreHolder {
    /** no scan has run yet - {@link #scan} takes any version but this one */
    private static final int NOT_SCANNED = Integer.MIN_VALUE;

    private final ChunkAccess chunk;
    private final ChunkPos pos;
    private ChunkStatus status;

    private final Semaphore mutex = new Semaphore(1, true);

    /** section y -> blocks found in it, guarded by {@link #oreLock} */
    private final Int2ObjectMap<OreDataMap> oreSectionMap = new Int2ObjectOpenHashMap<>();
    private final Object oreLock = new Object();
    /** which tracked block set {@link #oreSectionMap} was built from */
    private int scannedVersion = NOT_SCANNED;

    public GenChunkHolder(ChunkPos pos, ChunkAccess chunk) {
        this.chunk = chunk;
        this.pos = pos;
        this.status = ChunkStatus.EMPTY;
    }

    /**
     * Walks the generated sections and records where the tracked blocks ended up.
     * Cheap to call repeatedly: a chunk that was already scanned for this block set
     * is left alone, so moving around only pays for the chunks that are new.
     */
    public void scan(Set<Block> tracked, int version) {
        synchronized (oreLock) {
            if (this.scannedVersion == version) {
                return;
            }
            this.oreSectionMap.clear();
            this.scannedVersion = version;

            if (tracked.isEmpty()) {
                return;
            }

            LevelHeightAccessor heightAccessor = this.chunk.getHeightAccessorForGeneration();
            LevelChunkSection[] sections = this.chunk.getSections();
            int minBlockX = this.pos.getMinBlockX();
            int minBlockZ = this.pos.getMinBlockZ();

            for (int index = 0; index < sections.length; index++) {
                LevelChunkSection section = sections[index];
                // the palette check skips the vast majority of sections outright
                if (section == null || section.hasOnlyAir()
                        || !section.maybeHas(state -> tracked.contains(state.getBlock()))) {
                    continue;
                }

                int sectionY = heightAccessor.getSectionYFromSectionIndex(index);
                int minBlockY = sectionY << 4;
                OreDataMap data = new OreDataMap();

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            Block block = section.getBlockState(x, y, z).getBlock();
                            if (tracked.contains(block)) {
                                data.put(block, BlockPos.asLong(minBlockX + x, minBlockY + y, minBlockZ + z));
                            }
                        }
                    }
                }

                if (!data.isEmpty()) {
                    this.oreSectionMap.put(sectionY, data);
                }
            }
        }
    }

    public LongCollection getOreData(Block block, int startSectionIdx, int endSectionIdx) {
        LongCollection collection = new LongArrayList();

        synchronized (oreLock) {
            for (int i = startSectionIdx; i <= endSectionIdx; i++) {
                OreDataMap data = oreSectionMap.get(i);
                if (data != null) {
                    collection.addAll(data.get(block));
                }
            }
        }

        return collection;
    }

    public LongCollection getOreData(Block block) {
        LongCollection collection = new LongArrayList();

        synchronized (oreLock) {
            oreSectionMap.values().forEach(oreDataMap -> {
                collection.addAll(oreDataMap.get(block));
            });
        }

        return collection;
    }

    public void setStatus(ChunkStatus status) {
        this.status = status;
    }

    public ChunkStatus getStatus() {
        return this.status;
    }

    public ChunkAccess getChunk() {
        return this.chunk;
    }

    public ChunkPos getPos() {
        return this.pos;
    }

    @Override
    public Semaphore getMutex() {
        return mutex;
    }
}
