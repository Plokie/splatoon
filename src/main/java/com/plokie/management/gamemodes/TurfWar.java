package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.PlayerTeam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurfWar extends Gamemode {

    public TurfWar()
    {
        intro.add("Ink the most turf before the timer runs out");
        intro.add("Whichever team inks the most turf, wins!");
        intro.add("Don't get too caught up with kills! Ink wins the game!");
    }

    @Override
    public String getName() { return "Turf War"; }

    @Override
    public int getNumTeams() { return 2; }

    @Override
    public void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState)
    {
        if(gameState == GameFlowManager.GameState.INTRO)
        { // init
            teamScores.clear();
        }

        if(gameState == GameFlowManager.GameState.GAME_TIME)
        { // during

        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        { // results

        }

        if(gameState == GameFlowManager.GameState.NONE)
        { // cleanup
            teamScores.clear();
        }
    }

    int clearIndex = 0;
    Map<Integer, Integer> teamScores = new HashMap<>();

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer)
    {
        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.RESULTS)
        {
            if(clearIndex <= gameFlowManager.getCurrentMap().mapSize.x)
            {
                for(int teamIdx=0; teamIdx < gameFlowManager.getCurrentGamemode().getNumTeams(); teamIdx++)
                {
                    List<Player> players = gameFlowManager.getTeamPlayers(teamIdx);
                    if(players.isEmpty()) continue;

                    IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(players.get(0));
                    if(playerTeam == null) continue;

                    int count = Fill.replace(
                            Splatoon.SERVER.overworld(),
                            Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                            new BlockPos(clearIndex,0,0),
                            new BlockPos(clearIndex,(int)gameFlowManager.getCurrentMap().mapSize.y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                            Blocks.CYAN_TERRACOTTA,
                            playerTeam.getGroundBlock()
                    );

                    count += Fill.replace(
                            Splatoon.SERVER.overworld(),
                            Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                            new BlockPos(clearIndex,0,0),
                            new BlockPos(clearIndex,(int)gameFlowManager.getCurrentMap().mapSize.y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                            Blocks.PALE_MOSS_BLOCK,
                            playerTeam.getWallBlock()
                    );

                    if(teamScores.get(teamIdx) == null)
                    {
                        teamScores.put(teamIdx, count);
                    }
                    else {
                        teamScores.put(teamIdx, teamScores.get(teamIdx) + count);
                    }

                }

                Fill.replace(
                        Splatoon.SERVER.overworld(),
                        Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                        new BlockPos(clearIndex,0,0),
                        new BlockPos(clearIndex,(int)gameFlowManager.getCurrentMap().mapSize.y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                        Blocks.CYAN_TERRACOTTA,
                        Splatoon.Tags.GROUND_BLOCKS
                );

                Fill.replace(
                        Splatoon.SERVER.overworld(),
                        Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                        new BlockPos(clearIndex,0,0),
                        new BlockPos(clearIndex,(int)gameFlowManager.getCurrentMap().mapSize.y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                        Blocks.PALE_MOSS_BLOCK,
                        Splatoon.Tags.WALL_BLOCKS
                );

                clearIndex++;
            }
            else if(clearIndex == (int)gameFlowManager.getCurrentMap().mapSize.x + 1)
            {
                int highestScore = 0;
                int teamHighestScore = 0;
                for(int teamIdx=0; teamIdx < gameFlowManager.getCurrentGamemode().getNumTeams(); teamIdx++)
                {
                    if(teamScores.get(teamIdx) != null)
                    {
                        if(teamScores.get(teamIdx) > highestScore)
                        {
                            highestScore = teamScores.get(teamIdx);
                            teamHighestScore = teamIdx;
                        }
                    }
                }

                String winningTeam = "Error";
                List<Player> winningTeamMembers = gameFlowManager.getTeamPlayers(teamHighestScore);
                if(!winningTeamMembers.isEmpty())
                {
                    Player player = winningTeamMembers.get(0);
                    IPlayerTeamMixin teamMixin = Teams.getTeamMixinFromPlayer(player);
                    if(teamMixin != null) {
                        PlayerTeam playerTeam = (PlayerTeam)teamMixin;
                        winningTeam = playerTeam.getName();
                    }
                }


                for(Player player : gameFlowManager.getGamersIncludingSpectators())
                {
                    ServerPlayer serverPlayer = (ServerPlayer)player;

                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Winning team: ").append(winningTeam)));
                }

                clearIndex++;
            }

        }
    }
}
