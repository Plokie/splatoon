package com.plokie.management.maps;

import com.plokie.management.gamemodes.Gamemodes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GamemodeMap {
    public Map<Integer, Vec3> teamSpawns = new HashMap<>();

    public List<BlockPos> podiums = new ArrayList<>();
    public Vec3 podiumViewerPosition;
    public Vec2 podiumViewerRotation;

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

    public Block groundBlock = Blocks.CYAN_TERRACOTTA;
    public Block wallBlock = Blocks.PALE_MOSS_BLOCK;
}
