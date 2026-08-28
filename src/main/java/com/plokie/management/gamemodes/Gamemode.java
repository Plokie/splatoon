package com.plokie.management.gamemodes;

import com.plokie.management.GameFlowManager;
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

    public Gamemodes toEnum()
    {
        return Gamemodes.valueOf(getClass().getSimpleName());
    }
}
