package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.classes.abilities.SuperJump;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class JumperMace extends ICustomItem {

    @Override
    public void onAttackHit(Player player, Entity hitEntity)
    {
        //Vec3 deltaMovement = player.getDeltaMovement();
        //Splatoon.LOGGER.info("Mace hit vel {} fall dist {}", deltaMovement.y, player.fallDistance);


        if(player.fallDistance > 3.5) {
            IPlayerMixin playerMixin = (IPlayerMixin)player;
            playerMixin.getAbilities().forEach(ability -> {
                if(ability instanceof SuperJump superJump) {
                    superJump.setHasDashed(false);
                }
            });


            player.heal(4.0f);

            IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
            if(team != null) {
                int filled = Fill.replace(player.level(), player.getOnPos(),
                        new BlockPos(-3,-3,-3),
                        new BlockPos(3,3,3),
                        team.getGroundBlock(),
                        Splatoon.Tags.GROUND_BLOCKS
                );

            }

        }
    }
}
