package com.casiowatch123.vchunklib.mixin;

import com.casiowatch123.vchunklib.generation.virtual.world.chunk.VChunkRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.CappedProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

@Mixin(CappedProcessor.class)
public class CappedStructureProcessorMixin {
    
    @Redirect(
            method = "finalizeProcessing(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Ljava/util/List;Ljava/util/List;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;)Ljava/util/List;", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lnet/minecraft/server/level/ServerLevel;getSeed()J"
            )
    )
    private long redirectedGetSeed(
            ServerLevel instance, 
            ServerLevelAccessor world) {
        if (world instanceof VChunkRegion vChunkRegion) {
            return vChunkRegion.getSeed();
        }
        return instance.getSeed();
    }
}
