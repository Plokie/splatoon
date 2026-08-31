package com.plokie.management.gamemodes;

import com.plokie.Splatoon;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.GameFlowManager;
import com.plokie.management.maps.GamemodeMap;
import com.plokie.management.maps.GamemodeMaps;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Payload extends Gamemode {

    public Payload()
    {
        intro.add("Ink the payload to get it to move for your team");
        intro.add("Whoever moves the payload the furthest, wins!");
        intro.add("Get it all the way to the end to win instantly");
        intro.add("The payload will move faster on a return journey-");
        intro.add("-(as long as you move with it)");

        maps.add(GamemodeMaps.Goldrush);
    }

    @Override
    public String getName() { return "Payload"; }

    @Override
    public int getNumTeams() { return 2; }

    List<Entity> route = new ArrayList<>();
    List<Entity> payloads = new ArrayList<>();

    @Override
    public void onGameStateChange(GameFlowManager gameFlowManager, GameFlowManager.GameState gameState)
    {
        super.onGameStateChange(gameFlowManager, gameState);

        GamemodeMap map = gameFlowManager.getCurrentMap();
        Vec3 team0spawn = map.teamSpawns.get(0);

        if(gameState == GameFlowManager.GameState.INTRO)
        { // init
            route.clear();

            // find nearest payload route node to team 0 spawn
            // iterate if node:
            //  add node to list
            //  get next nearest node within like 10 blocks (that hasnt already been visited)
            // #now we have a list of nodes from team 0 to team 1
            // spawn payload at the middle node

            // find nearest payload route node to team 0 spawn
            Display.BlockDisplay nearestNav = null;
            double nearestDist = 999999.0;
            for (Display.BlockDisplay blockDisplay : Splatoon.SERVER.overworld().getEntitiesOfClass(Display.BlockDisplay.class, new AABB(Helpers.toBlockPos(team0spawn)).inflate(300.0))) {
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
            int middleIndex = (int)Math.ceil(route.size() * 0.5f);



            // payload keeps track of the most recent "visited" node index
            // if team 0 is in control, the payload should travel up the list
            // if team 1 is in control, the payload should move down the list

        }

        if(gameState == GameFlowManager.GameState.GAME_TIME)
        { // during

        }

        if(gameState == GameFlowManager.GameState.RESULTS)
        { // results

        }

        if(gameState == GameFlowManager.GameState.NONE)
        { // cleanup
            for(Entity payload : payloads) {

                killPassengersRecur(payload);
                payload.discard();
            }
            payloads.clear();
        }
    }

    void killPassengersRecur(Entity entity) {
        for(Entity passenger : entity.getPassengers())
        {
            killPassengersRecur(passenger);
            passenger.discard();
        }
    }

    void payloadTick(Entity payload)
    {

    }

    @Override
    public void tick(GameFlowManager gameFlowManager, int timer)
    {
        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.GAME_TIME)
        {
            payloads.forEach(this::payloadTick);
        }

        if(gameFlowManager.getCurrentGameState() == GameFlowManager.GameState.RESULTS)
        {
            clearStepMap();
        }
    }
}
