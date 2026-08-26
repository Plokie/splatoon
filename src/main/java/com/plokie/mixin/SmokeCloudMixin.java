package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.apache.logging.log4j.core.jmx.Server;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(AreaEffectCloud.class)
public abstract class SmokeCloudMixin {

    @org.jetbrains.annotations.Nullable
    @Shadow
    public abstract Entity getOwner();

    @Unique
    Block groundBlock = Blocks.AIR;

    @Inject(method = "tick", at=@At("TAIL"))
    void onTick(CallbackInfo ci)
    {
        AreaEffectCloud self = (AreaEffectCloud)(Object)this;

        if(!self.getTags().contains("SmokeCloud")) return;

        ServerLevel level = (ServerLevel)self.level();

        if(level.isClientSide()) return;

        level.getPlayers(player->true).forEach(player->{
            level.sendParticles(player,
                    ParticleTypes.EXPLOSION_EMITTER,
                    true, // unknown? Something to do with distance
                    true, // Force render
                    self.position().x, self.position().y, self.position().z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0
            );
        });

        Holder<DamageType> damageTypeHolder = Splatoon.SERVER.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.MAGIC);

        DamageSource source;

        if(getOwner() == null)
        {
            source = new DamageSource(damageTypeHolder);
        }
        else {
            source = new DamageSource(damageTypeHolder, getOwner());
        }

        level.getEntitiesOfClass(LivingEntity.class, new AABB(self.getOnPos()).inflate(2.5)).forEach(entity -> {
            if(entity != getOwner())
            {
                entity.hurtServer(level, source, 2.0f);
            }
        });

        if(getOwner() instanceof Player player)
        {
            IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
            if(playerTeam != null) {
                if(groundBlock == Blocks.AIR)
                {
                    // cache ground block so we arent getting it from the db every time
                    groundBlock = playerTeam.getGroundBlock();
                }

                if(groundBlock != Blocks.AIR)
                {
                    int numReplaced = Fill.replace(
                            level,
                            self.getOnPos(),
                            new BlockPos(2,2,2),
                            new BlockPos(-2,-2,-2),
                            groundBlock,
                            Splatoon.Tags.GROUND_BLOCKS
                    );
                }

            }
        }



        if(self.tickCount >= 300)
        {
            self.discard();
        }
    }
}
