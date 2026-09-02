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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SpectateItem extends ICustomItem {

    public SpectateItem()
    {
        this.usageRate = 10;
    }

    @Override
    public void onUseItem(Player player)
    {
        Splatoon.gameFlowManager.toggleSpectator((ServerPlayer)player);
    }

    @Override
    public void whileHeld(Player player)
    {

    }
}
