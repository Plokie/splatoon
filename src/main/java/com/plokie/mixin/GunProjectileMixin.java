package com.plokie.mixin;

import com.plokie.customitems.CustomItem;
import com.plokie.customitems.items.guns.Gun;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IGunProjectileMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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


        int numReplaced = Fill.replace(
                level,
                snowball.getOnPos(),
                new BlockPos(gun.getInkSpread(),1,gun.getInkSpread()),
                new BlockPos(-gun.getInkSpread(),-1,-gun.getInkSpread()),
                playerTeam.getGroundBlock(),
                BlockTags.CONCRETE_POWDER
        );

        numReplaced += Fill.replace(
                level,
                snowball.getOnPos(),
                new BlockPos(gun.getInkSpread(),1,gun.getInkSpread()),
                new BlockPos(-gun.getInkSpread(),-1,-gun.getInkSpread()),
                playerTeam.getWallBlock(),
                BlockTags.WOOL
        );
    }


}
