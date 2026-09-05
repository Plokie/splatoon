package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.customitems.CustomItem;
import com.plokie.helpers.Helpers;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.TeamSelector;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

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

            PlayerStats.resetMatchStats(player);
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
