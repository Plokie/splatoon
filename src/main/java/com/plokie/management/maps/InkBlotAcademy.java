package com.plokie.management.maps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class InkBlotAcademy extends GamemodeMap {
    public InkBlotAcademy()
    {
        teamSpawns.put(0, new Vec3(5756, 107, 1343));
        teamSpawns.put(1, new Vec3(5680, 107, 1463));

        this.introStartPosition = new Vec3(5685.623, 114.74331, 1440.082);
        this.introStartRotation = new Vec2(-127.4f, 10.0f);

        this.resultsPosition = new Vec3(5732.795, 128.52464, 1435.929);
        this.resultsRotation = new Vec2(143.1f, 47.3f);

        this.mapCorner = new Vec3(5668, 77, 1355);
        this.mapSize = new Vec3(95, 54, 114);

        this.spectatorZone = new Vec3(5718.5, 127, 1414.5);

        this.getGroundBlock = y-> Blocks.CYAN_TERRACOTTA;
        this.getWallBlock = y->Blocks.PALE_MOSS_BLOCK;

        for(int i=0; i<5; i++) {
            this.podiums.add(new BlockPos(5691+ (i*4), 96, 1403));
        }

        this.podiumViewerPosition = new Vec3(5698.5, 100.0, 1394.5);
        this.podiumViewerRotation = new Vec2(0.0f, 21.3f);
    }
}
