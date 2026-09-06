package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class Gamemode {

    public abstract String getName();
    public void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState)
    {
        if(gameState == GameFlowManager.GameState.INTRO) {
            clearIndex = 0;
        }
    }
    public abstract void tick(GameFlowManager gameFlowManager, int timer);
    public abstract int getNumTeams();

    protected List<String> intro = new ArrayList<>();
    public List<String> getIntroText() { return intro; }
    protected List<GamemodeMaps> maps = new ArrayList<>();
    public List<GamemodeMaps> getMaps() { return maps; }


    public Gamemodes toEnum()
    {
        return Gamemodes.valueOf(getClass().getSimpleName());
    }

    public Map<PlayerStats, Float> rewards = new HashMap<>();

    int clearIndex = 0;
    protected void clearStepMap(){
        clearStepMap(()->{return true;}, ()->{return true;});
    }

    protected void clearStepMap(Supplier<Boolean> stepCallback, Supplier<Boolean> onFinishCallback) {
        GameFlowManager gameFlowManager = Splatoon.gameFlowManager;
        GamemodeMap map = gameFlowManager.getCurrentMap();

        if(clearIndex <= gameFlowManager.getCurrentMap().mapSize.x)
        {
            stepCallback.get();

            for(
                    int y=0;
                    y < (int)gameFlowManager.getCurrentMap().mapSize.y;
                    y++
            )
            {
                Block groundBlock = map.getGroundBlock.apply(y + (int)gameFlowManager.getCurrentMap().mapCorner.y);
                Block wallBlock = map.getWallBlock.apply(y + (int)gameFlowManager.getCurrentMap().mapCorner.y);

                Fill.replace(
                        Splatoon.SERVER.overworld(),
                        Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                        new BlockPos(clearIndex,y,0),
                        new BlockPos(clearIndex,y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                        groundBlock,
                        Splatoon.Tags.GROUND_BLOCKS
                );

                Fill.replace(
                        Splatoon.SERVER.overworld(),
                        Helpers.toBlockPos(gameFlowManager.getCurrentMap().mapCorner),
                        new BlockPos(clearIndex,y,0),
                        new BlockPos(clearIndex,y,(int)gameFlowManager.getCurrentMap().mapSize.z),
                        wallBlock,
                        Splatoon.Tags.WALL_BLOCKS
                );

            }

            clearIndex++;
        }
        else if(clearIndex == (int)gameFlowManager.getCurrentMap().mapSize.x + 1)
        {
            onFinishCallback.get();

            clearIndex++;
        }
    }
}
