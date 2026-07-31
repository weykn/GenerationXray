package com.casiowatch123.vchunklib.generation.virtual.world.chunk;

import com.casiowatch123.vchunklib.generation.virtual.world.VWorldContext;
import com.casiowatch123.vchunklib.generation.virtual.world.VWorldService;
import com.casiowatch123.vchunklib.generation.virtual.world.gen.VStructureAccessor;


import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;

public final class VChunkGenerating {
    private VChunkGenerating() {};

//    /*remove func list:
//        isLightOn
//        initializeLight
//        light
//        generateEntities
//        convertToFullChunk
//        addEntities
//     */
    static CompletableFuture<ChunkAccess> noop(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> generateStructures(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        VWorldContext worldContext = worldService.worldContext();

        VStructureAccessor structureAccessor = 
                new VStructureAccessor(new VChunkRegion(worldService, chunks, step, chunk), worldContext.getGeneratorOptions());
        
        if (worldContext.shouldGenerateStructures()) {
            context.generator()
                    .createStructures(
                            worldContext.getRegistryManager(),
                            worldContext.getStructurePlacementCalculator(),
                            structureAccessor,
                            chunk,
                            context.structureTemplateManager(), 
                            worldContext.getDimensionKey()
                    );
        }

        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> loadStructures(
            VChunkGenerationContext context, ChunkStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> generateStructureReferences(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor = 
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());
        
        context.generator().createReferences(chunkRegion, structureAccessor, chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> populateBiomes(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor =
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());

        return context.generator()
                .createBiomes(
                        worldService.worldContext().getNoiseConfig(), Blender.of(chunkRegion), structureAccessor, chunk
                );
    }

    static CompletableFuture<ChunkAccess> populateNoise(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor =
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());

        return context.generator()
                .fillFromNoise(
                        Blender.of(chunkRegion), worldService.worldContext().getNoiseConfig(), structureAccessor, chunk
                );
    }

    static CompletableFuture<ChunkAccess> buildSurface(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor =
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());

        context.generator()
                .buildSurface(chunkRegion, structureAccessor, worldService.worldContext().getNoiseConfig(), chunk);
        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> carve(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        VWorldService worldService = context.worldService();
        VWorldContext worldContext = worldService.worldContext();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor =
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());


        context.generator()
                .applyCarvers(
                        chunkRegion,
                        worldContext.getSeed(),
                        worldContext.getNoiseConfig(),
                        new BiomeManager(chunkRegion, BiomeManager.obfuscateSeed(worldContext.getSeed())),
                        structureAccessor,
                        chunk
                );
        return CompletableFuture.completedFuture(chunk);
    }

    static CompletableFuture<ChunkAccess> generateFeatures(
            VChunkGenerationContext context, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks, ChunkAccess chunk
    ) {
        Heightmap.primeHeightmaps(
                chunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE)
        );
        VWorldService worldService = context.worldService();
        VWorldContext worldContext = worldService.worldContext();
        WorldGenRegion chunkRegion = new VChunkRegion(worldService, chunks, step, chunk);
        VStructureAccessor structureAccessor =
                new VStructureAccessor(chunkRegion, worldService.worldContext().getGeneratorOptions());

        context.generator().applyBiomeDecoration(chunkRegion, chunk, structureAccessor);
        
        return CompletableFuture.completedFuture(chunk);
    }
}
