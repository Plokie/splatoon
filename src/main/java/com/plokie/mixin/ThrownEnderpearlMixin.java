package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.PlayerStats;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public class ThrownEnderpearlMixin {
    @Inject(method="tick", at=@At("TAIL"))
    private void tick(CallbackInfo ci)
    {
        ThrownEnderpearl self = ((ThrownEnderpearl)(Object)this);

        Entity owner = self.getOwner();

        if(owner != null)
        {
            if(!self.getOwner().isPassenger())
            {
                self.getOwner().startRiding(self, true);
            }
        }
    }

    @Inject(method="onHit", at=@At("TAIL"))
    private void onHit(HitResult hitResult, CallbackInfo ci)
    {
        ThrownEnderpearl self = ((ThrownEnderpearl)(Object)this);
        Level level = self.level();
        Entity owner = self.getOwner();
        if(owner != null)
        {
            if(owner instanceof Player player)
            {
                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                if(playerTeam != null)
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

                    Effects.explosionEffect(level, player.getOnPos());
                }
            }
        }

    }
}
