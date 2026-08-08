package com.casiowatch123.vchunklib.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelData;

@Mixin(WorldGenRegion.class)
public abstract class ChunkRegionMixin {
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/server/level/ServerLevel;getSeed()J"))
    private long getSeed(ServerLevel world) {
        return world == null ? 0L : world.getSeed();
    }
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/server/level/ServerLevel;getLevelData()Lnet/minecraft/world/level/storage/LevelData;"))
    private LevelData getLevelProperties(ServerLevel world) {
        return world == null ? null : world.getLevelData();
    }

    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;dimensionType()Lnet/minecraft/world/level/dimension/DimensionType;"))
    private DimensionType getDimension(ServerLevel world) {
        return world == null ? null : world.dimensionType();
    }
    
    
    
    
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/server/level/ServerLevel;getChunkSource()Lnet/minecraft/server/level/ServerChunkCache;"))
    private ServerChunkCache getChunkManager(ServerLevel world) {
        return world == null ? null : world.getChunkSource();
    }
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerChunkCache;randomState()Lnet/minecraft/world/level/levelgen/RandomState;"))
    private RandomState getNoiseConfig(ServerChunkCache manager) {
        return manager == null ? null : manager.randomState();
    }
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/world/level/levelgen/RandomState;getOrCreateRandomFactory(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;"))
    private PositionalRandomFactory getOrCreateRandomDeriver(RandomState noiseConfig, Identifier id) {
        return noiseConfig == null ? null : noiseConfig.getOrCreateRandomFactory(id);
    }
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/util/RandomSource;"))
    private RandomSource split(PositionalRandomFactory splitter, BlockPos pos) {
        return splitter == null ? null : splitter.at(pos);
    }
    
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()Lnet/minecraft/world/level/ChunkPos;"))
    private ChunkPos getPos(ChunkAccess chunk) {
        return chunk == null ? null : chunk.getPos();
    }
    
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ChunkPos;getWorldPosition()Lnet/minecraft/core/BlockPos;"
    ))
    private BlockPos getStartPos(ChunkPos chunkPos) {
        return chunkPos == null ? null : chunkPos.getWorldPosition();
    }

    // 26.2 caches the center chunk coords and the write radius in the constructor.
    // They only feed the write-zone checks, which VChunkRegion overrides wholesale,
    // so a virtual region can leave them at zero.
    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ChunkPos;x()I"))
    private int getCenterChunkX(ChunkPos chunkPos) {
        return chunkPos == null ? 0 : chunkPos.x();
    }

    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/ChunkPos;z()I"))
    private int getCenterChunkZ(ChunkPos chunkPos) {
        return chunkPos == null ? 0 : chunkPos.z();
    }

    @Redirect(method = "<init>", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/status/ChunkStep;blockStateWriteRadius()I"))
    private int getWriteRadius(ChunkStep generationStep) {
        return generationStep == null ? 0 : generationStep.blockStateWriteRadius();
    }
}
