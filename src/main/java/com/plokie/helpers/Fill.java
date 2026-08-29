package com.plokie.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class Fill {
    public static int replace(Level level, BlockPos point, BlockPos min, BlockPos max, Block block) {
        return replace((ServerLevel)level, point.offset(min), point.offset(max), block);
    }

    public static int replace(ServerLevel level, BlockPos point, BlockPos min, BlockPos max, Block block) {
        return replace(level, point.offset(min), point.offset(max), block);
    }

    public static int replace(ServerLevel level, BlockPos min, BlockPos max, Block block) {
        BoundingBox box = BoundingBox.fromCorners(min, max);
        int blocksReplaced = 0;
        int noUpdatesFlag = Block.UPDATE_CLIENTS;

        BlockState blockState = block.defaultBlockState();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos targetPos = new BlockPos(x, y, z);
                    BlockState currentState = level.getBlockState(targetPos);

                    if (!currentState.is(block)) {

                        level.setBlock(targetPos, blockState, noUpdatesFlag);
                        blocksReplaced++;
                    }

                }
            }
        }

        return blocksReplaced;
    }

    public static int replace(Level level, BlockPos point, BlockPos min, BlockPos max, Block block, Block replaceBlock) {
        return replace((ServerLevel)level, point.offset(min), point.offset(max), block, replaceBlock);
    }

    public static int replace(ServerLevel level, BlockPos point, BlockPos min, BlockPos max, Block block, Block replaceBlock) {
        return replace(level, point.offset(min), point.offset(max), block, replaceBlock);
    }

    public static int replace(ServerLevel level, BlockPos min, BlockPos max, Block block, Block replaceBlock) {
        BoundingBox box = BoundingBox.fromCorners(min, max);
        int blocksReplaced = 0;
        int noUpdatesFlag = Block.UPDATE_CLIENTS;

        BlockState blockState = block.defaultBlockState();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos targetPos = new BlockPos(x, y, z);
                    BlockState currentState = level.getBlockState(targetPos);

                    if (currentState.is(replaceBlock)) {

                        if (!currentState.is(block)) {

                            level.setBlock(targetPos, blockState, noUpdatesFlag);
                            blocksReplaced++;
                        }
                    }
                }
            }
        }

        return blocksReplaced;
    }


    public static int replace(Level level, BlockPos point, BlockPos min, BlockPos max, Block block, TagKey<Block> replaceBlockTag) {
        return replace((ServerLevel)level, point.offset(min), point.offset(max), block, replaceBlockTag);
    }

    public static int replace(ServerLevel level, BlockPos point, BlockPos min, BlockPos max, Block block, TagKey<Block> replaceBlockTag) {
        return replace(level, point.offset(min), point.offset(max), block, replaceBlockTag);
    }

    public static int replace(ServerLevel level, BlockPos min, BlockPos max, Block block, TagKey<Block> replaceBlockTag) {
        BoundingBox box = BoundingBox.fromCorners(min, max);
        int blocksReplaced = 0;
        int noUpdatesFlag = Block.UPDATE_CLIENTS;

        BlockState blockState = block.defaultBlockState();

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockPos targetPos = new BlockPos(x, y, z);
                    BlockState currentState = level.getBlockState(targetPos);

                    if (currentState.is(replaceBlockTag)) {

                        if (!currentState.is(block)) {

                            level.setBlock(targetPos, blockState, noUpdatesFlag);
                            blocksReplaced++;
                        }
                    }
                }
            }
        }

        return blocksReplaced;
    }

}
