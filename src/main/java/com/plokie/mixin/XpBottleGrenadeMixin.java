package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.interfaces.IProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ThrownExperienceBottle.class)
public class XpBottleGrenadeMixin implements IProjectile {
    @Unique
    UUID playerOwnerUUID = null;

    @Override
    public void setPlayerOwner(Player player)
    {
        playerOwnerUUID = player.getUUID();
    }

    @Inject(method="onHit", at = @At("TAIL"))
    private void onHit(HitResult hitResult, CallbackInfo ci)
    {
        if(playerOwnerUUID == null) return;

        ThrownExperienceBottle self = (ThrownExperienceBottle)(Object)this;
        ServerLevel level = (ServerLevel)self.level();


        BlockPos blockPos = Helpers.toBlockPos(hitResult.getLocation());

        Fill.replace(
                level, blockPos,
                new BlockPos(-5,-5,-5),
                new BlockPos(5,5,5),
                Blocks.CYAN_TERRACOTTA, Splatoon.Tags.GROUND_BLOCKS
        );

        Fill.replace(
                level, blockPos,
                new BlockPos(-5,-5,-5),
                new BlockPos(5,5,5),
                Blocks.PALE_MOSS_BLOCK, Splatoon.Tags.WALL_BLOCKS
        );

        Effects.explosionEffect(level, blockPos);

        int numHurtEntities = Affects.hurtLivingEntitiesInRange(level, blockPos, 5.5f, 400.f, level.getPlayerByUUID(playerOwnerUUID), DamageTypes.EXPLOSION);


        Player player = level.getPlayerByUUID(playerOwnerUUID);

        if(player == null) return;

        //todo: player stats
    }
}
