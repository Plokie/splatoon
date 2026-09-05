package com.plokie.management.gamemodes;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TurfWar extends Gamemode {

    public TurfWar()
    {
        intro.add("Ink the most turf before the timer runs out");
        intro.add("Whichever team inks the most turf, wins!");
        intro.add("Don't get too caught up with kills! Ink wins the game!");

        maps.add(GamemodeMaps.UrchinUnderpass);
        maps.add(GamemodeMaps.MorayTowers);
        maps.add(GamemodeMaps.Cyberscape);

        scoreboardDisplayEntities.put(0, new ArrayList<>());
        scoreboardDisplayEntities.put(1, new ArrayList<>());
    }

    HashMap<Integer, List<Entity>> scoreboardDisplayEntities = new HashMap<>();

    @Override
    public String getName() { return "Turf War"; }

    @Override
    public int getNumTeams() { return 2; }

    @Override
    public void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState)
    {
        super.onGameStateChange(gameFlowManager, gameState);

        if(gameState == GameFlowManager.GameState.INTRO)
        { // init
            teamScores.clear();
        }

        if(gameState == GameFlowManager.GameState.GAME_TIME)
        { // during

        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        { // results
            float totalWidth = 4.0f;
            float perTeamSeg = totalWidth / getNumTeams();



//            Display.BlockDisplay menuPivot = EntityType.BLOCK_DISPLAY.create(Splatoon.SERVER.overworld(), EntitySpawnReason.COMMAND);
//            assert menuPivot != null;
//            menuPivot.setPos(gameFlowManager.getCurrentMap().resultsPosition);
//            menuPivot.setXRot(gameFlowManager.getCurrentMap().resultsRotation.x);
//            menuPivot.setYRot(gameFlowManager.getCurrentMap().resultsRotation.y);
//            Splatoon.SERVER.overworld().addFreshEntity(menuPivot);

            Vec2 rot = gameFlowManager.getCurrentMap().resultsRotation;

            Vec3 forward = Vec3.directionFromRotation(rot.y, rot.x);
            Vec3 right = Vec3.directionFromRotation(rot.y, rot.x  + 90.0f);
            Vec3 up = Vec3.directionFromRotation(rot.y- 90.0f, rot.x );

            for(int i=0; i<getNumTeams(); i++) {
                float localX = (perTeamSeg * i) + (perTeamSeg*0.5f);
                float localY = 0.0f;
                float localZ = 3.0f;

                Display.BlockDisplay teamBar = EntityType.BLOCK_DISPLAY.create(Splatoon.SERVER.overworld(), EntitySpawnReason.COMMAND);
                if(teamBar != null) {
                    Vec3 pos = gameFlowManager.getCurrentMap().resultsPosition;
                    pos = pos.add(right.scale(localX));
                    //pos = pos.add(up.scale(localY + 2.0f));
                    pos = pos.add(forward.scale(localZ));

                    teamBar.setPos(pos);
                    teamBar.forceSetRotation(rot.x, rot.y);
//                    teamBar.setYRot(rot.y);

                    teamBar.setBlockState(Blocks.WHITE_WOOL.defaultBlockState());

                    teamBar.setTransformation(new Transformation(
                            new Vector3f(-0.5f, -0.5f, -0.5f),
                            null,
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            null
                    ));

                    Splatoon.SERVER.overworld().addFreshEntity(teamBar);

                    scoreboardDisplayEntities.get(i).add(teamBar);
                }
            }
        }

        if(gameState == GameFlowManager.GameState.CELEBRATION)
        { // cleanup
            for(var entry : scoreboardDisplayEntities.entrySet())
            {
                for(Entity entity : entry.getValue()) {
                    entity.discard();
                }
            }
            scoreboardDisplayEntities.clear();

            teamScores.clear();
        }

        if(gameState == GameFlowManager.GameState.NONE)
        { // cleanup
            teamScores.clear();
        }
    }

    Map<Integer, Integer> teamScores = new HashMap<>();

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer)
    {
        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.RESULTS)
        {
            GamemodeMap map = gameFlowManager.getCurrentMap();

            clearStepMap(()->{
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
                            map.groundBlock,
                            playerTeam.getGroundBlock()
                    );

                    count += Fill.replace(
                            Splatoon.SERVER.overworld(),
                            Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                            new BlockPos(clearIndex,0,0),
                            new BlockPos(clearIndex,(int)gameFlowManager.getCurrentMap().mapSize.y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                            map.wallBlock,
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
                return true;
            }, ()->{
                int highestScore = 0;
                int teamHighestScore = -1;
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

                gameFlowManager.setWinningTeam(teamHighestScore);

                gameFlowManager.setGameState(GameFlowManager.GameState.CELEBRATION);

//                String winningTeam = "Error";
//                List<Player> winningTeamMembers = gameFlowManager.getTeamPlayers(teamHighestScore);
//                if(!winningTeamMembers.isEmpty())
//                {
//                    Player player = winningTeamMembers.get(0);
//                    IPlayerTeamMixin teamMixin = Teams.getTeamMixinFromPlayer(player);
//                    if(teamMixin != null) {
//                        PlayerTeam playerTeam = (PlayerTeam)teamMixin;
//                        winningTeam = playerTeam.getName();
//                    }
//                }

//                for(Player player : gameFlowManager.getGamersIncludingSpectators())
//                {
//                    ServerPlayer serverPlayer = (ServerPlayer)player;
//
//                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Winning team: ").append(winningTeam)));
//                }


                return true;
            });
        }
    }
}
