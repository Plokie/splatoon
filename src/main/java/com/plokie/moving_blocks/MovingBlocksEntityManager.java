package com.plokie.moving_blocks;

import com.mojang.math.Transformation;
import com.plokie.helpers.CommandBuilder;
import com.plokie.helpers.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MovingBlocksEntityManager {
    public MovingBlocksEntityManager()
    {
        CommandBuilder
                .command("movingblocksentity")
                .subcommand("create")
                    .argumentBlockPos("corner0")
                    .argumentBlockPos("corner1")
                    .argumentVec3("pivot_offset")
                .executes(ctx->{
            Level level = ctx.getStack().getSource().getLevel();
            BlockPos corner0 = ctx.getArgumentBlockPos("corner0");
            BlockPos corner1 = ctx.getArgumentBlockPos("corner1");
            Vec3 pivotOffset = ctx.getArgumentVec3("pivot_offset");
            Vec3 spawnPos = ctx.getStack().getSource().getPosition();

            MovingBlocksEntity movingBlocksEntity = MovingBlocksEntity.create(level, corner0, corner1, pivotOffset, spawnPos);
            if(movingBlocksEntity==null) {
                return "! Something went wrong, check console for more info";
            }

            return "Created moving blocks entity";

        }).register();

        CommandBuilder
                .command("movingblocksentity")
                .subcommand("destroy_nearest")
                .executes(ctx->{
                    Level level = ctx.getStack().getSource().getLevel();
                    Vec3 pos = ctx.getStack().getSource().getPosition();

                    boolean didDestroy = false;
                    for (Display.BlockDisplay blockDisplay : level.getEntitiesOfClass(Display.BlockDisplay.class, new AABB(Helpers.toBlockPos(pos)).inflate(10.0))) {
                        if(blockDisplay.getTags().contains("MovingBlocksEntity") || blockDisplay.getTags().contains("MovingBlocksEntity_Child"))
                        {
                            blockDisplay.discard();
                            didDestroy = true;
                        }
                    }

                    if(didDestroy) {
                        return "Destroyed nearest moving blocks entity";
                    }
                    else {
                        return "! No moving blocks entity nearby to destroy";
                    }

                }).register();
    }
}
