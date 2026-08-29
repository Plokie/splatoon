package com.plokie.management.maps;

import com.plokie.management.gamemodes.Gamemodes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public class GamemodeMap {
    public Map<Integer, Vec3> teamSpawns = new HashMap<>();

    public Vec3 introStartPosition;
    public Vec2 introStartRotation;

    public Vec3 resultsPosition;
    public Vec2 resultsRotation;

    public Vec3 mapCorner;
    public Vec3 mapSize;

    public Vec3 spectatorZone;

    public GamemodeMaps toEnum()
    {
        return GamemodeMaps.valueOf(getClass().getSimpleName());
    }
}
