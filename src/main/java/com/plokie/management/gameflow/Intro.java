package com.plokie.management.gameflow;

import com.plokie.Splatoon;
import com.plokie.helpers.Effects;
import com.plokie.management.GameFlowManager;
import com.plokie.management.gamemodes.Gamemode;
import com.plokie.management.maps.GamemodeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Intro implements IGameState {
    Entity introGuide = null;

    @Override
    public void onStateEnter(Gamemode currentGamemode, GamemodeMap currentMap) {
        BlockPos readyUpZoneSize = Splatoon.gameFlowManager.readyUpZoneSize;
        BlockPos readyUpZone = Splatoon.gameFlowManager.readyUpZone;

        int zoneSegmentWidth = (int)Math.ceil(readyUpZoneSize.getX() / (float)currentGamemode.getNumTeams());
        for(int i=0; i < currentGamemode.getNumTeams(); i++) {

            BlockPos segmentPos = new BlockPos(readyUpZone.getX() + (zoneSegmentWidth * i) + i, readyUpZone.getY(), readyUpZone.getZ());
            BlockPos segmentSize = new BlockPos(zoneSegmentWidth, readyUpZoneSize.getY(), readyUpZoneSize.getZ() + 1);
            BlockPos halfSegmentSize = new BlockPos((int)(segmentSize.getX() * 0.5f), (int)(segmentSize.getY() * 0.5f), (int)(segmentSize.getZ() * 0.5f));

            //AABB aabb = new AABB(new BlockPos(segmentPos.getX() + halfSegmentSize.getX(), segmentPos.getY() + halfSegmentSize.getY(),  segmentPos.getZ() + halfSegmentSize.getZ()));
            //aabb = aabb.inflate(halfSegmentSize.getX(), halfSegmentSize.getY(), halfSegmentSize.getZ());
//                    AABB aabb = new AABB(segmentPos.getX(), segmentPos.getY(), segmentPos.getZ(), )
            AABB aabb = new AABB(
                    segmentPos.getX(), segmentPos.getY(), segmentPos.getZ(),
                    segmentPos.getX() + segmentSize.getX(), segmentPos.getY() + segmentSize.getY(), segmentPos.getZ() + segmentSize.getZ() + 1
            );

            Splatoon.LOGGER.info("Setup team members {}, min: {} max: {}", i, aabb.getMinPosition(), aabb.getMaxPosition());

            for(Player player : Splatoon.SERVER.overworld().getEntitiesOfClass(Player.class, aabb))
            {
                Splatoon.LOGGER.info("\t Player {}", player.getName().toString());
                Splatoon.gameFlowManager.setPlayerTeam(player, i);
            }
        }


        ServerLevel level = Splatoon.SERVER.overworld();
        introGuide = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        assert introGuide != null;

        introGuide.setPos(currentMap.introStartPosition);
        introGuide.forceSetRotation(currentMap.introStartRotation.x, currentMap.introStartRotation.y);

        level.addFreshEntity(introGuide);
    }

    @Override
    public GameFlowManager.GameState onStateTick(int timer, Gamemode currentGamemode, GamemodeMap currentMap) {
        if(introGuide != null) {
            for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators()) {
//                            ((ServerPlayer)player).setGameMode(GameType.);
                Effects.givePotionEffect(player, MobEffects.INVISIBILITY, 1, 1, true);
                Effects.givePotionEffect(player, MobEffects.WEAKNESS, 1, 100, true);

                if(!player.getItemBySlot(EquipmentSlot.HEAD).is(Items.AIR)) {
                    player.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.HEAD));
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.AIR));
                }

                if(!player.isPassenger()) {
                    player.startRiding(introGuide, true);
                }
            }

            Vec3 forward = introGuide.getForward();
            Vec3 pos = introGuide.getEyePosition();
            forward = new Vec3(pos.x + (forward.x * 0.25f), pos.y + (forward.y * 0.25), pos.z + (forward.z * 0.25));

            introGuide.setPos(forward);
        }

        int duration = calculateDuration(currentGamemode, currentMap);
        if(timer == duration - 1)
        {
            for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
            {
                ServerPlayer serverPlayer = (ServerPlayer)player;

                serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal(currentGamemode.getName())));
            }


        }

        for(int i=0; i<currentGamemode.getIntroText().size(); i++)
        {
            if(timer == duration - ((i+1) * 100))
            {
                for(Player player : Splatoon.gameFlowManager.getGamersIncludingSpectators())
                {
                    ServerPlayer serverPlayer = (ServerPlayer)player;

                    player.level().playSound(
                            player,
                            player.getEyePosition().x, player.getEyePosition().y, player.getEyePosition().z,
                            SoundEvents.EXPERIENCE_ORB_PICKUP,
                            SoundSource.MASTER,
                            0.5f,
                            1.0f
                    );

                    String text = currentGamemode.getIntroText().get(i);
                    serverPlayer.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(text)));
                    serverPlayer.connection.send(new ClientboundSetTitleTextPacket(Component.literal("")));
                }
            }
        }

        return null;
    }

    @Override
    public void onStateExit(Gamemode currentGamemode, GamemodeMap currentMap) {
        if(introGuide != null) {
            introGuide.discard();
            introGuide = null;
        }
    }

    @Override
    public GameFlowManager.GameState getDefaultNextState() {
        return GameFlowManager.GameState.CLASS_SELECT;
    }

    @Override
    public int calculateDuration(Gamemode currentGamemode, GamemodeMap currentMap) {
        return currentGamemode.getIntroText().size() * 100;
    }

    @Override
    public String getStateMusic() { return "music.opening.match_start"; }
}
