package com.plokie.mixin;

import com.plokie.Splatoon;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Chicken.class)
public class ChickenMixin {

    @Inject(method = "aiStep", at=@At("TAIL"))
    void tick(CallbackInfo ci) {
        Chicken self = (Chicken) (Object)this;

        if(self.isBaby()) {
            Splatoon.LOGGER.info("\tIs baby");
            self.discard();
        }
    }
}
