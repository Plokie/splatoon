package com.plokie.management.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Cyberscape extends GamemodeMap {
    public Cyberscape()
    {
        teamSpawns.put(0, new Vec3(4124, 121, 1374));
        teamSpawns.put(1, new Vec3(4002, 121, 1311));

        //this.introStartPosition = new Vec3(4015.663, 124.11913, 1347.858);
        //this.introStartRotation = new Vec2(-114.0f, 1.8f);

        this.introStartPosition = new Vec3(4042.273, 127.5, 1316.5);
        this.introStartRotation = new Vec2(-39.8f, 0.0f);

        this.resultsPosition = new Vec3(4078.511, 137.13, 1362.965);
        this.resultsRotation = new Vec2(147.0f, 33.5f);

        this.mapCorner = new Vec3(3970, 100, 1305);
        this.mapSize = new Vec3(173, 79, 80);

        this.spectatorZone = new Vec3(4063, 145, 1342);

        this.getGroundBlock = y->Blocks.WHITE_CONCRETE_POWDER;
        this.getWallBlock = y->Blocks.WHITE_WOOL;

        for(int i=0;i<5;i++) {
            this.podiums.add(new BlockPos(4093, 114, 1359 - (i*4)));
        }
        this.podiumViewerPosition = new Vec3(4084.5, 118.0, 1351.5);
        this.podiumViewerRotation = new Vec2(-90.0f, 19.0f);
    }
}
