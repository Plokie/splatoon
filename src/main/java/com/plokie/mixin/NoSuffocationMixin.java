package com.plokie.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class NoSuffocationMixin {
    @Inject(method="isInWall", at= @At(value = "HEAD"), cancellable = true)
    void isInWall(CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(false);
    }
}
