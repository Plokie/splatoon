package com.plokie.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;

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
}
