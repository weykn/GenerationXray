package com.casiowatch123.vchunklib.mixin;

import com.casiowatch123.vchunklib.generation.virtual.VUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.feature.FossilFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

@Mixin(FossilFeature.class)
public class FossilFeatureMixin {
    @Redirect(
            method = "place", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lnet/minecraft/server/level/ServerLevel;getServer()Lnet/minecraft/server/MinecraftServer;"
            )
    )
    private MinecraftServer redirectedGetServer(ServerLevel instance) {
        if (instance == null) {
            return null;
        }
        return instance.getServer();
    }
    
    @Redirect(
            method = "place", 
            at = @At(
                    value = "INVOKE", 
                    target = "Lnet/minecraft/server/MinecraftServer;getStructureManager()Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;"
            )
    )
    private StructureTemplateManager redirectedGetStructureTemplateManager(MinecraftServer instance) {
        if (instance == null) {
            return VUtils.STRUCTURE_TEMPLATE_MANAGER;
        }
        return instance.getStructureManager();
    }
}
