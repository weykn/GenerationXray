package com.casiowatch123.vchunklib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.decoration.ItemFrame;

@Mixin(ItemFrame.class)
public class ItemFrameEntityMixin {
    @Redirect(
            method = "setItem(Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/decoration/ItemFrame;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"
            )
    )
    private void redirectPlaySound(ItemFrame instance,
                                   SoundEvent sound,
                                   float volume,
                                   float pitch) {
        if (instance.level() != null) {
            instance.playSound(sound, volume, pitch);
        }
    }
}
