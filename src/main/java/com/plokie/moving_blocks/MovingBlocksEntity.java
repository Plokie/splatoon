package com.plokie.moving_blocks;

import com.mojang.math.Transformation;
import com.plokie.Splatoon;
import com.plokie.helpers.Affects;
import com.plokie.helpers.Effects;
import com.plokie.helpers.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Tuple;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MovingBlocksEntity {
    public static MovingBlocksEntity create(Level level, BlockPos corner0, BlockPos corner1, Vec3 pivotOffset, Vec3 spawnPos, boolean collision) {
        MovingBlocksEntity movingBlocksEntity = new MovingBlocksEntity();

        Display.BlockDisplay rootEntity = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
        if(rootEntity == null) return null;

        rootEntity.setPos(spawnPos);
        rootEntity.setUUID(UUID.randomUUID());
        rootEntity.addTag("MovingBlocksEntity");

        level.addFreshEntity(rootEntity);

        List<Tuple<UUID, Vec3>> displayEntities = new ArrayList<>();
        List<Tuple<UUID, Vec3>> collisionVehicles = new ArrayList<>();

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

                    displayEntities.add(new Tuple<>(displayEntity.getUUID(), new Vec3(relativePosition)));

                    //
                    boolean canMakeCollision = true;
                    if(blockState.is(BlockTags.TRAPDOORS)) canMakeCollision = false;

                    if(collision && canMakeCollision)
                    {
                        Display.BlockDisplay collisionVehicle = EntityType.BLOCK_DISPLAY.create(level, EntitySpawnReason.COMMAND);
                        Shulker collisionBox = EntityType.SHULKER.create(level, EntitySpawnReason.COMMAND);
                        if(collisionBox == null) {
                            Splatoon.LOGGER.warn("Failed to create shulker collision box");
                            continue;
                        }
                        if(collisionVehicle == null) {
                            Splatoon.LOGGER.warn("Failed to create collision vehicle");
                            continue;
                        }

                        collisionBox.setNoAi(true);
                        collisionBox.setPersistenceRequired();
                        collisionBox.setInvulnerable(true);

                        Vec3 collisionPos = new Vec3(relativePosition).add(0.5, 0, 0.5);

                        Vec3 pos = Helpers.localToWorld(spawnPos, 0.0f, 0.0f, collisionPos);
                        Splatoon.LOGGER.info("Spawned shulker collision box at {}", Helpers.toBlockPos(pos));

                        collisionVehicle.setPos(pos);
                        collisionBox.setPos(pos);

                        level.addFreshEntity(collisionVehicle);
                        level.addFreshEntity(collisionBox);

                        Effects.givePotionEffect(collisionBox, MobEffects.RESISTANCE, 9999, 200, true);
                        Effects.givePotionEffect(collisionBox, MobEffects.INVISIBILITY, 9999, 200, true);
                        Effects.givePotionEffect(collisionBox, MobEffects.REGENERATION, 9999, 200, true);
                        Effects.givePotionEffect(collisionBox, MobEffects.HEALTH_BOOST, 9999, 200, true);


                        //Affects.setAttributeModifier(collisionBox, "scale", "scale", -0.1, AttributeModifier.Operation.ADD_VALUE);

                        collisionBox.startRiding(collisionVehicle, true);

                        collisionVehicles.add(new Tuple<>(collisionVehicle.getUUID(), collisionPos));

                    }

                }
            }
        }


        movingBlocksEntity.rootEntity = rootEntity.getUUID();
        movingBlocksEntity.displayEntities = displayEntities;
        movingBlocksEntity.collisionVehicles = collisionVehicles;


        return movingBlocksEntity;
    }

    public void tick()
    {
        if(getRootEntity() == null) return;

        for(var vehiclePassenger : collisionVehicles) {
            Entity vehicle = Splatoon.SERVER.overworld().getEntity(vehiclePassenger.getA());
            if(vehicle == null) continue;

            Vec3 relativePos = vehiclePassenger.getB();
            Vec3 pos = Helpers.localToWorld(getRootEntity(), relativePos);
            vehicle.setPos(pos);
        }
    }

    public void discard()
    {
        Display.BlockDisplay rootEntity = getRootEntity();
        if(rootEntity == null) return;

        killPassengersRecur(rootEntity);
        rootEntity.discard();

        for(var vehiclePassenger : collisionVehicles) {
            Entity entity1 = Splatoon.SERVER.overworld().getEntity(vehiclePassenger.getA());

            if(entity1 != null) {
                killPassengersRecur(entity1);
                entity1.discard();
            }
        }
    }

    void killPassengersRecur(Entity entity) {
        for(Entity passenger : entity.getPassengers())
        {
            killPassengersRecur(passenger);
            passenger.discard();
        }
    }

    public void updateRotation()
    {
        Display.BlockDisplay rootEntity = getRootEntity();
        if(rootEntity == null) return;

        for(Tuple<UUID, Vec3> displayEntity : displayEntities) {
            Entity childEntity = Splatoon.SERVER.overworld().getEntity(displayEntity.getA());
            if(childEntity == null) continue;
            if(!(childEntity instanceof Display.BlockDisplay child)) continue;

            child.setXRot(rootEntity.getXRot());
            child.setYRot(rootEntity.getYRot());
        }
    }

    public List<Tuple<Display.BlockDisplay, Vec3>> getDisplayEntities() {
        List<Tuple<Display.BlockDisplay, Vec3>> ret = new ArrayList<>();
        for(Tuple<UUID, Vec3> displayEntity : displayEntities)
        {
            Entity childEntity = Splatoon.SERVER.overworld().getEntity(displayEntity.getA());
            if(childEntity == null) {
                Splatoon.LOGGER.warn("Failed to get moving display entity child");
                continue;
            }
            if(!(childEntity instanceof Display.BlockDisplay child)) {
                Splatoon.LOGGER.warn("Child wasnt a block display?");
                continue;
            }
            ret.add(new Tuple<>(child, displayEntity.getB()));
        }

        return ret;
    }

    public Display.BlockDisplay getRootEntity()
    {
        Entity entity = Splatoon.SERVER.overworld().getEntity(rootEntity);
        if(entity == null) return null;
        if(entity instanceof Display.BlockDisplay blockDisplay) return blockDisplay;
        return null;
    }

    public UUID rootEntity;
    List<Tuple<UUID, Vec3>> displayEntities = new ArrayList<>();
    List<Tuple<UUID, Vec3>> collisionVehicles = new ArrayList<>();
}
