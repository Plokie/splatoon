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
    String groundBlockId = "minecraft:air";

    public static final Codec<TeamData> CODEC = RecordCodecBuilder.create(instance->
            instance.group(
                    Codec.STRING.fieldOf("groundBlock").forGetter(TeamData::getGroundBlockId)
            ).apply(instance, TeamData::new)
    );

    TeamData()
    {

    }

    TeamData(String groundBlock)
    {
        this.groundBlockId = groundBlock;
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
}
