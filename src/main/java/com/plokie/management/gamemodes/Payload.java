package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.helpers.*;
import com.plokie.interfaces.IInkablePayloadBlock;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import com.plokie.moving_blocks.MovingBlocksEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Payload extends Gamemode {
    static class PayloadInstance {
        public final MovingBlocksEntity entity;
        public int visitedIndex;
        public final List<Tuple<UUID, Vec3>> slimeBoxes;

        public PayloadInstance(MovingBlocksEntity entity, int visitedIndex, List<Tuple<UUID, Vec3>> slimeBoxes)
        {
            this.entity = entity;
            this.visitedIndex = visitedIndex;
            this.slimeBoxes = slimeBoxes;
        }
    }

    public Payload()
    {
        intro.add("Ink the payload to get it to move for your team");
        intro.add("Whoever moves the payload the furthest, wins!");
        intro.add("Get it all the way to the end to win instantly");
        intro.add("The payload will move faster on a return journey-");
        intro.add("-(as long as you move with it)");

        maps.add(GamemodeMaps.Goldrush);

        rewards.put(PlayerStats.BLOCKS_INKED, 1.0f / 1250.0f);
        rewards.put(PlayerStats.AMOUNT_HEALED, 1.0f / 25.0f);
        rewards.put(PlayerStats.PLAYER_KILLS, 4.0f);
        rewards.put(PlayerStats.DAMAGE_DEALT, 1.0f / 200.0f);
        rewards.put(PlayerStats.PAYLOAD_INKED, 1.0f / 50.0f);
    }

    @Override
    public String getName() { return "Payload"; }

    @Override
    public int getNumTeams() { return 2; }

    List<Entity> route = new ArrayList<>();

    List<PayloadInstance> payloads = new ArrayList<>();

    void createPayloadAndRoute()
    {
        route.clear();

        GamemodeMap map = Splatoon.gameFlowManager.getCurrentMap();
        Vec3 team0spawn = map.teamSpawns.get(0);

        // find nearest payload route node to team 0 spawn
        // iterate if node:
        //  add node to list
        //  get next nearest node within like 10 blocks (that hasnt already been visited)
        // #now we have a list of nodes from team 0 to team 1
        // spawn payload at the middle node

        // find nearest payload route node to team 0 spawn
        Display nearestNav = null;
        double nearestDist = 999999.0;
        for (Display blockDisplay : Splatoon.SERVER.overworld().getEntitiesOfClass(Display.class, new AABB(Helpers.toBlockPos(team0spawn)).inflate(300.0))) {
            //Splatoon.LOGGER.info("Check display at {}", blockDisplay.getPosition(0.0f));
            if(!blockDisplay.getTags().contains("PayloadNav")) continue;

            double dist = blockDisplay.distanceToSqr(team0spawn);
            if(dist < nearestDist) {
                nearestDist = dist;
                nearestNav = blockDisplay;
            }
        }

        if(nearestNav == null) {
            Splatoon.LOGGER.error("Could not find a valid block display with tag PayloadNav clost to team0 spawn");
            return;
        }

        // iterate if node:
        //  add node to list
        //  get next nearest node within like 10 blocks (that hasnt already been visited)
        int failsafe = 1000;
        while(nearestNav != null) {
            route.add(nearestNav);

            Display.BlockDisplay nearestOtherNav = null;
            double nearestOtherDist = 99999999.0;
            for (Display.BlockDisplay otherNav : Splatoon.SERVER.overworld().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(nearestNav.getOnPos()).inflate(10.0))) {
                if(!otherNav.getTags().contains("PayloadNav")) continue;
                if(route.contains(otherNav)) continue;

                double dist = otherNav.distanceTo(nearestNav);
                if(dist < nearestOtherDist) {
                    nearestOtherDist = dist;
                    nearestOtherNav = otherNav;
                }
            }

            nearestNav = nearestOtherNav;

            if(failsafe-- <= 0){
                Splatoon.LOGGER.error("Had to failsafe break out of nearest nav while finder route builder loop thing");
                break;
            }
        }
        // #now we have a list of nodes from team 0 to team 1

        // the payload spawns at the middle node
        int middleIndex = Math.round(route.size() * 0.5f);

        Entity middleNode = route.get(middleIndex);
        Entity nextNode = route.get(middleIndex - 1);

        Splatoon.LOGGER.info("Determined middle node to be at {}:{} (out of {} total nodes) ", middleIndex, middleNode.getPosition(0.0f), route.size());

        MovingBlocksEntity movingBlocksEntity = MovingBlocksEntity.create(
                Splatoon.SERVER.overworld(),
                new BlockPos(1362, 129, 2473),
                new BlockPos(1366, 132, 2479),
                new Vec3(0, 2.0, 0),
                middleNode.getPosition(0.0f),
                true
        );

        if(movingBlocksEntity == null) {
            Splatoon.LOGGER.error("Failed to create payload entity!!!!! FUCK!!!");
            return;
        }

        movingBlocksEntity.getRootEntity().lookAt(EntityAnchorArgument.Anchor.EYES, nextNode.getPosition(0.0f));
        movingBlocksEntity.updateRotation();


        List<Tuple<UUID, Vec3>> slimeBoxes = new ArrayList<>();
        for(Tuple<Display.BlockDisplay, Vec3> blockAndPos : movingBlocksEntity.getDisplayEntities())
        {
            Vec3 relativePos = blockAndPos.getB();
            Display.BlockDisplay blockDisplay = blockAndPos.getA();

            if(blockDisplay.getBlockState().is(Blocks.WHITE_WOOL)) {
                Slime slime = EntityType.SLIME.create(Splatoon.SERVER.overworld(), EntitySpawnReason.COMMAND);
                if(slime == null) {
                    Splatoon.LOGGER.error("Failed to spawn slime");
                    continue;
                }

                slime.setUUID(UUID.randomUUID());

                slime.setPos(middleNode.getPosition(0.0f));
                slime.setSilent(true);
                slime.setNoAi(true);
                slime.setPersistenceRequired();
                slime.setSize(2, true);

                ((IInkablePayloadBlock)slime).setLinkedBlockDisplay(blockDisplay.getUUID());

                Splatoon.SERVER.overworld().addFreshEntity(slime);

                Effects.givePotionEffect(slime, MobEffects.RESISTANCE, 9999, 200, true);
                Effects.givePotionEffect(slime, MobEffects.INVISIBILITY, 9999, 200, true);
                Effects.givePotionEffect(slime, MobEffects.REGENERATION, 9999, 200, true);
                Effects.givePotionEffect(slime, MobEffects.HEALTH_BOOST, 9999, 200, true);

                slimeBoxes.add(new Tuple<>(slime.getUUID(), relativePos));

                //Splatoon.LOGGER.info("Spawn slime at {}", middleNode.getPosition(0.0f));
            }
        }
        Splatoon.LOGGER.info("Spawned {} slimes total (of {} total blocks)", slimeBoxes.size(), movingBlocksEntity.getDisplayEntities().size());

        payloads.add(new PayloadInstance(movingBlocksEntity, middleIndex, slimeBoxes));
    }

    @Override
    public void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState)
    {
        super.onGameStateChange(gameFlowManager, gameState);

        if(gameState == GameFlowManager.GameState.INTRO)
        { // init
            ScheduleEvent.schedule(10, server->{
                Fill.replace(server.overworld(),
                        new BlockPos(1362, 129, 2473),
                        new BlockPos(1366, 132, 2479),
                        Blocks.WHITE_WOOL,
                        Splatoon.Tags.WALL_BLOCKS
                );
            });

            ScheduleEvent.schedule(20, server->createPayloadAndRoute());
        }

        if(gameState == GameFlowManager.GameState.GAME_TIME)
        { // during

        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        { // results

        }

        if(gameState == GameFlowManager.GameState.NONE)
        { // cleanup
            for(PayloadInstance payload : payloads) {
                for(var slimeUUIDrel : payload.slimeBoxes) {
                    Entity slimeEnitity = Splatoon.SERVER.overworld().getEntity(slimeUUIDrel.getA());
                    if(slimeEnitity == null) continue;

                    slimeEnitity.discard();
                }

                payload.entity.discard();
            }
            payloads.clear();
        }
    }




    // payload keeps track of the most recent "visited" node index
    // if team 0 is in control, the payload should travel up the list
    // if team 1 is in control, the payload should move down the list
    void payloadTick(PayloadInstance payload)
    {
        Entity entity = payload.entity.getRootEntity();
        if(entity == null) {
            Splatoon.LOGGER.warn("Couldnt find root entity for a tick");
            return;
        }

        int currentIdx = payload.visitedIndex;


        //Splatoon.LOGGER.info("tick {} slime boxes", payload.slimeBoxes.size());
        for(Tuple<UUID, Vec3> slimeBox : payload.slimeBoxes) {
            Entity slimeEntity = Splatoon.SERVER.overworld().getEntity(slimeBox.getA());
            if(slimeEntity == null) continue;
            if(!(slimeEntity instanceof Slime slime)) continue;

            Vec3 relativePos = slimeBox.getB();


            Vec3 worldPos = Helpers.localToWorld(entity, relativePos.add(0.5, 0.0, 0.5));
            slime.forceSetRotation(entity.getRotationVector().y, entity.getRotationVector().x);
            slime.teleportTo(worldPos.x, worldPos.y, worldPos.z);

            //Splatoon.LOGGER.info("{}", worldPos);

            for(ServerPlayer player : PlayerLookup.tracking(slime)) {
                player.connection.send(new ClientboundTeleportEntityPacket(
                        slime.getId(),
                        new PositionMoveRotation(slime.position(), slime.getDeltaMovement(), slime.getYRot(), slime.getXRot()),
                        Set.of(),
                        slime.onGround()
                ));
            }

        }



        int numWool = payload.slimeBoxes.size();
        int minRequiredToMove = numWool / 2;
        int teamWithMostInked = -1;

        for(int teamIdx=0; teamIdx<getNumTeams(); teamIdx++) {
            for(Player player : Splatoon.gameFlowManager.getTeamPlayers(teamIdx)) {
                IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
                if(team != null) {
                    int teamInked = 0;

                    for(var slimeAndOffset : payload.slimeBoxes) {
                        UUID slimeUUID = slimeAndOffset.getA();
                        Entity slimeEntity = player.level().getEntity(slimeUUID);
                        if(slimeEntity==null) continue;
                        if(slimeEntity instanceof Slime slime) {
                            IPlayerTeamMixin slimeTeam = ((IInkablePayloadBlock)slime).getTeam();
                            if(slimeTeam == team) {
                                teamInked++;
                            }
                        }
                    }

                    if(teamInked > minRequiredToMove) {
                        teamWithMostInked = teamIdx;
                    }

                    break;
                }

            }
        }

        int nextIdx = currentIdx;

        switch(teamWithMostInked) {
            case 0: nextIdx += 1; break;
            case 1: nextIdx -= 1; break;
        }

        //todo: calculate next idx

        if(nextIdx >= route.size()) {
            // team0 win
            Splatoon.gameFlowManager.setWinningTeam(0);
            Splatoon.gameFlowManager.setGameState(GameFlowManager.GameState.CELEBRATION);
        }
        else if(nextIdx < 0){
            // team1 win
            Splatoon.gameFlowManager.setWinningTeam(1);
            Splatoon.gameFlowManager.setGameState(GameFlowManager.GameState.CELEBRATION);
        }
        else if(nextIdx == currentIdx)
        {
            // tied up
        }
        else
        {
            Entity next = route.get(nextIdx);

            entity.lookAt(EntityAnchorArgument.Anchor.EYES, next.position());

            entity.setPos(entity.position().add(entity.getForward().scale(0.05)));

            if(teamWithMostInked == 0) {
//                entity.rotate(Rotation.CLOCKWISE_180);
                entity.lookAt(EntityAnchorArgument.Anchor.EYES, entity.position().subtract(entity.getForward()));
            }

            payload.entity.updateRotation();

            float distance = (float) Math.sqrt(entity.distanceTo(next));
            //Splatoon.LOGGER.info("Distance to next nav point: {}", distance);
            if(distance < 0.2f) {
                entity.setPos(next.position());
                payload.visitedIndex = nextIdx;
                //Splatoon.LOGGER.info("Set visited index {}", nextIdx);
            }
        }

        payload.entity.tick();
    }

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer)
    {
        //Splatoon.LOGGER.info("tick payload");
        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.GAME_TIME)
        {
            payloads.forEach(this::payloadTick);
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.CELEBRATION)
        {
            clearStepMap();
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.RESULTS)
        {
            Splatoon.gameFlowManager.setGameState(GameFlowManager.GameState.CELEBRATION);
        }
    }
}
