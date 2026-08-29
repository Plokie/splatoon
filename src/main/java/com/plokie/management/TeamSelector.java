package com.plokie.management;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.scores.PlayerTeam;

import java.util.function.Consumer;

public class TeamSelector {
    public enum Type {
        OwnTeam, TeamSlot
    }

    public final BlockPos blockPos;
    public final Type type;
    public final int teamIndex;
    public PlayerTeam selectedTeam = null;

    public TeamSelector(BlockPos blockPos, Type type, int teamIndex)
    {
        this.blockPos = blockPos;
        this.type = type;
        this.teamIndex = teamIndex;

        if(this.type == Type.TeamSlot)
        {
            BlockPos readyUpZoneSize = Splatoon.gameFlowManager.readyUpZoneSize;
            int segmentSize = (int)Math.floor(readyUpZoneSize.getX() / (float)Splatoon.gameFlowManager.getCurrentGamemode().getNumTeams());
            int halfSegmentSize = (int)Math.floor(segmentSize * 0.5f);

            Fill.replace(
                    Splatoon.SERVER.overworld(),
                    blockPos.offset(-halfSegmentSize, -2, 0),
                    blockPos.offset(halfSegmentSize, 0, readyUpZoneSize.getZ() + 2),
                    Blocks.CYAN_TERRACOTTA,
                    Splatoon.Tags.GROUND_BLOCKS
            );
        }

        BlockEntity blockEntity = Splatoon.SERVER.overworld().getBlockEntity(blockPos);
        if(blockEntity != null)
        {
            if(blockEntity instanceof BarrelBlockEntity barrel)
            {
                int i=0;
                for(PlayerTeam team : Splatoon.SERVER.getScoreboard().getPlayerTeams())
                {
                    ItemStack currentItem = barrel.getItem(i);
                    if(!currentItem.getItemName().equals(team.getFormattedDisplayName()))
                    {
                        IPlayerTeamMixin teamMixin = (IPlayerTeamMixin)team;

                        Block block = null;
                        try {
                            block = teamMixin.getWallBlock();
                        }
                        catch(NullPointerException e)
                        {
                            Splatoon.LOGGER.warn("Team {} did not have expected team data", team.getDisplayName());
                            continue;
                        }
                        if(block == null)
                        {
                            Splatoon.LOGGER.warn("Team {} did not have a valid wall block", team.getDisplayName());
                            continue;
                        }

                        ItemStack item = new ItemStack(block);


                        item.set(DataComponents.ITEM_NAME, team.getFormattedDisplayName());

                        barrel.setItem(i, item);
                    }


                    i++;
                }
            }
        }
    }

    public void callback(Player player, ItemStack clickedItem)
    {
        //PlayerTeam team = Splatoon.SERVER.getScoreboard().getPlayerTeam(clickedItem.getItemName().getString());
        Splatoon.LOGGER.info("Clicked {}", clickedItem.getItemName());

        PlayerTeam team = null;
        for(PlayerTeam checkTeam : Splatoon.SERVER.getScoreboard().getPlayerTeams())
        {
            if(clickedItem.getItemName().equals(checkTeam.getFormattedDisplayName())) {
                team = checkTeam;
            }
        }

        if(team == null) return;

        Splatoon.LOGGER.info("Is a team");

        if(type == Type.OwnTeam)
        {
            Splatoon.SERVER.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
            Splatoon.LOGGER.info("Joined?");
        }
        else
        {
            IPlayerTeamMixin teamMixin = (IPlayerTeamMixin)team;

            selectedTeam = team;

            BlockPos readyUpZoneSize = Splatoon.gameFlowManager.readyUpZoneSize;
            int segmentSize = (int)Math.floor(readyUpZoneSize.getX() / (float)Splatoon.gameFlowManager.getCurrentGamemode().getNumTeams());
            int halfSegmentSize = (int)Math.floor(segmentSize * 0.5f);

            Fill.replace(
                    Splatoon.SERVER.overworld(),
                    blockPos.offset(-halfSegmentSize, -2, 0),
                    blockPos.offset(halfSegmentSize, 0, readyUpZoneSize.getZ() + 2),
                    teamMixin.getGroundBlock(),
                    Splatoon.Tags.GROUND_BLOCKS
            );

        }

    }

}
