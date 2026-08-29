package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class InkRoller extends ICustomItem {

    public InkRoller()
    {
        this.useDuration = 5;
        this.usageRate = 2;
    }

    @Override
    public void onUse(Player player)
    {
        IPlayerMixin playerMixin = (IPlayerMixin)player;

        if(playerMixin.getInk() <= 0.0f) return;

        if(playerMixin.getTimeNotInInk() < 20) {
            return;
        }

        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
        if(team == null) return;

        Vec2 rot = player.getRotationVector();


        float temp = 0.0f;

        Vec3 forward = player.getForward();
        temp = 3.0f;
        forward = new Vec3(forward.x * temp, forward.y * temp, forward.z * temp);

        Vec2 tempRot = new Vec2(rot.x, rot.y - 90.0f);
        Vec3 left = Vec3.directionFromRotation(tempRot);
        temp = 3.0f;
        left = new Vec3(left.x * temp, left.y * temp, left.z * temp);

        tempRot = new Vec2(rot.x, rot.y + 90.0f);
        Vec3 right = Vec3.directionFromRotation(tempRot);
        right = new Vec3(right.x * temp, right.y * temp, right.z * temp);

        Vec3 eyePos = player.getEyePosition();

        Block groundBlock = team.getGroundBlock();

        for(int height = -3; height <= 3; height++)
        {
            Vec3 bl = new Vec3(
                    eyePos.x + forward.x + left.x,
                    eyePos.y + forward.y + left.y + height,
                    eyePos.z + forward.z + left.z
            );

            Vec3 tr = new Vec3(
                    eyePos.x + forward.x + right.x,
                    eyePos.y + forward.y + right.y + height,
                    eyePos.z + forward.z + right.z
            );

            BlockGetter.traverseBlocks(bl, tr, null, (context, currentPos)->{
                int filled = Fill.replace((ServerLevel)player.level(), currentPos, currentPos, groundBlock, Splatoon.Tags.GROUND_BLOCKS);

                if(filled > 0) {
                    playerMixin.changeInk(-0.0033f);
                }

                return null;
            }, (ctx)->{return null;});
        }
    }

    @Override
    public void whileHeld(Player player)
    {

    }
}
