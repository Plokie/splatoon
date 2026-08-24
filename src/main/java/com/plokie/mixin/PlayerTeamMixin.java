package com.plokie.mixin;

import com.plokie.Splatoon;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.teams.*;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin implements IPlayerTeamMixin {
    @Shadow
    @Final
    private String name;


    @Unique
    <T> T getData(Function<TeamData, T> callback)
    {
        //Splatoon.LOGGER.info("Requested {} get data", this.name);
        TeamSaveData teamSaveData = Splatoon.SERVER.overworld().getDataStorage().get(TeamSaveData.TYPE);
        assert teamSaveData != null;
        TeamData data = teamSaveData.getTeamData(this.name);

        //Splatoon.LOGGER.info("\tGot data for getting");

        return callback.apply(data);
    }

    @Unique
    <T> void setData(Consumer<TeamData> callback)
    {
        //Splatoon.LOGGER.info("Requested {} set data field", this.name);
        TeamSaveData teamSaveData = Splatoon.SERVER.overworld().getDataStorage().get(TeamSaveData.TYPE);
        assert teamSaveData != null;
        TeamData data = teamSaveData.getTeamData(this.name);

        //Splatoon.LOGGER.info("\tGot data for setting");

        callback.accept(data);

        teamSaveData.setDirty();
    }

    @Override public Block getGroundBlock()
    {
        return getData(TeamData::getGroundBlock);
    }
    @Override public void setGroundBlock(Block block)
    {
        setData(data -> data.setGroundBlock(block));
    }

    @Override public Block getWallBlock()
    {
        return getData(TeamData::getWallBlock);
    }
    @Override public void setWallBlock(Block block)
    {
        setData(data -> data.setWallBlock(block));
    }
    @Override public int getTeamColourInt()
    {
        return getData(TeamData::getTeamColourInt);
    }
    @Override public void setTeamColourInt(int teamColourInt)
    {
        setData(data -> data.setTeamColourInt(teamColourInt));
    }
    @Override public byte getTeamColourByte()
    {
        return getData(TeamData::getTeamColourByte);
    }
    @Override public void setTeamColourByte(byte teamColourByte)
    {
        setData(data -> data.setTeamColourByte(teamColourByte));
    }
    @Override public String getBossbarColour()
    {
        return getData(TeamData::getBossbarColour);
    }
    @Override public void setBossbarColour(String teamBosssbarColour)
    {
        setData(data -> data.setBossbarColour(teamBosssbarColour));
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Scoreboard scoreboard, String name, CallbackInfo ci) {

    }
}
