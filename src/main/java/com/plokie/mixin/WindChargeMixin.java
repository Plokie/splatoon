package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractWindCharge.class)
public class WindChargeMixin {

    @Inject(method="onHitEntity", at=@At("TAIL"))
    void onHitEntity(CallbackInfo ci)
    {
        onHitBlock(ci);
    }

    @Inject(method="onHitBlock", at=@At("TAIL"))
    void onHitBlock(CallbackInfo ci)
    {
        Splatoon.LOGGER.info("EXPLODEEEEE");
        AbstractWindCharge self = ((AbstractWindCharge)(Object)this);
        Level level = self.level();
        Entity owner = self.getOwner();
        if(owner != null) {
            if (owner instanceof Player player) {
                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                if (playerTeam != null) {
                    int numReplaced = Fill.replace(
                            level,
                            self.getOnPos(),
                            new BlockPos(2,2,2),
                            new BlockPos(-2,-2,-2),
                            playerTeam.getGroundBlock(),
                            Splatoon.Tags.GROUND_BLOCKS
                    );

                    numReplaced += Fill.replace(
                            level,
                            self.getOnPos(),
                            new BlockPos(2,2,2),
                            new BlockPos(-2,-2,-2),
                            playerTeam.getWallBlock(),
                            Splatoon.Tags.WALL_BLOCKS
                    );
                }
            }
        }
    }
}
