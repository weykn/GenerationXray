package com.casiowatch123.vchunklib.generation.virtual.world.chunk;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public record VChunkGenerationStep(
        ChunkStatus targetStatus,
        ChunkDependencies directDependencies,
        ChunkDependencies accumulatedDependencies,
        int blockStateWriteRadius,
        VGenerationTask task
) {

    public int getRadiusOf(ChunkStatus status) {
        return status == this.targetStatus ? 0 : this.accumulatedDependencies.getRadiusOf(status);
    }

    public CompletableFuture<ChunkAccess> run(VChunkGenerationContext context, StaticCache2D<ChunkAccess> boundedRegionArray, ChunkAccess chunk) {
        if (chunk.getPersistedStatus().isBefore(this.targetStatus)) {
            return this.task.doWork(context, this, boundedRegionArray, chunk).thenApply(this::finalizeGeneration);
        } else {
            return this.task.doWork(context, this, boundedRegionArray, chunk);
        }
    }

    private ChunkAccess finalizeGeneration(ChunkAccess chunk) {
        if (chunk instanceof ProtoChunk protoChunk && protoChunk.getPersistedStatus().isBefore(this.targetStatus)) {
            protoChunk.setPersistedStatus(this.targetStatus);
        }

        return chunk;
    }

    public static class Builder {
        private final ChunkStatus targetStatus;
        @Nullable
        private final VChunkGenerationStep previousStep;
        private ChunkStatus[] directDependencies;
        private int blockStateWriteRadius = -1;
        private VGenerationTask task = VChunkGenerating::noop;

        protected Builder(ChunkStatus targetStatus) {
            if (targetStatus.getParent() != targetStatus) {
                throw new IllegalArgumentException("Not starting with the first status: " + targetStatus);
            } else {
                this.targetStatus = targetStatus;
                this.previousStep = null;
                this.directDependencies = new ChunkStatus[0];
            }
        }

        protected Builder(ChunkStatus blockStateWriteRadius, VChunkGenerationStep previousStep) {
            if (previousStep.targetStatus.getIndex() != blockStateWriteRadius.getIndex() - 1) {
                throw new IllegalArgumentException("Out of order status: " + blockStateWriteRadius);
            } else {
                this.targetStatus = blockStateWriteRadius;
                this.previousStep = previousStep;
                this.directDependencies = new ChunkStatus[]{previousStep.targetStatus};
            }
        }

        public VChunkGenerationStep.Builder dependsOn(ChunkStatus status, int level) {
            if (status.isOrAfter(this.targetStatus)) {
                throw new IllegalArgumentException("Status " + status + " can not be required by " + this.targetStatus);
            } else {
                ChunkStatus[] chunkStatuss = this.directDependencies;
                int i = level + 1;
                if (i > chunkStatuss.length) {
                    this.directDependencies = new ChunkStatus[i];
                    Arrays.fill(this.directDependencies, status);
                }

                for (int j = 0; j < Math.min(i, chunkStatuss.length); j++) {
                    this.directDependencies[j] = ChunkStatus.max(chunkStatuss[j], status);
                }

                return this;
            }
        }

        public VChunkGenerationStep.Builder blockStateWriteRadius(int blockStateWriteRadius) {
            this.blockStateWriteRadius = blockStateWriteRadius;
            return this;
        }

        public VChunkGenerationStep.Builder task(VGenerationTask task) {
            this.task = task;
            return this;
        }

        public VChunkGenerationStep build() {
            return new VChunkGenerationStep(
                    this.targetStatus,
                    new ChunkDependencies(ImmutableList.copyOf(this.directDependencies)),
                    new ChunkDependencies(ImmutableList.copyOf(this.accumulateDependencies())),
                    this.blockStateWriteRadius,
                    this.task
            );
        }

        private ChunkStatus[] accumulateDependencies() {
            if (this.previousStep == null) {
                return this.directDependencies;
            } else {
                int i = this.getParentStatus(this.previousStep.targetStatus);
                ChunkDependencies generationDependencies = this.previousStep.accumulatedDependencies;
                ChunkStatus[] chunkStatuss = new ChunkStatus[Math.max(i + generationDependencies.size(), this.directDependencies.length)];

                for (int j = 0; j < chunkStatuss.length; j++) {
                    int k = j - i;
                    if (k < 0 || k >= generationDependencies.size()) {
                        chunkStatuss[j] = this.directDependencies[j];
                    } else if (j >= this.directDependencies.length) {
                        chunkStatuss[j] = generationDependencies.get(k);
                    } else {
                        chunkStatuss[j] = ChunkStatus.max(this.directDependencies[j], generationDependencies.get(k));
                    }
                }

                return chunkStatuss;
            }
        }

        private int getParentStatus(ChunkStatus status) {
            for (int i = this.directDependencies.length - 1; i >= 0; i--) {
                if (this.directDependencies[i].isOrAfter(status)) {
                    return i;
                }
            }

            return 0;
        }
    }
}
