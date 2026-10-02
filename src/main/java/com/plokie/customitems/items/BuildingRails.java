package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.Fill;
import com.plokie.helpers.Helpers;
import com.plokie.helpers.Teams;
import com.plokie.interfaces.IPlayerTeamMixin;
import com.plokie.management.PlayerStats;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class BuildingRails extends ICustomItem {
//    static class PlacementInProgress {
//        UUID playerUUID;
//        BlockPos pos;
//    }
    public static void dataCallback(Player player, ItemStack item)
    {
        Block wallBlock = Blocks.RAIL;
        ResourceLocation model = BuiltInRegistries.BLOCK.getKey(wallBlock);
        item.set(DataComponents.ITEM_MODEL, model);
    }

    static class PlacedRails {
        UUID ownerUUID;
//        BlockPos startPos;
//        BlockPos endPos;
        List<BlockPos> nodes = new ArrayList<>();
        //List<BlockPos> allRailPositions = new ArrayList<>();
    }

    static HashMap<UUID, PlacedRails> s_placedRails = new HashMap<>();
//    static HashMap<UUID, PlacementInProgress> s_railsBeingPlaced = new HashMap<>();
    static boolean hasSetupTick = false;

    public void tick(MinecraftServer server)
    {

    }

    public void placeRails(Level level , BlockPos bpa, BlockPos bpb)
    {
        Vec3 a = bpa.getCenter();
        Vec3 b = bpb.getCenter();

        BlockGetter.traverseBlocks(a, b, null, (context, bp)->{
            BlockPos bpDown = new BlockPos(bp.getX(), bp.getY() - 1, bp.getZ());

            if(!level.getBlockState(bp).is(Blocks.AIR)) {
                return null;
            }
            if(level.getBlockState(bpDown).is(Blocks.AIR)) {
                return null;
            }

            level.setBlockAndUpdate(bp, Blocks.RAIL.defaultBlockState());
            return null;
        }, (ctx)->{return null;});

//        float length = (float)a.distanceTo(b);
//        Vec3 dir = b.subtract(a).normalize();
//
//        for(float i=0; i < length; i += 0.1f)
//        {
//            Vec3 p = a.add(dir.scale(i));
//            BlockPos bp = Helpers.toBlockPos(p);
//            BlockPos bpDown = Helpers.toBlockPos(p.subtract(0, 1, 0));
//
////            boolean doPlace = true;
//
//            if(!level.getBlockState(bp).is(Blocks.AIR)) {
//                continue;
//            }
//            if(level.getBlockState(bpDown).is(Blocks.AIR)) {
//                continue;
//            }
//
//            level.setBlockAndUpdate(bp, Blocks.RAIL.defaultBlockState());
//        }
    }

    public boolean plotRailLineFunc(Player player, BlockPos bpa, BlockPos bpb)
    {
//        int numStoredRails = player.getItemBySlot(EquipmentSlot.MAINHAND).getCount();
        int numStoredRails = 32;

        Vec3 a = bpa.getCenter();
        Vec3 b = bpb.getCenter();

        float length = (float)a.distanceTo(b);
        Vec3 dir = b.subtract(a).normalize();
        boolean isInvalid = false;
        int dustCol = CommonColors.GREEN;



        for(float i=0; i < length; i += 0.5f)
        {
            Vec3 p = a.add(dir.scale(i));
            BlockPos bp = Helpers.toBlockPos(p);
            BlockPos bpDown = Helpers.toBlockPos(p.subtract(0, 1, 0));

            if(i > numStoredRails) {
                isInvalid = true;
                dustCol = CommonColors.RED;
            }

            if(i > 1) {
                if(!player.level().getBlockState(bp).is(Blocks.AIR)) {
                    isInvalid = true;
                    dustCol = CommonColors.RED;
                }
                if(player.level().getBlockState(bpDown).is(Blocks.AIR)) {
                    isInvalid = true;
                    dustCol = CommonColors.RED;
                }
            }

            DustParticleOptions dustParticleOptions = new DustParticleOptions(dustCol, 1);

            ((ServerLevel)player.level()).sendParticles(
                    dustParticleOptions,
                    p.x, p.y, p.z,
                    1, // count
                    0.0, 0.0, 0.0, // delta
                    10.0 // speed
            );
        }

        return !isInvalid;
    }

    public void whileHeld(Player player)
    {
        UUID playerUUID = player.getUUID();

        if(s_placedRails.containsKey(playerUUID))
        {
            PlacedRails rails = s_placedRails.get(playerUUID);

            List<BlockPos> nodes = rails.nodes;

            for(int i=0; i<nodes.size()-1; i++)
            {
                BlockPos nodeA = nodes.get(i);
                BlockPos nodeB = nodes.get(i+1);

                Vec3 a = nodeA.getCenter();
                Vec3 b = nodeB.getCenter();

                //lineFunc.accept(a, b);
                Helpers.particleLine(player.level(), a, b, ParticleTypes.ELECTRIC_SPARK, 0.5f, player);
            }

            BlockHitResult raycast = (BlockHitResult) player.pick(4.5f, 0.0f, false);

            if(raycast.getType() == HitResult.Type.BLOCK)
            {
                if(!nodes.isEmpty()) {
                    BlockPos lastNode = nodes.getLast();
//                    Vec3 lastPos = lastNode.getCenter();

    //                BlockPos hitBlock = Helpers.toBlockPos(raycast.getLocation());
                    BlockPos hitBlock = Helpers.calculateDesiredPlacement(raycast);
//                    Vec3 hitPos = hitBlock.getCenter();

//                    boolean isValid = lineFunc.apply(lastPos, hitPos);
                    boolean isValid = plotRailLineFunc(player, lastNode, hitBlock);

    //                Helpers.particleLine(player.level(), lastPos, hitPos, ParticleTypes.ELECTRIC_SPARK, 0.5f, player);
                }
            }
        }
    }

    public void onUseBlock(Player player, BlockHitResult hit)
    {
        if(!hasSetupTick) {
            Splatoon.LOGGER.info("BuildingBlock first tick setup");
            ServerTickEvents.START_SERVER_TICK.register(this::tick);
            hasSetupTick = true;
        }

        int numStoredRails = player.getItemBySlot(EquipmentSlot.MAINHAND).getCount();

        UUID playerUUID = player.getUUID();

        BlockPos placePos = Helpers.calculateDesiredPlacement(hit);

        if(!s_placedRails.containsKey(playerUUID))
        {
            s_placedRails.put(playerUUID, new PlacedRails());
        }

        BlockPos lastNodePos = placePos;

        boolean isValidPlacement = s_placedRails.get(playerUUID).nodes.isEmpty();
        if(!isValidPlacement) {
            lastNodePos = s_placedRails.get(playerUUID).nodes.getLast();
            isValidPlacement = plotRailLineFunc(player, lastNodePos, placePos);
        }

        if(isValidPlacement) {
            if(s_placedRails.get(playerUUID).nodes.isEmpty())
            {
                player.level().setBlockAndUpdate(placePos, Blocks.RAIL.defaultBlockState());
            }
            else {
                placeRails(player.level(), lastNodePos, placePos);
            }

            s_placedRails.get(playerUUID).nodes.add(placePos);
        }

    }
}
