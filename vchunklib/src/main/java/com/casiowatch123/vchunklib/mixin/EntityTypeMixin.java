package com.casiowatch123.vchunklib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

@Mixin(EntityType.class)
public class EntityTypeMixin {
    
    @Inject(
            method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;", 
            at = @At("HEAD"), 
            cancellable = true)
    private void create(Level world, EntitySpawnReason reason, CallbackInfoReturnable<Entity> cir) {
        if (world == null) {
            cir.setReturnValue(null);
        }
    }
}
