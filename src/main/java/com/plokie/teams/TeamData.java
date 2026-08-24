package com.plokie.teams;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.plokie.Splatoon;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Optional;

public class TeamData {
    String groundBlockId = "minecraft:black_concrete_powder";
    String wallBlockId = "minecraft:black_wool";
    int teamColourInt = 0;
    byte teamColourByte = 0;
    String bossbarColour = "black";

    public static final Codec<TeamData> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.STRING.optionalFieldOf("groundBlock", "minecraft:black_concrete_powder")
                            .forGetter(TeamData::getGroundBlockId),
                    Codec.STRING.optionalFieldOf("wallBlock", "minecraft:black_wool")
                            .forGetter(TeamData::getWallBlockId),
                    Codec.INT.optionalFieldOf("teamColourInt", 0)
                            .forGetter(TeamData::getTeamColourInt),
                    Codec.BYTE.optionalFieldOf("teamColourByte", (byte)0)
                            .forGetter(TeamData::getTeamColourByte),
                    Codec.STRING.optionalFieldOf("bossbarColour", "black")
                            .forGetter(TeamData::getBossbarColour)
            ).apply(instance, TeamData::new)
    );

    TeamData()
    {

    }

    TeamData(String groundBlock, String wallBlock, int teamColourInt, byte teamColourByte, String bossbarColour)
    {
        this.groundBlockId = groundBlock;
        this.wallBlockId = wallBlock;
        this.teamColourInt = teamColourInt;
        this.teamColourByte = teamColourByte;
        this.bossbarColour = bossbarColour;
    }

    public String getGroundBlockId()
    {
        return groundBlockId;
    }

    public Block getGroundBlock()
    {
        Optional<Holder.Reference<Block>> block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(groundBlockId));

        if(block.isEmpty()) {
            return Blocks.AIR;
        }

        return block.get().value();
    }

    public void setGroundBlock(Block block)
    {
        groundBlockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        Splatoon.LOGGER.info("TeamData:Set ground block to {}", this.groundBlockId);
    }


    public String getWallBlockId()
    {
        return wallBlockId;
    }

    public Block getWallBlock()
    {
        Optional<Holder.Reference<Block>> block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(wallBlockId));

        if(block.isEmpty()) {
            return Blocks.AIR;
        }

        return block.get().value();
    }

    public void setWallBlock(Block block)
    {
        wallBlockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        Splatoon.LOGGER.info("TeamData:Set wall block to {}", this.wallBlockId);
    }

    public int getTeamColourInt()
    {
        return teamColourInt;
    }

    public void setTeamColourInt(int teamColourInt)
    {
        this.teamColourInt = teamColourInt;

        Splatoon.LOGGER.info("TeamData:Set team colour integer to {}", this.teamColourInt);
    }

    public byte getTeamColourByte()
    {
        return teamColourByte;
    }

    public void setTeamColourByte(byte teamColourByte)
    {
        this.teamColourByte = teamColourByte;

        Splatoon.LOGGER.info("TeamData:Set team colour byte to {}", this.teamColourByte);
    }

    public String getBossbarColour()
    {
        return bossbarColour;
    }

    public void setBossbarColour(String bossbarColour)
    {
        this.bossbarColour = bossbarColour;

        Splatoon.LOGGER.info("TeamData:Set team bossbar colour to {}", this.bossbarColour);
    }
}
