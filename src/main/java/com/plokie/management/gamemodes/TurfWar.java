package com.plokie.management.gamemodes;

import com.plokie.management.GameFlowManager;

public class TurfWar extends Gamemode {

    @Override
    public String getName() { return "Turf War"; }

    @Override
    public void onGameStateChange(GameFlowManager.GameState gameState)
    {
        if(gameState == GameFlowManager.GameState.GAME_TIME)
        {

        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        {
            
        }
    }

    @Override
    public void tick()
    {

    }
}
