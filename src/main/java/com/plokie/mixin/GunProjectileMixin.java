package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.customitems.items.BuildingRails;
import com.plokie.customitems.items.guns.Gun;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IGunProjectileMixin;
import com.plokie.interfaces.IInkablePayloadBlock;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Snowball.class)
public class GunProjectileMixin implements IGunProjectileMixin {
    @Unique
    CustomItem shotByItem = null;

    @Override
    public void setShotByCustomItem(CustomItem item) {
        shotByItem = item;
    }

    @Inject(method="onHit", at=@At("TAIL"))
    void onHit(HitResult hitResult, CallbackInfo ci)
    {
        hit(null);
    }

    @Inject(method="onHitEntity", at=@At("TAIL"))
    void onHitEntity(EntityHitResult entityHitResult, CallbackInfo ci)
    {
        Snowball snowball = (Snowball)(Object)this;
        Entity ownerEntity = snowball.getOwner();
        if(ownerEntity == null) return;
        if(!(ownerEntity instanceof Player player)) return;

        if(entityHitResult.getEntity() instanceof Slime slime) {
            IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
            if(team != null) {
                IPlayerTeamMixin currentTeam = ((IInkablePayloadBlock)slime).getTeam();
                if(currentTeam != team) {
                    ((IInkablePayloadBlock)slime).setTeam(team);
                    PlayerStats.get(player).add(PlayerStats.PAYLOAD_INKED, 1);
                }
            }

        }


        hit(entityHitResult.getEntity());
    }

    void hit(Entity entity)
    {
        Snowball snowball = (Snowball)(Object)this;
        Entity ownerEntity = snowball.getOwner();
        if(ownerEntity == null) return;

        if(!(ownerEntity instanceof Player player)) return;

        IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
        if(playerTeam == null) return;

        if(shotByItem == null) return;

        if(!(shotByItem.getItemDefinition().getItemInterface() instanceof Gun gun)) return;

        Level level = player.level();

        if(entity != null)
        {
           if(entity instanceof LivingEntity livingEntity)
           {
               Affects.hurtEntity(livingEntity, gun.getDamage(), player, DamageTypes.ARROW);
           }
        }

        Vec3 pos = snowball.position();

        BlockPos blockAbove = snowball.getOnPos();
        blockAbove = new BlockPos(blockAbove.getX(), blockAbove.getY() + 1, blockAbove.getZ());
        BuildingRails.FetchedRail rail = BuildingRails.getRailNodeForPosition(blockAbove);
        if(rail != null) {
            BuildingRails.PlacedRails placedRails = rail.placedRails();
            List<BuildingRails.RailNode> channel = placedRails.channels.get(rail.channelIndex());
            BuildingRails.RailNode node = channel.get(rail.nodeIndex());
            BuildingRails.RailNode nextNode = channel.get(rail.nodeIndex() + 1);

            boolean wasBroken = node.isBroken;

            node.changeHealth(snowball.level(), -0.02f, nextNode);

            level.playSound(
                    null,
                    pos.x, pos.y, pos.z,
                    SoundEvents.ANVIL_LAND,
                    SoundSource.HOSTILE,
                    1.0f,
                    1.0f
            );

            if(!wasBroken && node.isBroken) {
                level.playSound(
                        null,
                        pos.x, pos.y, pos.z,
                        SoundEvents.ITEM_BREAK,
                        SoundSource.HOSTILE,
                        4.0f,
                        1.0f
                );
            }
        }


        int numReplaced = Fill.replace(
                level,
                snowball.getOnPos(),
                new BlockPos(gun.getInkSpread(),1,gun.getInkSpread()),
                new BlockPos(-gun.getInkSpread(),-1,-gun.getInkSpread()),
                playerTeam.getGroundBlock(),
                Splatoon.Tags.GROUND_BLOCKS
        );

        numReplaced += Fill.replace(
                level,
                snowball.getOnPos(),
                new BlockPos(1,1,1),
                new BlockPos(-1,-1,-1),
                playerTeam.getWallBlock(),
                Splatoon.Tags.WALL_BLOCKS
        );

        PlayerStats.get(player).add(PlayerStats.BLOCKS_INKED, numReplaced);
    }


}
