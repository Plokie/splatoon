package com.plokie.interfaces;

import net.minecraft.world.level.block.Block;

public interface IPlayerTeamMixin {
    Block getGroundBlock();
    void setGroundBlock(Block block);

    Block getWallBlock();
    void setWallBlock(Block block);

    int getTeamColourInt();
    void setTeamColourInt(int teamColourInt);

    byte getTeamColourByte();
    void setTeamColourByte(byte teamColourByte);

    String getBossbarColour();
    void setBossbarColour(String teamBossbarColour);
}
