package com.plokie.interfaces;

import net.minecraft.world.level.block.Block;

public interface IPlayerTeamMixin {
    Block getGroundBlock();
    void setGroundBlock(Block block);
}
