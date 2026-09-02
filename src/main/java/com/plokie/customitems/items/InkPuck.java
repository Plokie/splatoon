package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class InkPuck extends ICustomItem {
    @Override
    public void onUseBlock(Player player, BlockHitResult hit) {
        super.onUseBlock(player, hit);

        Splatoon.LOGGER.info("Use ink puck on block {}", hit.getBlockPos());
    }

}
