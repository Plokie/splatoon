package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.*;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IWindChargeMixin;
import com.plokie.management.PlayerStats;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(AbstractWindCharge.class)
public class WindChargeMixin implements IWindChargeMixin {

    @Unique
    boolean shotByLauncher = false;

    @Override
    public void setShotByLauncher(boolean value) {
        shotByLauncher = value;
    }

    @Inject(method="tick", at=@At("TAIL"))
    void tick(CallbackInfo ci)
    {
        AbstractWindCharge self = (AbstractWindCharge)(Object)this;
        ServerLevel level = (ServerLevel)self.level();

        Vec3 pos = self.position();

        if(shotByLauncher) {
            level.sendParticles(ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        }
    }

    @Inject(method="onHitEntity", at=@At("TAIL"))
    void onHitEntity(CallbackInfo ci)
    {
        onHitBlock(ci);
    }

    @Inject(method="onHitBlock", at=@At("TAIL"))
    void onHitBlock(CallbackInfo ci)
    {
        //Splatoon.LOGGER.info("EXPLODEEEEE");
        AbstractWindCharge self = ((AbstractWindCharge)(Object)this);
        ServerLevel level = (ServerLevel)self.level();
        Entity owner = self.getOwner();
        Vec3 pos = self.position();

        if(shotByLauncher)
        {

            level.playSound(
                    null, // everyone
                    pos.x, pos.y, pos.z,
                    SoundEvents.ENDER_DRAGON_FLAP,
                    SoundSource.HOSTILE,
                    4.0f, // volume
                    1.0f // pitch
            );

            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y, pos.z,
                    1, // count
                    0.0, 0.0, 0.0, // delta
                    0.0 // speed
            );
        }


        if(owner != null) {
            if (owner instanceof Player player) {
                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                if (playerTeam != null) {
                    int fillSize = 2;
                    if(shotByLauncher) {
                        //fillSize = 3;

                        for (LivingEntity target : Helpers.getEntitiesInRadius(level, LivingEntity.class, pos, 3.5f)) {
                            boolean doHurt = false;

                            if(target instanceof Player targetPlayer)
                            {
                                IPlayerTeamMixin targetTeam = Teams.getTeamMixinFromPlayer(targetPlayer);
                                if(targetTeam != playerTeam) {
                                    doHurt = true;
                                }
                            }
                            else
                            {
                                doHurt = true;
                            }

                            if(doHurt)
                            {
                                Affects.hurtEntity(target, 5.0f, player, DamageTypes.PLAYER_EXPLOSION);
                            }

                            Vec3 diff = target.position().subtract(pos);
                            Vec3 dir = diff.normalize();

                            if(target != owner) {

                                float force = 3.0f;
                                Vec3 forceDir = dir.multiply(force, force, force);

                                target.addDeltaMovement(forceDir);

//                                var packet = ClientboundTeleportEntityPacket.teleport(
//                                        target.getId(),
//                                        new PositionMoveRotation(target.position(), target.getDeltaMovement(), target.getYRot(), target.getXRot()),
//                                        Set.of(),
//                                        target.onGround()
//                                );
                                if(target instanceof ServerPlayer player1)
                                {
                                    player1.connection.send(new ClientboundSetEntityMotionPacket(target));
                                }

                                for (ServerPlayer player1 : PlayerLookup.tracking(target)) {
                                    player1.connection.send(new ClientboundSetEntityMotionPacket(target));
                                }
                            }
//                            else {
//                                // For player who shot the wind charge,
//                                // add an extra force if they are looking away from
//                                // the point of explosion
//                                Vec3 forward = target.getForward();
//
//                                float dot = (float)-forward.dot(dir);
//
//                                float force = 1.5f;
//                                force *= (dot + 1.0f) * 0.5f;
//                                Vec3 forceDir = dir.multiply(force, force, force);
//
//                                target.addDeltaMovement(forceDir);
//
//                                if(target instanceof ServerPlayer player1)
//                                {
//                                    player1.connection.send(new ClientboundSetEntityMotionPacket(target));
//                                }
//
//                                for (ServerPlayer player1 : PlayerLookup.tracking(target)) {
//                                    player1.connection.send(new ClientboundSetEntityMotionPacket(target));
//                                }
//                            }
                        }
                    }

                    int numReplaced = Fill.replace(
                            level,
                            self.getOnPos(),
                            new BlockPos(fillSize,fillSize,fillSize),
                            new BlockPos(-fillSize,-fillSize,-fillSize),
                            playerTeam.getGroundBlock(),
                            Splatoon.Tags.GROUND_BLOCKS
                    );

                    numReplaced += Fill.replace(
                            level,
                            self.getOnPos(),
                            new BlockPos(fillSize,fillSize,fillSize),
                            new BlockPos(-fillSize,-fillSize,-fillSize),
                            playerTeam.getWallBlock(),
                            Splatoon.Tags.WALL_BLOCKS
                    );

                    PlayerStats.get(player).add(PlayerStats.BLOCKS_INKED, numReplaced);

                    Helpers.inkSlimesInRadius(self.position(), 2.5f, player);
                }
            }
        }
    }
}
