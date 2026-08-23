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

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin implements IPlayerTeamMixin {
    @Shadow
    @Final
    private String name;

    @Override public Block getGroundBlock()
    {
        Splatoon.LOGGER.info("Requested {} get ground block", this.name);
        TeamSaveData teamSaveData = Splatoon.SERVER.overworld().getDataStorage().get(TeamSaveData.TYPE);
        assert teamSaveData != null;
        TeamData data = teamSaveData.getTeamData(this.name);

        Splatoon.LOGGER.info("\tGot data");

        return data.getGroundBlock();
    }
    @Override public void setGroundBlock(Block block)
    {
        Splatoon.LOGGER.info("Requested {} set ground block {}", this.name, block.getDescriptionId());
        TeamSaveData teamSaveData = Splatoon.SERVER.overworld().getDataStorage().get(TeamSaveData.TYPE);
        assert teamSaveData != null;
        TeamData data = teamSaveData.getTeamData(this.name);

        Splatoon.LOGGER.info("\tGot data");

        data.setGroundBlock(block);

        teamSaveData.setDirty();

        Splatoon.LOGGER.info("\tSet data");
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(Scoreboard scoreboard, String name, CallbackInfo ci) {

    }
}
