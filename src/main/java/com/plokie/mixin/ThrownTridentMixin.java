package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.CustomItem;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.interfaces.IProjectile;
import com.plokie.interfaces.IThrownTrident;
import com.plokie.management.PlayerStats;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;

@Mixin(AbstractArrow.class)
public class ThrownTridentMixin implements IThrownTrident {
    //@Unique
    //UUID playerOwnerUUID = null;

    @Unique boolean hasHit = false;
    @Unique boolean hasSetup = false;

    @Unique LivingEntity hookedEntity = null;
    @Unique LivingEntity ropedEntity = null;

    @Override
    public void setPlayerOwner(Player player)
    {

    }

    @Override
    public void setRopedTarget(LivingEntity ropedTarget)
    {
        hasHit = true;
        ropedEntity = ropedTarget;
        //ropedTarget.startRiding((AbstractArrow)(Object)this, true);
        //hookedEntity = ropedTarget;
    }

    @Inject(method="onHitEntity", at = @At("TAIL"))
    private void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object)this;

        self.ejectPassengers();
        ropedEntity= null;

        Splatoon.LOGGER.info("Hit entity {}", entityHitResult.getEntity().getName());

        if(!(self instanceof ThrownTrident)) return;

        if(!hasHit)
        {
            Splatoon.LOGGER.info("Hit entity {}", entityHitResult.getEntity().getName());
            hitGround();
        }
    }

    @Inject(method="onHitBlock", at = @At("TAIL"))
    void onHitBlock(BlockHitResult blockHitResult, CallbackInfo ci)
    {
        AbstractArrow self = (AbstractArrow) (Object)this;

        if(!(self instanceof ThrownTrident)) return;

        self.ejectPassengers();
        ropedEntity = null;

        if(!hasHit)
        {
            Splatoon.LOGGER.info("Hit ground");
            hitGround();
        }
    }

    @Inject(method="tick", at = @At("TAIL"))
    private void tick(CallbackInfo ci)
    {
        AbstractArrow self = (AbstractArrow) (Object)this;

        if(!(self instanceof ThrownTrident)) return;

        Entity owner = self.getOwner();

        if(owner != null) {
            if(owner instanceof Player playerOwner) {
                IPlayerMixin playerMixin = (IPlayerMixin) playerOwner;
                if(playerMixin.getSplatoonClass() == null) self.discard();
            }
        }

//        if(!self.getPassengers().isEmpty())
//        {
//            for(ServerPlayer serverPlayer : PlayerLookup.tracking(self))
//            {
//                serverPlayer.connection.send(new ClientboundSetPassengersPacket(self));
//            }
//        }
        if(ropedEntity != null) {
            ropedEntity.setPos(self.getPosition(0.0f));
            ropedEntity.setDeltaMovement(self.getDeltaMovement());
            var packet = ClientboundTeleportEntityPacket.teleport(
                    ropedEntity.getId(),
                    new PositionMoveRotation(ropedEntity.position(), ropedEntity.getDeltaMovement(), ropedEntity.getYRot(), ropedEntity.getXRot()),
                    Set.of(),
                    self.onGround()
            );
            if(ropedEntity instanceof Player ropedPlayer) {
                ((ServerPlayer)ropedPlayer).connection.send(packet);
            }
            for(ServerPlayer serverPlayer : PlayerLookup.tracking(ropedEntity))
            {
                serverPlayer.connection.send(packet);
            }
        }

        if(!hasSetup)
        {

            Splatoon.LOGGER.info("Trident thrown by {}", owner.getName());
            if(owner instanceof Player player)
            {
                Splatoon.LOGGER.info("Trident thrown by player {}", owner.getName());
                //((IPlayerMixin)player).onUseAbilityItem("Hook", player, player.getUsedItemHand());
                Ability ability = ((IPlayerMixin)player).getAbility(AbilityManager.AbilityEnum.Hook);
                if(ability != null) {
//                    ability.onUseItem(player, player.getUsedItemHand(), 0);
                    ability.onUse();
                    CustomItem.Hook.getItemDefinition().getItemInterface().onUserFunc(player, self);
                }
                else {
                    Splatoon.LOGGER.warn("Couldnt find hook ability on player");
                }
            }
            hasSetup = true;
        }

        if(!hasHit)
        {
            for (LivingEntity entity : self.level().getEntitiesOfClass(LivingEntity.class, new AABB(self.getOnPos()).inflate(1.0))) {
                if(entity == self.getOwner()) continue;

                hitGround();
            }
        }

        if(hookedEntity != null)
        {
            if(!hookedEntity.isPassenger())
            {
                hookedEntity.startRiding(self, true);
            }
        }
    }

    void hitGround()
    {
        AbstractArrow selfArrow = (AbstractArrow) (Object)this;

        if(!(selfArrow instanceof ThrownTrident)) return;
        //if(!((Object)this instanceof ThrownTrident)) return;

        ThrownTrident self = (ThrownTrident)selfArrow;

        Level level = self.level();

        Effects.explosionEffect(level, self.getOnPos());

        if(self.getOwner() == null) {
            Splatoon.LOGGER.warn("Faulty trident with no owner");
            return;
        }

        if(!(self.getOwner() instanceof Player)) return;

//        Player player = level.getPlayerByUUID(playerOwnerUUID);
        Player player = (Player)self.getOwner();
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
                        Splatoon.Tags.GROUND_BLOCKS
                );

                numReplaced += Fill.replace(
                        level,
                        self.getOnPos(),
                        new BlockPos(3,3,3),
                        new BlockPos(-3,-3,-3),
                        playerTeam.getWallBlock(),
                        Splatoon.Tags.WALL_BLOCKS
                );

                PlayerStats.get(player).add(PlayerStats.BLOCKS_INKED, numReplaced);
            }

            float nearestDistance = 9999.f;
            LivingEntity nearestEntity = null;

            AABB aabb = new AABB(self.getOnPos()).inflate(3.5);
            for(LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, aabb))
            {
                if(entity instanceof Shulker) continue;
                if(entity == player) continue;

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
            if(nearestEntity == null)
            {
                Splatoon.LOGGER.info("Trident couldnt find nearest entity");
            }
        }


        hasHit = true;
    }

}
