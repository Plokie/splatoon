package com.plokie.management.gameflow;

import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;

public interface IGameState {
    void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap);
    GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap);
    void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap);

    default String getStateMusic() { return ""; }
    GameFlowManager.GameState getDefaultNextState();

    int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap);
}
