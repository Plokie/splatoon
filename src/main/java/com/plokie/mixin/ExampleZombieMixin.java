package com.plokie.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.monster.Zombie;

import java.util.Objects;

@Mixin(Zombie.class)
public abstract class ExampleZombieMixin {
    @Unique Zombie zombie;

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void onRegister(CallbackInfo ci) {
        zombie = (Zombie)(Object)this;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {

        if(zombie.level().isClientSide()) return;

        if(!zombie.getTags().contains("mytag")) return;

        if(zombie.tickCount % 20 == 0) {
            zombie.level().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("hello"), false
            );
        }
    }
}