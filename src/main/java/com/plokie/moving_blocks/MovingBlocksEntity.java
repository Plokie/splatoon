package com.plokie.moving_blocks;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MovingBlocksEntity {
    public static MovingBlocksEntity create(Level level, BlockPos corner0, BlockPos corner1, Vec3 pivotOffset, Vec3 spawnPos) {
        MovingBlocksEntity movingBlocksEntity = new MovingBlocksEntity();

        Display.BlockDisplay rootEntity = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        if(rootEntity == null) return null;

        rootEntity.setPos(spawnPos);
        rootEntity.setUUID(UUID.randomUUID());
        rootEntity.addTag("MovingBlocksEntity");

        level.addFreshEntity(rootEntity);

        List<Display.BlockDisplay> displayEntities = new ArrayList<>();

        BoundingBox box = BoundingBox.fromCorners(corner0, corner1);

        Splatoon.LOGGER.info("Creating movingblocksentity dim:{},{},{}", box.getXSpan(), box.getYSpan(), box.getZSpan());

        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    BlockState blockState = level.getBlockState(new BlockPos(x,y,z));
                    if(blockState.is(Blocks.AIR)) continue;

                    Display.BlockDisplay displayEntity = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
                    if(displayEntity == null) continue;

                    displayEntity.setUUID(UUID.randomUUID());
                    displayEntity.addTag("MovingBlocksEntity_Child");
                    displayEntity.setPos(spawnPos);

                    displayEntity.setBlockState(blockState);

                    Vector3f relativePosition = new Vector3f(x - box.minX(), y - box.minY(), z - box.maxZ());
                    relativePosition = new Vector3f(
                            relativePosition.x - (((float)box.getXSpan()) * 0.5f),
                            relativePosition.y - (((float)box.getYSpan()) * 0.5f),
                            relativePosition.z + (((float)box.getZSpan()) * 0.5f)
                    );
                    relativePosition = new Vector3f(
                            relativePosition.x + (float)pivotOffset.x - 0.5f,
                            relativePosition.y + (float)pivotOffset.y - 0.5f,
                            relativePosition.z + (float)pivotOffset.z - 0.5f - 1.0f
                    );

                    Transformation transform = new Transformation(
                            relativePosition,
                            null,
                            null,
                            //new Vector3f(1.0f, 1.0f, 1.0f),
                            null
                    );
                    displayEntity.setTransformation(transform);

                    level.addFreshEntity(displayEntity);

                    displayEntity.startRiding(rootEntity, true);

                    displayEntities.add(displayEntity);

                    //

                    Shulker collisionBox = EntityType.SHULKER.create(level, EntitySpawnReason.COMMAND);
                    if(collisionBox == null) {
                        continue;
                    }
                    collisionBox.setNoAi(true);
                    collisionBox.setPos(relativePosition.x + x, relativePosition.y + y, relativePosition.z + z);
                    level.addFreshEntity(collisionBox);
                }
            }
        }


        movingBlocksEntity.rootEntity = rootEntity;
        movingBlocksEntity.displayEntities = displayEntities;


        return movingBlocksEntity;
    }

    public void discard()
    {
        killPassengersRecur(rootEntity);
        rootEntity.discard();
    }

    void killPassengersRecur(Entity entity) {
        for(Entity passenger : entity.getPassengers())
        {
            killPassengersRecur(passenger);
            passenger.discard();
        }

        for(Shulker shulker : collisionBoxes) {
            shulker.discard();
        }
    }

    public Display.BlockDisplay rootEntity;
    List<Display.BlockDisplay> displayEntities = new ArrayList<>();
    List<Shulker> collisionBoxes = new ArrayList<>();
}
