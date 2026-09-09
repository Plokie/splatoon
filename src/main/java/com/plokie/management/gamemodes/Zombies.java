package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.management.GameFlowManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.bossevents.CustomBossEvent;

public class Zombies extends Gamemode {
    @Override
    public String getName() {
        return "Zombies";
    }

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer) {
           if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.GAME_TIME) {
               gameFlowManager.addTimer(1); // timer never decays
               zombiesTick(gameFlowManager);
           }
    }

    CustomBossEvent getBossbar() {
        ResourceLocation barId = ResourceLocation.fromNamespaceAndPath("minecraft", "zombies_round");
        CustomBossEvent bar = Splatoon.SERVER.getCustomBossEvents().get(barId);
        if (bar == null) {
            bar = Splatoon.SERVER.getCustomBossEvents().create(barId, Component.literal("zombies_round"));
        }

        return bar;
    }

    void zombiesTick(GameFlowManager gameFlowManager)
    {

    }

    @Override
    public int getNumTeams() {
        return 1;
    }
}
