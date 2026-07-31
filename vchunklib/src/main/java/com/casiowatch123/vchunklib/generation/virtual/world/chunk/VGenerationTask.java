package com.casiowatch123.vchunklib.generation.virtual.world.chunk;


import java.util.concurrent.CompletableFuture;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface VGenerationTask {
    CompletableFuture<ChunkAccess> doWork(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> boundedRegionArray, ChunkAccess chunk
    );
}