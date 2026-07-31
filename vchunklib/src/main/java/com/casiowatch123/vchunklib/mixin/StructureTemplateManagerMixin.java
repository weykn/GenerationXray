package com.casiowatch123.vchunklib.mixin;

import com.mojang.datafixers.DataFixer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.core.HolderGetter;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;

@Mixin(StructureTemplateManager.class)
public abstract class StructureTemplateManagerMixin {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;getLevelPath(Lnet/minecraft/world/level/storage/LevelResource;)Ljava/nio/file/Path;"
            )
    )
    private Path redirectGetDirectory(LevelStorageSource.LevelStorageAccess session, LevelResource savePath) {
        if (session == null) {
            return Paths.get(".").toAbsolutePath();
        }
        return session.getLevelPath(savePath);
    }
}