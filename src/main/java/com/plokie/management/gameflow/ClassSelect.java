package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.helpers.Helpers;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class ClassSelect implements IGameState {
    Map<Integer, Vec3> classSelectSpawns = new HashMap<>();

    public ClassSelect()
    {
        classSelectSpawns.put(0, new Vec3(-135.5,94,-157.5));
        classSelectSpawns.put(1, new Vec3(-126.5,94,-157.5));
    }

    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
        for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
        {
            if(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR)) {
                player.setItemSlot(EquipmentSlot.HEAD, player.getItemBySlot(EquipmentSlot.FEET));
                player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.AIR));
            }
        }

        CustomBossEvent timerBossbar = Splatoon.gameFlowManager.getTimerBossbar();
        timerBossbar.setVisible(true);
        timerBossbar.setName(Component.literal("Class select"));

        for(int i=0; i < currentGamemode.getNumTeams(); i++) {
            Vec3 classSelect = classSelectSpawns.get(i);
            for(Player player : Splatoon.gameFlowManager.getTeamPlayers(i)) {
                player.teleportTo(classSelect.x, classSelect.y, classSelect.z);

                ServerPlayer serverPlayer = ((ServerPlayer)player);

                ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(classSelect), 0.0f, true);
                serverPlayer.setRespawnPosition(respawnConfig, false);

//                        CommandSourceStack source = serverPlayer.getServer().createCommandSourceStack().withSuppressedOutput();
//                        serverPlayer.getServer().getCommands().performPrefixedCommand(source, "bossbar set minecraft:timer visible true");

                timerBossbar.addPlayer(serverPlayer);
            }
        }

        Vec3 spectatorZone = currentMap.spectatorZone;
        for(Player player : Splatoon.gameFlowManager.getSpectators()) {
            player.teleportTo(spectatorZone.x, spectatorZone.y, spectatorZone.z);

            ServerPlayer serverPlayer = ((ServerPlayer)player);

            ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(spectatorZone), 0.0f, true);
            serverPlayer.setRespawnPosition(respawnConfig, false);

            timerBossbar.addPlayer(serverPlayer);
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
        return GameFlowManager.GameState.GAME_TIME;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return 600;
    }

    @Override
    public String getStateMusic() { return "music.lobby.main"; }
}
