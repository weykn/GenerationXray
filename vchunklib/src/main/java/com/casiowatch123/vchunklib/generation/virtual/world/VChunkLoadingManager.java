package com.casiowatch123.vchunklib.generation.virtual.world;

import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationContext;
import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationStep;
import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationSteps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class VChunkLoadingManager {
    private static final ChunkStatus START_STATUS = ChunkStatus.EMPTY;
    private static final ChunkStatus FINAL_STATUS = ChunkStatus.FEATURES;
    private static final List<Integer> GENERATE_RADIUS_LIST = List.of(10, 10, 2, 2, 1, 1, 1, 0);
    private static final List<ChunkStatus> CHUNK_STATUS_LIST = ChunkStatus.getStatusList()
            .subList(START_STATUS.getIndex(), FINAL_STATUS.getIndex() + 1);
            
    public static final int MAX_DEPENDENT_REGION_RADIUS = 10;
    private final VWorldService world;
    private final VChunkGenerationContext generationContext;
    
    private final PalettedContainerFactory palettesFactory;
    
    public VChunkLoadingManager(VWorldService world) {
        this.world = world;
        this.generationContext = world.getGenerationContext();
        this.palettesFactory = PalettedContainerFactory.create(world.worldContext().getRegistryManager());
    }
    
    //return custom chunk type 
    public StaticCache2D<ChunkAccess> loadChunk(ChunkPos centerPos, int centerRadius) {
        if (centerRadius < 0) {
            throw new IllegalArgumentException("center radius is wrong: " + centerRadius);
        }
        
        int size = 2 * (MAX_DEPENDENT_REGION_RADIUS + centerRadius) + 1;
        int radius = size/2;
        
        
        StaticCache2D<ChunkAccess> chunks = this.generateRegion(centerPos, centerRadius);
        
        for(ChunkStatus status : CHUNK_STATUS_LIST) {
            int currentGeneratingSize = 2 * (GENERATE_RADIUS_LIST.get(status.getIndex()) + centerRadius) + 1;
            int blank = (size - currentGeneratingSize) / 2;
//            System.out.println("current status: " + status + "(" + GENERATE_RADIUS_LIST.get(status.getIndex()) + ")");
//            System.out.println("blank: " + blank);
//            System.out.println("currentGeneratingSize: " + currentGeneratingSize);

            VChunkGenerationStep step = VChunkGenerationSteps.GENERATION.get(status);
            List<CompletableFuture<ChunkAccess>> futures = new ArrayList<>();
            for(int j = blank; j < size - blank; j++) {
                for(int k = blank; k < size - blank; k++) {
                    int x = centerPos.x() - radius + k;
                    int z = centerPos.z() - radius + j;
                    futures.add(generate(chunks.get(x, z), step, chunks));
                }
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }

        return chunks;
    }
    
    //generate VChunkHolder chunks;
    private StaticCache2D<ChunkAccess> generateRegion(ChunkPos centerPos, int centerRadius) {
        int size = 2 * (MAX_DEPENDENT_REGION_RADIUS + centerRadius) + 1;
        int radius = size/2;

        ChunkAccess[][] chunkHolders = new ChunkAccess[size][size];
        
        for(int i = 0; i < size ; i++) {
            for(int j = 0; j < size ; j++) {
                ChunkPos pos = new ChunkPos(centerPos.x() - radius + i, centerPos.z() - radius + j);
                chunkHolders[i][j] = generateProtoChunk(pos);
            }
        }
        
        return StaticCache2D.create(centerPos.x(), centerPos.z(), radius, 
                (chunkX, chunkZ) -> chunkHolders[chunkX - centerPos.x() + radius][chunkZ - centerPos.z() + radius]
        );
    }
    
    
    private CompletableFuture<ChunkAccess> generate(ChunkAccess chunk, VChunkGenerationStep step, StaticCache2D<ChunkAccess> chunks) {
        return step.run(this.generationContext, chunks, chunk);
    }
    
    private ProtoChunk generateProtoChunk(ChunkPos chunkPos) {
        return new ProtoChunk(
                chunkPos, 
                UpgradeData.EMPTY, 
                this.world.world(), 
                palettesFactory, 
                null
        );
    }
}
