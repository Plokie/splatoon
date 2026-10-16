package com.plokie.customitems.items;

import com.plokie.Splatoon;
import com.plokie.classes.abilities.Ability;
import com.plokie.classes.abilities.AbilityManager;
import com.plokie.customitems.ICustomItem;
import com.plokie.helpers.*;
import com.plokie.interfaces.IPlayerMixin;
import com.plokie.interfaces.IPlayerTeamMixin;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class BuildingBlock extends ICustomItem {
    public static class BuildingBlockInstance {
        public BuildingBlockInstance(ServerPlayer player, BlockPos position, int tickPlaced, Level level)
        {
            this.playerUUID = player.getUUID();
            this.position = position;
            this.tickPlaced = tickPlaced;
            this.level = level;
        }

        UUID playerUUID;
        BlockPos position;
        int tickPlaced;
        Level level;

        int previousDestrStage = 0;
        float crumbleProgress = 0.0f;
    }

    static HashMap<UUID, List<BuildingBlockInstance>> blockInstances = new HashMap<>();
    static boolean hasSetupTick = false;

    final static int blockBaseLifetime = 2400; // number of ticks before natural crumble begins, -1 = no decay
    final static float crumblePerTick = 0.0025f;

    final static int wallHeight = 3;

    public static void dataCallback(Player player, ItemStack item)
    {
        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
        if(team != null) {
            Block wallBlock = team.getAuxBlock();
            ResourceLocation model = BuiltInRegistries.BLOCK.getKey(wallBlock);
            item.set(DataComponents.ITEM_MODEL, model);
        }

        BlockPredicate blockPredicate = BlockPredicate.Builder.block().build();
        AdventureModePredicate canPlaceOn = new AdventureModePredicate(List.of(blockPredicate));
        item.set(
                DataComponents.CAN_PLACE_ON,
                canPlaceOn
        );
    }

    public static List<BuildingBlockInstance> getWallsInRadius(Vec3 pos, float radius)
    {
        List<BuildingBlockInstance> ret = new ArrayList<>();

        for(var entry : blockInstances.entrySet())
        {
            for(BuildingBlockInstance instance : entry.getValue())
            {
                Vec3 wallPos = instance.position.getCenter();

                float dist = (float)pos.distanceTo(wallPos);

                if(dist < radius)
                {
                   ret.add(instance);
                }
            }
        }

        return ret;
    }

    public static void damageWallsInRadius(Vec3 pos, float radius, float damageAmount)
    {
        List<BuildingBlockInstance> walls = getWallsInRadius(pos, radius);
        for(BuildingBlockInstance wall : walls)
        {
            wall.crumbleProgress += damageAmount;
            wall.crumbleProgress = Math.clamp(wall.crumbleProgress, 0.0f, 2.0f);
        }
    }

    public static void damageWall(BlockPos position, float damageAmount)
    {
        for(var entry : blockInstances.entrySet()) {
            for(BuildingBlockInstance wall : entry.getValue()) {
                for(int i=0; i<wallHeight; i++) {
                    BlockPos checkPos = new BlockPos(position.getX(), position.getY() - i, position.getZ());
                    if(wall.position.equals(checkPos))
                    {
                        wall.crumbleProgress += damageAmount;
                        wall.crumbleProgress = Math.clamp(wall.crumbleProgress, 0.0f, 2.0f);


                        break;
                    }
                }
            }
        }
    }

    public void onUseItem(Player player)
    {

    }

    public void addPlacedBlock(Player player, BlockPos pos)
    {
        int tick = Splatoon.SERVER.getTickCount();

        if(!blockInstances.containsKey(player.getUUID()))
        {
            blockInstances.put(player.getUUID(), new ArrayList<>());
        }

        blockInstances.get(player.getUUID()).add(new BuildingBlockInstance((ServerPlayer) player, pos, tick, player.level()));
    }

    void onBlockDestroyed(BuildingBlockInstance blockInstance)
    {
        BlockPos pos = blockInstance.position;

//        Splatoon.LOGGER.info("onBlockDestroyed {} fill {}", pos, wallHeight);
        //BlockPos posAbove = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());

        sendDestructionStage((ServerLevel) blockInstance.level, pos, 15);

        Fill.replace((ServerLevel) blockInstance.level, pos, new BlockPos(pos.getX(), pos.getY() + (wallHeight-1), pos.getZ()), Blocks.AIR, Splatoon.Tags.AUX_BLOCKS);

//        Ability ability = ((IPlayerMixin)blockInstance.player).getAbility(AbilityManager.AbilityEnum.BuildingBlocks);
//        if(ability != null) {
//            int count = ability.getCount();
//            count += 1;
//            if(count <= ability.getMaxCount()) {
//                ability.setCount(count);
//            }
//        }
    }

    void sendDestructionStage(ServerLevel serverLevel, BlockPos pos, int stage)
    {
        for(int i=0; i<wallHeight; i++) {
            BlockPos updatePos = new BlockPos(pos.getX(), pos.getY() + i, pos.getZ());

            ClientboundBlockDestructionPacket packet = new ClientboundBlockDestructionPacket(updatePos.hashCode(), updatePos, stage);

            for (ServerPlayer player : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(updatePos), false)) {
                player.connection.send(packet);
            }
        }
    }

    public void tick(MinecraftServer server)
    {
        int currentTick = server.getTickCount();

        for(var playerEntry : blockInstances.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerEntry.getKey());
            if(player != null) {
                Ability ability = ((IPlayerMixin)player).getAbility(AbilityManager.AbilityEnum.BuildingBlocks);
                if(ability != null) {
                    int count = 24 - playerEntry.getValue().size();

                    int currentCount = ability.getCount();
                    if(currentCount != count) {
                        ability.setCount(count);
                    }

                }
            }


            for(BuildingBlockInstance blockInstance : playerEntry.getValue()) {
                BlockPos pos = blockInstance.position;

                ServerLevel level = (ServerLevel) blockInstance.level;

                boolean isAnyAir = false;
                for(int i=0; i<wallHeight; i++) {
                    if(level.getBlockState(new BlockPos(pos.getX(), pos.getY() + i, pos.getZ())).is(Blocks.AIR))
                    {
                        isAnyAir = true;
                        break;
                    }
                }
                if(isAnyAir)
                {
                    Splatoon.LOGGER.info("Detect manually destroyed BuildingBlock at {}", pos);
                    onBlockDestroyed(blockInstance);
                    playerEntry.getValue().remove(blockInstance);
                    break;
                }

                int totalTimeAlive = currentTick - blockInstance.tickPlaced;
                if(blockBaseLifetime >= 0 && totalTimeAlive > blockBaseLifetime) {
                    blockInstance.crumbleProgress += crumblePerTick;
                }
                //float crumbleProgress = (totalTimeAlive - blockBaseLifetime) / (float)(blockDestrTime);


                int destructionStage = Math.round(blockInstance.crumbleProgress * 9);

                if(destructionStage < 0) continue;

                if(destructionStage >= 10) {
                    level.playSound(
                            null,
                            pos.getX(), pos.getY(), pos.getZ(),
                            SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                            SoundSource.HOSTILE,
                            2.0f,
                            1.0f
                    );

                    Splatoon.LOGGER.info("Destroy BuildingBlock at {}", pos);
                    onBlockDestroyed(blockInstance);
                    playerEntry.getValue().remove(blockInstance);
                    break;
                }
                else if(destructionStage != blockInstance.previousDestrStage)
                {


//                    Splatoon.LOGGER.info("Block {} destr stage change {} to {}", pos, blockInstance.previousDestrStage, destructionStage);

                    blockInstance.previousDestrStage = destructionStage;

                    ServerLevel serverLevel = (ServerLevel)blockInstance.level;

                    sendDestructionStage(serverLevel, pos, destructionStage);
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

//        Splatoon.LOGGER.info("onUseBlock {}", player.getName().getString());

        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
        if(team == null) return;

        Vec3 hitPos = hit.getLocation();
        var coreDir = hit.getDirection();
        Vec3 hitDir = hit.getDirection().getUnitVec3();

        Block auxBlock = team.getAuxBlock();

        BlockPos placePos = Helpers.calculateDesiredPlacement(hit);
        BlockPos placePosBelow = new BlockPos(placePos.getX(), placePos.getY() - 1, placePos.getZ());

        boolean validPlacement = !player.level().getBlockState(placePosBelow).is(Blocks.AIR);
        //if(coreDir == Direction.UP) validPlacement = true;
//        else {
//            if(player.level().getBlockState(hit.getBlockPos()).is(auxBlock)) {
//                validPlacement = true;
//            }
//        }

//        Vec3 newPos = hitPos.add(hitDir.multiply(0.5, 0.5, 0.5));
//        BlockPos newBlockPos = Helpers.toBlockPos(newPos);

        if(!player.level().getBlockState(placePos).is(Blocks.AIR)) {
            validPlacement = false;
        }

        if(validPlacement) {


//            BlockPos blockPosTop = Helpers.toBlockPos(newPos.add(0, wallHeight-1, 0));
            BlockPos blockPosTop = new BlockPos(placePos.getX(), placePos.getY() + (wallHeight-1), placePos.getZ());

    //        BlockState block = team.getAuxBlock().defaultBlockState();

            Fill.replace((ServerLevel)player.level(), placePos, blockPosTop, auxBlock, BlockTags.AIR);

            addPlacedBlock(player, placePos);

            super.onUseBlock(player, hit);
        }
    }
}
