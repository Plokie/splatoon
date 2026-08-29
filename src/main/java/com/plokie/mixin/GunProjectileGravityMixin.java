package com.plokie.mixin;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ThrowableProjectile.class)
public class GunProjectileGravityMixin {

    @Inject(method="tick", at=@At("HEAD"))
    void tick(CallbackInfo ci)
    {
        ThrowableProjectile selfProj = (ThrowableProjectile)(Object)this;

        if(!(selfProj instanceof Snowball self)) return;

        Vec3 vel = self.getDeltaMovement();
        vel = new Vec3(vel.x, vel.y - 0.1f, vel.z);

        self.setDeltaMovement(vel);

        var packet = ClientboundTeleportEntityPacket.teleport(
                self.getId(),
                new PositionMoveRotation(self.position(), self.getDeltaMovement(), self.getYRot(), self.getXRot()),
                Set.of(),
                self.onGround()
        );

        for (ServerPlayer player : PlayerLookup.tracking(self)) {
            player.connection.send(packet);
        }

    }
}
