package com.plokie.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


// https://github.com/ForwarD-NerN/PlayerLadder/blob/Fabric-1.21.8/src/main/java/ru/nern/playerladder/mixin/shared/EntityMixin.java

@Mixin(Entity.class)
public class RideablePlayerMixin {
    void onDismount(Entity vehicle)
    {
        if(!vehicle.level().isClientSide && vehicle instanceof Player)
            ((ServerPlayer) vehicle).connection.send(new ClientboundSetPassengersPacket(vehicle));
    }

    @Inject(method = "removePassenger", at = @At("TAIL"))
    private void removePassenger(Entity passenger, CallbackInfo ci) {
        onDismount((Entity) (Object) this);
    }

    void onMount(Entity vehicle, Entity passenger)
    {
        if(!vehicle.level().isClientSide && vehicle instanceof Player) {
            ((ServerPlayer)vehicle).connection.send(new ClientboundSetPassengersPacket(vehicle));
        }
    }

    @Inject(method = "addPassenger", at = @At("TAIL"))
    private void addPassenger(Entity passenger, CallbackInfo ci) {
        onMount((Entity) (Object) this, passenger);
    }

    @WrapOperation(
            method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z")
    )
    boolean ride(EntityType instance, Operation<Boolean> original)
    {
        if(instance == EntityType.PLAYER) {
            return true;
        }else{
            return original.call(instance);
        }
    }
}
