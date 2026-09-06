package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.TeamSelector;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.TellRawCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;

public class None implements IGameState {


    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
//        this.winningTeam = -1;
        Splatoon.gameFlowManager.setWinningTeam(-1);
        Vec3 hubSpawn = Splatoon.gameFlowManager.hubSpawn;

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
        {
            if(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR)) {
                player.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.FEET));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.AIR));
            }
        }

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators()) {
            player.teleportTo(hubSpawn.x, hubSpawn.y, hubSpawn.z);

            ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(hubSpawn), 0.0f, true);
            ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);


        }

        for(int i=0; i<currentGamemode.getNumTeams(); i++) {
            int numPlayersOnTeam = Splatoon.gameFlowManager.getTeamPlayers(i).size();

            for(Player player : Splatoon.gameFlowManager.getTeamPlayers(i))
            {
                MutableComponent rewardsText = Component.empty();

                int totalMoneyRewarded = 0;

                for(var entry : currentGamemode.rewards.entrySet())
                {
                    PlayerStats stat = entry.getKey();
                    float multiplier = entry.getValue();

                    int statValue = PlayerStats.get(player).getMatchStat(stat);

                    int rewardedForThisStat = (int)(statValue * multiplier);
                    totalMoneyRewarded += rewardedForThisStat;

                    String mostText = "for " + stat.title.toLowerCase();

                    rewardsText.append(
                            Component.literal("[ +")
                                    .append(String.valueOf(rewardedForThisStat))
                                    .append(" " + mostText + "\n")
                                    .withStyle(ChatFormatting.GOLD)
                    );
                }

                float teamBonusMultiplier = numPlayersOnTeam * 0.5f;
                teamBonusMultiplier = Math.clamp(teamBonusMultiplier, 1.0f, 2.5f);

                totalMoneyRewarded = (int)(totalMoneyRewarded * teamBonusMultiplier);

                DecimalFormat decimalFormat = new DecimalFormat("#.#");

                rewardsText.append(Component.literal("[ *" + decimalFormat.format(teamBonusMultiplier) + " team bonus\n").withStyle(ChatFormatting.GOLD));

                PlayerStats.get(player).forceAddNoMatch(PlayerStats.MONEY, totalMoneyRewarded);
                int newTotalMoney = PlayerStats.get(player).get(PlayerStats.MONEY);
                rewardsText.append(Component.literal("+" + totalMoneyRewarded + " total for the match!\n").withStyle(ChatFormatting.GOLD));
                rewardsText.append(Component.literal("You now have ").append(String.valueOf(newTotalMoney)).append(" gold!\n").withStyle(ChatFormatting.GOLD));

                ((ServerPlayer)player).sendSystemMessage(rewardsText);



                PlayerStats.resetMatchStats(player);
            }
        }

        Splatoon.gameFlowManager.clearActivePlayers();

        Splatoon.gameFlowManager.setGamemode(currentGamemode.toEnum());


    }


    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        if(Splatoon.gameFlowManager.areAllTeamsReady(currentGamemode))
        {
            return GameFlowManager.GameState.INTRO;
        }

        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {

    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.NONE;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return -1;
    }
}
