package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractThrownPotion.class)
public class SplashPotionMixin {

    @Inject(method="onHit", at=@At("TAIL"))
    void onHit(HitResult hitResult, CallbackInfo ci)
    {
        AbstractThrownPotion thrownPotion = (AbstractThrownPotion)(Object)this;

        if(thrownPotion instanceof ThrownSplashPotion self)
        {
            Level level = self.level();
            Entity owner = self.getOwner();
            if(owner != null) {
                if (owner instanceof Player player) {
                    IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                    if (playerTeam != null)
                    {
                        int numReplaced = Fill.replace(
                                level,
                                self.getOnPos(),
                                new BlockPos(1,1,1),
                                new BlockPos(-1,-1,-1),
                                playerTeam.getGroundBlock(),
                                BlockTags.CONCRETE_POWDER
                        );

                        numReplaced += Fill.replace(
                                level,
                                self.getOnPos(),
                                new BlockPos(1,1,1),
                                new BlockPos(-1,-1,-1),
                                playerTeam.getWallBlock(),
                                BlockTags.WOOL
                        );
                    }
                }
            }
        }

    }
}
