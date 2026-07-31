package com.casiowatch123.vchunklib.mixin;

import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkRegion;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.treedecorators.PaleMossDecorator;


@Mixin(PaleMossDecorator.class)
public class PaleMossTreeDecoratorMixin {
    @Redirect(
            method = "lambda$place$1(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder$Reference;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getChunkSource()Lnet/minecraft/server/level/ServerChunkCache;"
            )
    )
    private static ServerChunkCache method(ServerLevel instance) {
        if (instance == null) {
            return null;
        }
        return instance.getChunkSource();
    }

    @Redirect(
            method = "lambda$place$1(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Holder$Reference;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerChunkCache;getGenerator()Lnet/minecraft/world/level/chunk/ChunkGenerator;"
            )
    )
    private static ChunkGenerator method(
            ServerChunkCache instance, 
            WorldGenLevel structureWorldAccess) {
        if (instance == null
                && structureWorldAccess instanceof VChunkRegion region) {
            return region.getWorldService().worldContext().getGenerator();
        }
        return instance.getGenerator();
    }
}
