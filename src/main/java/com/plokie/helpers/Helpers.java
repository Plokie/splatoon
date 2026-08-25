package com.plokie.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Helpers {
    public static BlockPos toBlockPos(Vec3 vec3) {
        return new BlockPos(
                (int)Math.floor(vec3.x()),
                (int)Math.floor(vec3.y()),
                (int)Math.floor(vec3.z())
        );
    }

    public static BlockPos toBlockPos(Vector3f vec3f)
    {
        return new BlockPos(
                (int)Math.floor(vec3f.x),
                (int)Math.floor(vec3f.y),
                (int)Math.floor(vec3f.z)
        );
    }

    public static BlockPos toBlockPos(Vector3d vec3d)
    {
        return new BlockPos(
                (int)Math.floor(vec3d.x),
                (int)Math.floor(vec3d.y),
                (int)Math.floor(vec3d.z)
        );
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(Level level, Class<T> ofClass, Vec3 position, float radius) {
        return getEntitiesInRadius(level, ofClass, position.toVector3f(), radius);
    }

    public static <T extends Entity> List<T> getEntitiesInRadius(Level level, Class<T> ofClass, Vector3f position, float radius) {
        List<T> ret = new ArrayList<>();

        AABB aabb = new AABB(toBlockPos(position)).inflate(radius * 1.2);

        for(T entity : level.getEntitiesOfClass(ofClass, aabb))
        {
            if(entity.distanceToSqr(new Vec3(position.x, position.y, position.z)) < radius*radius)
            {
                ret.add(entity);
            }
        }

        return  ret;
    }
}
