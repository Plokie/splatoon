package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

@Mixin(Silverfish.class)
public class InkPuckMixin implements IProjectile {
    UUID ownerUUID = null;
    Silverfish self = null;

    @Override
    public void setPlayerOwner(Player player) {
        ownerUUID = player.getUUID();
    }

    @Inject(method="<init>", at=@At("TAIL"))
    void init(EntityType entityType, Level level, CallbackInfo ci)
    {
        self = (Silverfish)(Object)this;
    }

    @Inject(method="registerGoals", at=@At("TAIL"))
    void register(CallbackInfo ci)
    {
        if(ownerUUID == null) return;

    }

    boolean firstTicked = false;

    @Inject(method="tick", at=@At("TAIL"))
    void tick(CallbackInfo ci)
    {
        if(self == null ){
            Splatoon.LOGGER.info("noself");
            return;
        }
        if(!self.getTags().contains("InkPuck")) {
            Splatoon.LOGGER.info("notag");
            return;
        }
        if(ownerUUID==null) {
            Splatoon.LOGGER.info("noowneruuid");
            return;
        }
        Player owner = self.level().getPlayerByUUID(ownerUUID);
        if(owner == null) {
            Splatoon.LOGGER.info("noowner");
            return;
        }

        if(!firstTicked) {
            self.setYRot(owner.getYRot());

            firstTicked = true;
        }

        Vec3 fwd = self.getForward();
        self.setDeltaMovement(fwd.multiply(2f,2f,2f));

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
