package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public class Overtime implements IGameState {
    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
        Splatoon.gameFlowManager.getTimerBossbar().setName(Component.literal("Overtime"));

        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
        {
            ((ServerPlayer)player).connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Overtime!")));
            ((ServerPlayer)player).connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
        }
    }

    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {

    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.CELEBRATION;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return 200;
    }

    @Override
    public String getStateMusic() { return "music.battle.now_or_never"; }
}
