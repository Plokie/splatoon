package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(Display.class)
public class InkPuckMixin implements IProjectile {
    @Unique UUID ownerUUID = null;
    @Unique Display self = null;

    @Unique float speed = 0.5f;

    @Override
    public void setPlayerOwner(Player player) {
        ownerUUID = player.getUUID();
    }

    @Inject(method="<init>", at=@At("TAIL"))
    void init(EntityType entityType, Level level, CallbackInfo ci)
    {
        self = (Display)(Object)this;
    }

    @Inject(method="tick", at=@At("TAIL"))
    void tick(CallbackInfo ci)
    {
        if(self == null ){
            return;
        }
        if(!self.getTags().contains("InkPuck")) {
            return;
        }
        if(ownerUUID==null) {
            Splatoon.LOGGER.warn("Puck with no owner UUID");
            self.discard();
            return;
        }
        Player owner = self.level().getPlayerByUUID(ownerUUID);
        if(owner == null) {
            Splatoon.LOGGER.warn("Puck with no owner but with owner UUID");
            self.discard();
            return;
        }

        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(owner);
        if(team == null) return;


        Vec3 fwd = self.getForward();
        Vec3 move = fwd.multiply(speed, speed, speed);

        { // snap to ground
            Vec3 from = self.getPosition(0.0f).add(0, 1.5, 0);
            from = from.add(fwd.multiply(speed, speed, speed));

            Vec3 to = self.getPosition(0.0f).add(0, -3, 0);
            to = to.add(fwd.multiply(speed, speed, speed));

            BlockHitResult hit = self.level().clip(new ClipContext(
                    from, to,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
                    )
            );

            if(hit.getType() == HitResult.Type.BLOCK)
            {
                Vec3 hitPoint = hit.getLocation();

                double dist = from.distanceTo(hitPoint);
                if(dist < 0.01f) {
                    HitResult surfaceNormalRaycast = self.pick(10.0, 0.0f, false);
                    if(surfaceNormalRaycast.getType() == HitResult.Type.BLOCK) {
//                        Vec3 N = hit.getDirection().getUnitVec3().multiply(-1.0,-1.0,-1.0);
                        Vec3 N = ((BlockHitResult)surfaceNormalRaycast).getDirection().getUnitVec3();
                        Vec3 I = self.getForward();
                        // I - 2.0 * dot(N, I) * N
                        double NdotI = (N.x * I.x) + (N.y * I.y) + (N.z * I.z);
                        NdotI *= 2.0;
                        Vec3 NdotI2N = N.multiply(NdotI, NdotI, NdotI);

                        Vec3 reflect = I.subtract(NdotI2N);

                        //self.lookAt(EntityAnchorArgument.Anchor.FEET, self.getPosition(0.0f).add(reflect));
                        Vec3 vec3 = self.getPosition(0.0f).add(reflect);
                        Vec3 vec32 = EntityAnchorArgument.Anchor.EYES.apply(self);
                        double d = vec3.x - vec32.x;
                        double e = vec3.y - vec32.y;
                        double f = vec3.z - vec32.z;
                        double g = Math.sqrt(d * d + f * f);
                        float yRot = Mth.wrapDegrees((float)(Mth.atan2(f, d) * 180.0F / (float)Math.PI) - 90.0F);

                        self.setYRot(yRot);

                        speed *= 0.5f;

                        fwd = self.getForward();
                        move = fwd.multiply(speed, speed, speed);

                        from = self.getPosition(0.0f).add(0, 1.5, 0);
                        from = from.add(fwd.multiply(speed, speed, speed));

                        to = self.getPosition(0.0f).add(0, -3, 0);
                        to = to.add(fwd.multiply(speed, speed, speed));

                        hit = self.level().clip(new ClipContext(
                                        from, to,
                                        ClipContext.Block.COLLIDER,
                                        ClipContext.Fluid.NONE,
                                        CollisionContext.empty()
                                )
                        );

                        hitPoint = hit.getLocation();
                    }

                }

                self.setPos(hitPoint);
            }
            else
            {
                self.setPos(to);
            }
            //self.setPos(self.getPosition(0.0f).add(move));
        }

        var packet = ClientboundTeleportEntityPacket.teleport(
                self.getId(),
                new PositionMoveRotation(self.position(), self.getDeltaMovement(), self.getYRot(), self.getXRot()),
                Set.of(),
                self.onGround()
        );
        for (ServerPlayer player : PlayerLookup.tracking(self)) {
            player.connection.send(packet);
        }

        BlockPos blockPos = self.getOnPos();
        Fill.replace(
                (ServerLevel)self.level(),
                blockPos.offset(0,1,0),
                blockPos.offset(0,-1,0),
                team.getGroundBlock(),
                Splatoon.Tags.GROUND_BLOCKS
        );

        if(self.tickCount % 7 == 0)
        {
            self.level().playSound(null, self.getOnPos(), SoundEvents.SQUID_AMBIENT, SoundSource.HOSTILE);
        }

        if(self.tickCount > 100) {
            self.discard();
        }
    }
}
