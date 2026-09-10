package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.interfaces.IXpBottleGrenadeMixin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public class XpBottleGrenadeHitFixMixin {

    @Inject(method="onHitEntity", at=@At("TAIL"))
    void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci)
    {
        Projectile projectile = (Projectile) (Object) this;
        if(projectile instanceof ThrownExperienceBottle experienceBottle) {
            Splatoon.LOGGER.info("Hit entity xp bottle");

            // fixes bug where hitting a shield doesnt trigger explosion of xp bomb hit
            ((IXpBottleGrenadeMixin)experienceBottle).explode(projectile.position());
        }
    }

    @Inject(method="onHitBlock", at=@At("TAIL"))
    void onHitBlock(BlockHitResult blockHitResult, CallbackInfo ci)
    {
        Projectile projectile = (Projectile) (Object) this;
        if(projectile instanceof ThrownExperienceBottle experienceBottle) {
            Splatoon.LOGGER.info("Hit block xp bottle");
        }
    }
}
