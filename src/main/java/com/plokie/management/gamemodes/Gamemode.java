package com.plokie.management.gamemodes;

import com.plokie.management.GameFlowManager;
import com.plokie.management.maps.GamemodeMaps;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public abstract class Gamemode {

    public abstract String getName();
    public abstract void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState);
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
}
