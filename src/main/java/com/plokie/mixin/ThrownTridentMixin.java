package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ThrownTrident.class)
public class ThrownTridentMixin implements IProjectile {
    @Unique
    UUID playerOwnerUUID = null;

    @Unique boolean hasHit = false;
    @Unique boolean hasSetup = false;

    @Unique LivingEntity hookedEntity = null;

    @Override
    public void setPlayerOwner(Player player)
    {
        playerOwnerUUID = player.getUUID();
    }

    @Inject(method="onHitEntity", at = @At("TAIL"))
    private void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        if(!hasHit)
        {
            hitGround();
        }
    }

    @Inject(method="tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci)
    {
        ThrownTrident self = (ThrownTrident)(Object)this;

        //Splatoon.LOGGER.info("Trident tick");

        if(!hasSetup)
        {
            Entity owner = self.getOwner();
            Splatoon.LOGGER.info("Trident thrown by {}", owner.getName());
            if(owner instanceof Player player)
            {
                Splatoon.LOGGER.info("Trident thrown by player {}", owner.getName());
                ((IPlayerMixin)player).onUseAbilityItem("Hook", player, player.getUsedItemHand());
            }
            hasSetup = true;
        }

        if(self.onGround() && !hasHit)
        {
            hitGround();
        }

        if(hasHit && hookedEntity != null)
        {
            hookedEntity.startRiding(self, true);
        }
    }

    void hitGround()
    {
        ThrownTrident self = (ThrownTrident)(Object)this;

        Level level = self.level();

        Effects.explosionEffect(level, self.getOnPos());

        Player player = level.getPlayerByUUID(playerOwnerUUID);
        if(player != null) {
            IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
            if(playerTeam!=null)
            {
                int numReplaced = Fill.replace(
                        level,
                        self.getOnPos(),
                        new BlockPos(3,3,3),
                        new BlockPos(-3,-3,-3),
                        playerTeam.getGroundBlock(),
                        BlockTags.CONCRETE_POWDER
                );

                numReplaced += Fill.replace(
                        level,
                        self.getOnPos(),
                        new BlockPos(3,3,3),
                        new BlockPos(-3,-3,-3),
                        playerTeam.getWallBlock(),
                        BlockTags.WOOL
                );
            }

            float nearestDistance = 9999.f;
            LivingEntity nearestEntity = null;

            AABB aabb = new AABB(self.getOnPos()).inflate(3.5);
            for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb))
            {
                float distance = entity.distanceTo(self);

                if(distance < 3.5f)
                {
                    if(distance < nearestDistance && entity != player) {
                        nearestDistance = distance;
                        nearestEntity = entity;
                    }

                    Effects.givePotionEffect(entity, MobEffects.BLINDNESS, 7, 3, true);
                    Effects.givePotionEffect(entity, MobEffects.SLOWNESS, 3, 3, true);
                    Effects.givePotionEffect(entity, MobEffects.WEAKNESS, 3, 3, true);
                }
            }

            if(nearestEntity != null)
            {
                hookedEntity = nearestEntity;
            }
        }


        hasHit = true;
    }

}
