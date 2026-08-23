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

    public static final Codec<TeamData> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.STRING.optionalFieldOf("groundBlock", "minecraft:black_concrete_powder")
                            .forGetter(TeamData::getGroundBlockId),
                    Codec.STRING.optionalFieldOf("wallBlock", "minecraft:black_wool")
                            .forGetter(TeamData::getWallBlockId)
            ).apply(instance, TeamData::new)
    );

    TeamData()
    {

    }

    TeamData(String groundBlock, String wallBlock)
    {
        this.groundBlockId = groundBlock;
        this.wallBlockId = wallBlock;
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

        Splatoon.LOGGER.info("TeamData:Set ground block to {}", groundBlockId);
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

        Splatoon.LOGGER.info("TeamData:Set wall block to {}", wallBlockId);
    }
}
