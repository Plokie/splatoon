package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.helpers.*;
import com.plokie.interfaces.IInkablePayloadBlock;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.PlayerStats;
import com.plokie.management.gameflow.IGameState;
import com.plokie.management.gameflow.Overtime;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import com.plokie.management.maps.PayloadMap;
import com.plokie.moving_blocks.MovingBlocksEntity;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Tuple;
import net.minecraft.world.BossEvent;
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
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;

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

        CommandBuilder.command("payload").subcommand("override_most_ink").argumentInteger("team_index").executes(ctx->{
            int teamLeader = ctx.getArgumentInteger("team_index");
            overrideTeamWithMostInk = teamLeader;

            return "Force set team leader to " + overrideTeamWithMostInk;
        }).register();

        CommandBuilder.command("payload").subcommand("query").subcommand("most_ink").executes(ctx->{
            String returnMessage = "Payload team leaders:\n";
            int payloadIdx = 0;
            for(PayloadInstance payload : payloads) {
                int teamLeader = calculateTeamWithMostInk(payload, true);
                int trueTeamLeader = calculateTeamWithMostInk(payload, false);
                returnMessage += "payload " + payloadIdx +": " + teamLeader+" (" + trueTeamLeader+" without override)\n";

                payloadIdx++;
            }

            return returnMessage;
        }).register();

        CommandBuilder.command("payload").subcommand("query").subcommand("furthest_distances").executes(ctx->{
            String returnMessage = "Furthest distances:\n";
            for(var teamIndexToDistance : furthestDistances.entrySet()) {
                int relativeDistance = calculateTeamFurthestDistance(teamIndexToDistance.getKey());
                returnMessage += "team " + teamIndexToDistance.getKey() + ": " + teamIndexToDistance.getValue()+" / " + relativeDistance + "\n";
            }

            return returnMessage;
        }).register();

        CommandBuilder.command("payload").subcommand("query").subcommand("altspawn_indices").executes(ctx->{
            String returnMessage = "Altspawn indices:\n";
            for(var teamIndexToDistance : furthestDistances.entrySet()) {
                int altSpawnIndex = calculateAltSpawnIndex(teamIndexToDistance.getKey());
                returnMessage += "team " + teamIndexToDistance.getKey() + ": " + altSpawnIndex + "\n";
            }

            return returnMessage;
        }).register();
    }

    @Override
    public String getName() { return "Payload"; }

    @Override
    public int getNumTeams() { return 2; }

    List<Entity> route = new ArrayList<>();

    List<PayloadInstance> payloads = new ArrayList<>();
    Map<Integer, Integer> furthestDistances = new HashMap<>();

    int overrideTeamWithMostInk = -1;

    CustomBossEvent getTeam0Bossbar() {
        ResourceLocation barId = ResourceLocation.fromNamespaceAndPath("minecraft", "side1");
        CustomBossEvent bar = Splatoon.SERVER.getCustomBossEvents().get(barId);
        if (bar == null) {
            bar = Splatoon.SERVER.getCustomBossEvents().create(barId, Component.literal("side1"));
        }

        return bar;
    }

    CustomBossEvent getTeam1Bossbar() {
        ResourceLocation barId = ResourceLocation.fromNamespaceAndPath("minecraft", "side2");
        CustomBossEvent bar = Splatoon.SERVER.getCustomBossEvents().get(barId);
        if (bar == null) {
            bar = Splatoon.SERVER.getCustomBossEvents().create(barId, Component.literal("side2"));
        }

        return bar;
    }

    int calculateAltSpawnIndex(int teamIndex)
    {
        if(route.size() == 0) return 0;

        int numNodes = route.size();
        int midpoint = Math.round(numNodes * 0.5f);
        int halfmidpoint = Math.round(midpoint * 0.5f);

        if(teamIndex == 0) {
            return midpoint + halfmidpoint;
        }
        if(teamIndex == 1) {
            return midpoint - halfmidpoint;
        }

        return 0;
    }

    void setTeamSpawnpoint(int teamIndex, Vec3 spawnpoint)
    {
        //Splatoon.LOGGER.info("Set team {} spawnpoint to {}", teamIndex, spawnpoint);

        for(Player player : Splatoon.gameFlowManager.getTeamPlayers(teamIndex))
        {
            ServerPlayer.RespawnConfig respawnConfig = new ServerPlayer.RespawnConfig(ServerLevel.OVERWORLD, Helpers.toBlockPos(spawnpoint), 0.0f, true);
            ((ServerPlayer)player).setRespawnPosition(respawnConfig, false);

            //((ServerPlayer)player).sendSystemMessage(Component.literal("Your teams spawn has moved"));
        }
    }

    int calculateTeamFurthestDistance()
    {
        int furthest = -1;
        int teamFurthest = -1;

        int middleIndex = Math.round(route.size() * 0.5f);

        for(var teamIdxToDistance : furthestDistances.entrySet()) {
            int distance = Math.abs(middleIndex - teamIdxToDistance.getValue());
            if(distance > furthest) {
                furthest = distance;
                teamFurthest = teamIdxToDistance.getKey();
            }
        }

        return teamFurthest;
    }

    int calculateTeamFurthestDistance(int teamIdx) {
        if(!furthestDistances.containsKey(teamIdx)) return -1;

        int middleIndex = Math.round(route.size() * 0.5f);
        int currentNode = furthestDistances.get(teamIdx);
        int distance = Math.abs(middleIndex - currentNode);
        return distance;
    }

    int calculateTeamWithMostInk(PayloadInstance payloadInstance, boolean allowOverride)
    {
        if(allowOverride && overrideTeamWithMostInk>=0) {
            return overrideTeamWithMostInk;
        }

        int numWool = payloadInstance.slimeBoxes.size();
        int minRequiredToMove = numWool / 2;
        int teamWithMostInked = -1;

        for(int teamIdx=0; teamIdx<getNumTeams(); teamIdx++) {
            for(Player player : Splatoon.gameFlowManager.getTeamPlayers(teamIdx)) {
                IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
                if(team != null) {
                    int teamInked = 0;

                    for(var slimeAndOffset : payloadInstance.slimeBoxes) {
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

        return teamWithMostInked;
    }

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

            double dist = Math.sqrt(blockDisplay.distanceToSqr(team0spawn));
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
            for (Display.BlockDisplay otherNav : Splatoon.SERVER.overworld().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(nearestNav.getOnPos()).inflate(50.0))) {
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

        furthestDistances.put(0, middleIndex);
        furthestDistances.put(1, middleIndex);

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

            ScheduleEvent.schedule(60, server->createPayloadAndRoute());
        }

        if(gameState == GameFlowManager.GameState.GAME_TIME)
        { // during
            for(int i=0; i<getNumTeams(); i++) {
                CustomBossEvent bossbar;
                switch (i) {
                    case 0:
                        bossbar = getTeam0Bossbar();
                        break;
                    case 1:
                        bossbar = getTeam1Bossbar();
                        break;
                    default:
                        continue;
                }

                IPlayerTeamMixin team = gameFlowManager.getTeamMixinFromTeamIndex(i);
                if(team != null) {
                    PlayerTeam playerTeam = (PlayerTeam)team;

                    MutableComponent bossbarName = playerTeam.getFormattedDisplayName();
                    bossbar.setName(bossbarName.append(" furthest distance"));

                    try {
                        BossEvent.BossBarColor bossBarColor = BossEvent.BossBarColor.valueOf(team.getBossbarColour().toUpperCase());
                        bossbar.setColor(bossBarColor);

                    } catch (IllegalArgumentException ignored) {}


                }
            }
        }

        if(gameState == GameFlowManager.GameState.CELEBRATION)
        {
            for(int i=0; i<getNumTeams(); i++) {
                CustomBossEvent bossbar;
                switch (i) {
                    case 0:
                        bossbar = getTeam0Bossbar();
                        break;
                    case 1:
                        bossbar = getTeam1Bossbar();
                        break;
                    default:
                        continue;
                }

                bossbar.setVisible(false);
            }
        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        { // results
            for(int i=0; i<getNumTeams(); i++) {
                CustomBossEvent bossbar;
                switch (i) {
                    case 0:
                        bossbar = getTeam0Bossbar();
                        break;
                    case 1:
                        bossbar = getTeam1Bossbar();
                        break;
                    default:
                        continue;
                }

                bossbar.setVisible(false);
            }
        }

        if(gameState == GameFlowManager.GameState.NONE)
        { // cleanup
            for(int i=0; i<getNumTeams(); i++) {
                CustomBossEvent bossbar;
                switch (i) {
                    case 0:
                        bossbar = getTeam0Bossbar();
                        break;
                    case 1:
                        bossbar = getTeam1Bossbar();
                        break;
                    default:
                        continue;
                }

                bossbar.setVisible(false);
            }

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




        int teamWithMostInked = calculateTeamWithMostInk(payload, true);
        IPlayerTeamMixin playerTeam = Splatoon.gameFlowManager.getTeamMixinFromTeamIndex(teamWithMostInked);
        if(playerTeam != null) {
            int intCol = playerTeam.getTeamColourInt();
            for(var blockDisplayToPos : payload.entity.getDisplayEntities())
            {
                blockDisplayToPos.getA().setGlowingTag(true);
                blockDisplayToPos.getA().setGlowColorOverride(intCol);
            }
        }

        int nextIdx = currentIdx;
        int nextDiff = 0;

        switch(teamWithMostInked) {
            case 0: nextDiff = 1; break;
            case 1: nextDiff = -1; break;
        }
        nextIdx += nextDiff;

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
            int intCol = CommonColors.WHITE;
            for(var blockDisplayToPos : payload.entity.getDisplayEntities())
            {
                blockDisplayToPos.getA().setGlowingTag(true);
                blockDisplayToPos.getA().setGlowColorOverride(intCol);
            }
        }
        else
        {
            Entity next = route.get(nextIdx);

            entity.lookAt(EntityAnchorArgument.Anchor.EYES, next.position());

            double speed = 0.08;
            int furthestDistance = furthestDistances.getOrDefault(teamWithMostInked, 0);
            if(nextDiff == 1) {
                if(nextIdx < furthestDistance) {
                    speed = 0.18;
                }
            }
            else if(nextDiff == -1) {
                if(nextIdx > furthestDistance) {
                    speed = 0.18;
                }
            }

            entity.setPos(entity.position().add(entity.getForward().scale(speed)));

            if(teamWithMostInked == 0) {
//                entity.rotate(Rotation.CLOCKWISE_180);
                entity.lookAt(EntityAnchorArgument.Anchor.EYES, entity.position().subtract(entity.getForward()));
            }

            payload.entity.updateRotation();

            float distance = (float) Math.sqrt(entity.distanceTo(next));
            //Splatoon.LOGGER.info("Distance to next nav point: {}", distance);
            if(distance < 0.5f) {
                entity.setPos(next.position());
                payload.visitedIndex = nextIdx;

                if(nextDiff == 1) {
                    if(payload.visitedIndex > furthestDistance) {
                        furthestDistances.put(teamWithMostInked, payload.visitedIndex);
                    }
                }
                else if(nextDiff == -1) {
                    if(payload.visitedIndex < furthestDistance) {
                        furthestDistances.put(teamWithMostInked, payload.visitedIndex);
                    }
                }

                GamemodeMap map = Splatoon.gameFlowManager.getCurrentMap();
                if(map instanceof PayloadMap payloadMap) {
                    {
                        int altSpawnIndex = calculateAltSpawnIndex(0);
                        if(payload.visitedIndex >= altSpawnIndex) {
                            // alt spawn
                            setTeamSpawnpoint(0, payloadMap.altSpawns.get(0));
                        }
                        else
                        {
                            setTeamSpawnpoint(0, payloadMap.teamSpawns.get(0));
                        }
                    }

                    {
                        int altSpawnIndex = calculateAltSpawnIndex(1);

                        if(payload.visitedIndex <= altSpawnIndex) {
                            // alt spawn
                            setTeamSpawnpoint(1, payloadMap.altSpawns.get(1));
                        }
                        else
                        {
                            setTeamSpawnpoint(1, payloadMap.teamSpawns.get(1));
                        }
                    }
                }
                else
                {
                    Splatoon.LOGGER.error("Current map is not a payload map!");
                }
            }
        }

        payload.entity.tick();
    }

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer)
    {


        //Splatoon.LOGGER.info("tick payload");
        if(
                gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.GAME_TIME ||
                gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.OVERTIME
        )
        {
            payloads.forEach(this::payloadTick);

            for(int i=0; i<getNumTeams(); i++) {
                CustomBossEvent bossbar;
                switch(i) {
                    case 0: bossbar = getTeam0Bossbar(); break;
                    case 1: bossbar = getTeam1Bossbar(); break;
                    default: continue;
                }

                int distance = calculateTeamFurthestDistance(i);
                int middleIndex = Math.round(route.size() * 0.5f);
                bossbar.setMax(middleIndex);
                bossbar.setValue(distance);

                List<ServerPlayer> serverPlayers = new ArrayList<>();
                for(Player player : gameFlowManager.getGamersIncludingSpectators()) {
                    serverPlayers.add((ServerPlayer) player);
                }

                bossbar.setPlayers(serverPlayers);
                bossbar.setVisible(true);
            }
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.OVERTIME)
        {
            if(timer == 1) {
                int teamInLead = calculateTeamFurthestDistance();
                gameFlowManager.setWinningTeam(teamInLead);
            }

            IGameState gameState = gameFlowManager.getCurrentGameState().getGameState();
            if(gameState instanceof Overtime overtime) {
                boolean doReplenish = Splatoon.SERVER.getTickCount() % 2 == 0;

                if(doReplenish) {
                    for(PayloadInstance payloadInstance : payloads) {
                        int teamInLead = calculateTeamFurthestDistance();
                        int teamCurrentlyMostInk = calculateTeamWithMostInk(payloadInstance, true);
                        IPlayerTeamMixin teamMixinInLead = gameFlowManager.getTeamMixinFromTeamIndex(teamInLead);

                        if(teamInLead != teamCurrentlyMostInk) {
                            // replenish timer
                            gameFlowManager.addTimer(3);
                        }

                        // if someone on losing team is near the payload
                        // make timer go down slower
                        Entity rootPayloadEntity = payloadInstance.entity.getRootEntity();
                        if(rootPayloadEntity != null) {
                            for (Player player : rootPayloadEntity.level().getEntitiesOfClass(Player.class, new AABB(rootPayloadEntity.getOnPos()).inflate(16.0))) {
                                IPlayerTeamMixin playerTeam = Teams.getTeamMixinFromPlayer(player);
                                if(playerTeam == null) continue;

                                if(teamMixinInLead != playerTeam) {
                                    gameFlowManager.addTimer(1);
                                }
                            }
                        }
                    }
                }
            }
            else {
                Splatoon.LOGGER.error("Current game state is overtime, but the instance of the game state isnt of overtime type");
            }
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.CELEBRATION)
        {
            clearStepMap();
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.RESULTS)
        {
            Splatoon.gameFlowManager.setGameState(GameFlowManager.GameState.CELEBRATION);
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.GAME_TIME)
        {
            if(timer == 1) {
                int teamInLead = calculateTeamFurthestDistance();
                gameFlowManager.setWinningTeam(teamInLead);

                for(PayloadInstance payloadInstance : payloads) {

                    int teamCurrentlyMostInk = calculateTeamWithMostInk(payloadInstance, true);

                    if(teamInLead != teamCurrentlyMostInk) {
                        Splatoon.gameFlowManager.setGameState(GameFlowManager.GameState.OVERTIME);
                    }

                }
            }
        }


    }
}
