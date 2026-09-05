package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.classes.SplatoonClasses;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Helpers;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class GameTime implements IGameState {
    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
        Splatoon.gameFlowManager.getTimerBossbar().setName(Component.literal("Game time"));

        for(int i=0; i < currentGamemode.getNumTeams(); i++) {
            Vec3 teamSpawn = currentMap.teamSpawns.get(i);
            for(Player player : Splatoon.gameFlowManager.getTeamPlayers(i)) {
                List<String> removeTags = new ArrayList<>();
                for(String tag : player.getTags())
                {
                    if(tag.endsWith("Pick")) {
//                                String klass = tag.substring(0, tag.length() - 4);
                        String klass = tag.replace("Pick", "");
                        Splatoon.LOGGER.info("XPICK {}", klass);

                        try {
                            SplatoonClasses.SplatoonClass pickClass = SplatoonClasses.SplatoonClass.valueOf(klass);
                            ((IPlayerMixin)player).setClass(pickClass);
                        }
                        catch(Exception e)
                        {
                            Splatoon.LOGGER.warn("Unknown pick tag {}", tag);
                        }

                        removeTags.add(tag);
                    }
                }
                // avoids concurrentmodificationexception
                for(String tag : removeTags) {
                    player.removeTag(tag);
                }

                player.teleportTo(teamSpawn.x, teamSpawn.y, teamSpawn.z);

                ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(teamSpawn), 0.0f, true);
                ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);
            }
        }
    }

    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        if(timer == 1200) {

            for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
            {
                Splatoon.gameFlowManager.playSong("music.battle.last_minute", player);

                ServerPlayer serverPlayer = (ServerPlayer)player;
                serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("One minute left!")));
                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
            }
        }

        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {
        AABB mapAABB = new AABB(currentMap.mapCorner, currentMap.mapCorner.add(currentMap.mapSize));
        for(Entity entity : Splatoon.SERVER.overworld().getEntitiesOfClass(Entity.class, mapAABB))
        {
            boolean doKill = false;
            if(entity instanceof Shulker) doKill = true;

            if(entity instanceof Sheep) doKill = true;

            if(entity instanceof Display.BlockDisplay) {
                if(entity.getTags().contains("InkPuck")) doKill = true;
            }

            if(doKill) {
                if(entity instanceof LivingEntity livingEntity)
                {
                    Affects.hurtEntity(livingEntity, 1000.f);
                }
                else
                {
                    entity.discard();
                }
            }
        }
    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.RESULTS;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return 7200;
    }

    @Override
    public String getStateMusic() { return "music.battle.splattack"; }
}
