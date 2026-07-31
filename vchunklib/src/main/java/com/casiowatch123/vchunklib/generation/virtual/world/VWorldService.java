package com.casiowatch123.vchunklib.generation.virtual.world;

import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkGenerationContext;
import net.minecraft.core.RegistryAccess;

public class VWorldService {
    private final VWorld world;
    private final VWorldContext worldContext;
    private final VChunkGenerationContext generationContext;
    
    public VWorldService(RegistryAccess registryManager, VDimensionArgs dimensionArgs, long seed) {
        this.worldContext = new VWorldContext(registryManager, dimensionArgs, seed);
        this.world = new VWorld(dimensionArgs.dimensionType());
        this.generationContext = new VChunkGenerationContext(
                this, 
                worldContext.getGenerator(), 
                dimensionArgs.structureTemplateManager());
    }
    
    public VWorld world() {
        return world;
    }
    
    public VWorldContext worldContext() {
        return worldContext;
    }
    
    public VChunkGenerationContext getGenerationContext() {
        return generationContext;
    }
}
