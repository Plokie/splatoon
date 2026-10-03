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
    static class BuildingBlockInstance {
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

    final static int blockBaseLifetime = 800; // number of ticks before natural crumble begins
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
                    int count = 16 - playerEntry.getValue().size();

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
                    playerEntry.getValue().remove(blockInstance);
                    onBlockDestroyed(blockInstance);
                    break;
                }

                int totalTimeAlive = currentTick - blockInstance.tickPlaced;
                if(totalTimeAlive > blockBaseLifetime) {
                    blockInstance.crumbleProgress += crumblePerTick;
                }
                //float crumbleProgress = (totalTimeAlive - blockBaseLifetime) / (float)(blockDestrTime);


                int destructionStage = Math.round(blockInstance.crumbleProgress * 9);

                if(destructionStage < 0) continue;

                if(destructionStage >= 10) {
                    Splatoon.LOGGER.info("Destroy BuildingBlock at {}", pos);
                    playerEntry.getValue().remove(blockInstance);
                    onBlockDestroyed(blockInstance);
                    break;
                }
                else if(destructionStage != blockInstance.previousDestrStage)
                {


                    Splatoon.LOGGER.info("Block {} destr stage change {} to {}", pos, blockInstance.previousDestrStage, destructionStage);

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

        Splatoon.LOGGER.info("onUseBlock {}", player.getName().getString());

        IPlayerTeamMixin team = Teams.getTeamMixinFromPlayer(player);
        if(team == null) return;

        Vec3 hitPos = hit.getLocation();
        var coreDir = hit.getDirection();
        Vec3 hitDir = hit.getDirection().getUnitVec3();

        Block auxBlock = team.getAuxBlock();



        boolean validPlacement = false;
        if(coreDir == Direction.UP) validPlacement = true;
        else {
            if(player.level().getBlockState(hit.getBlockPos()).is(auxBlock)) {
                validPlacement = true;
            }
        }

        Vec3 newPos = hitPos.add(hitDir.multiply(0.5, 0.5, 0.5));
        BlockPos newBlockPos = Helpers.toBlockPos(newPos);

        if(!player.level().getBlockState(newBlockPos).is(Blocks.AIR)) {
            validPlacement = false;
        }

        if(validPlacement) {


            BlockPos blockPosTop = Helpers.toBlockPos(newPos.add(0, wallHeight-1, 0));

    //        BlockState block = team.getAuxBlock().defaultBlockState();

            Fill.replace((ServerLevel)player.level(), newBlockPos, blockPosTop, auxBlock, BlockTags.AIR);

            addPlacedBlock(player, newBlockPos);

            super.onUseBlock(player, hit);
        }
    }
}
