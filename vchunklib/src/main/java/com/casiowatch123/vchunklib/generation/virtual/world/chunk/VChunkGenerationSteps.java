package com.casiowatch123.vchunklib.generation.virtual.world.chunk;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public record VChunkGenerationSteps(ImmutableList<VChunkGenerationStep> steps) {
    public static final VChunkGenerationSteps GENERATION = new VChunkGenerationSteps.Builder()
            .then(ChunkStatus.EMPTY, builder -> builder)
            .then(ChunkStatus.STRUCTURE_STARTS, builder -> builder.task(VChunkGenerating::generateStructures))
            .then(ChunkStatus.STRUCTURE_REFERENCES, builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8).task(VChunkGenerating::generateStructureReferences))
            .then(ChunkStatus.BIOMES, builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8).task(VChunkGenerating::populateBiomes))
            .then(
                    ChunkStatus.NOISE,
                    builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8).dependsOn(ChunkStatus.BIOMES, 1).blockStateWriteRadius(0).task(VChunkGenerating::populateNoise)
            )
            .then(
                    ChunkStatus.SURFACE,
                    builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8).dependsOn(ChunkStatus.BIOMES, 1).blockStateWriteRadius(0).task(VChunkGenerating::buildSurface)
            )
            .then(ChunkStatus.CARVERS, builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8).blockStateWriteRadius(0).task(VChunkGenerating::carve))
            .then(
                    ChunkStatus.FEATURES,
                    builder -> builder.dependsOn(ChunkStatus.STRUCTURE_STARTS, 8)
                            .dependsOn(ChunkStatus.CARVERS, 1)
                            .blockStateWriteRadius(1)
                            .task(VChunkGenerating::generateFeatures)
            )
            .build();
    
    public VChunkGenerationStep get(ChunkStatus status) {
        return this.steps.get(status.getIndex());
    }

    public static class Builder {
        private final List<VChunkGenerationStep> steps = new ArrayList<>();

        public VChunkGenerationSteps build() {
            return new VChunkGenerationSteps(ImmutableList.copyOf(this.steps));
        }

        public VChunkGenerationSteps.Builder then(ChunkStatus status, UnaryOperator<VChunkGenerationStep.Builder> stepFactory) {
            VChunkGenerationStep.Builder builder;
            if (this.steps.isEmpty()) {
                builder = new VChunkGenerationStep.Builder(status);
            } else {
                builder = new VChunkGenerationStep.Builder(status, (VChunkGenerationStep)this.steps.getLast());
            }

            this.steps.add(((VChunkGenerationStep.Builder)stepFactory.apply(builder)).build());
            return this;
        }
    }
}
